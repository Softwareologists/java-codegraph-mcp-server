package tech.softwareologists.core;

import org.junit.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import tech.softwareologists.core.db.EmbeddedNeo4j;
import tech.softwareologists.core.db.NodeLabel;
import tech.softwareologists.core.db.EdgeType;

import java.util.List;

public class QueryServiceImplTest {
    @Test
    public void findCallers_singleDependency_returnsCaller() {
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            try (Session session = driver.session()) {
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'dep.A'})");
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'dep.B'})");
                session.run("MATCH (b:" + NodeLabel.CLASS + " {name:'dep.B'}), (a:" + NodeLabel.CLASS + " {name:'dep.A'}) CREATE (b)-[:DEPENDS_ON]->(a)");
            }

            QueryService service = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> callers = service.findCallers("dep.A", null, null, null);
            if (callers.getItems().size() != 1 || !callers.getItems().get(0).equals("dep.B")) {
                throw new AssertionError("Unexpected callers: " + callers.getItems());
            }
        }
    }

    @Test
    public void findImplementations_interfaceWithTwoClasses_returnsBoth() throws Exception {
        java.nio.file.Path src = java.nio.file.Files.createTempDirectory("implsrc");
        java.nio.file.Path pkg = src.resolve("impl");
        java.nio.file.Files.createDirectories(pkg);
        java.nio.file.Path iface = pkg.resolve("MyIntf.java");
        java.nio.file.Files.write(iface, "package impl; public interface MyIntf {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Path c1 = pkg.resolve("ImplA.java");
        java.nio.file.Files.write(c1, "package impl; public class ImplA implements MyIntf {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Path c2 = pkg.resolve("ImplB.java");
        java.nio.file.Files.write(c2, "package impl; public class ImplB implements MyIntf {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java compiler not available");
        int res = compiler.run(null, null, null, iface.toString(), c1.toString(), c2.toString());
        if (res != 0) throw new IllegalStateException("Compilation failed");

        java.io.File jar = java.io.File.createTempFile("impl", ".jar");
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(new java.io.FileOutputStream(jar))) {
            for (String n : new String[]{"impl/MyIntf.class", "impl/ImplA.class", "impl/ImplB.class"}) {
                jos.putNextEntry(new java.util.jar.JarEntry(n));
                java.nio.file.Files.copy(pkg.resolve(n.substring(n.lastIndexOf('/') + 1)), jos);
                jos.closeEntry();
            }
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            JarImporter.importJar(jar, driver);

            QueryService service = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> impls = service.findImplementations("impl.MyIntf", null, null, null);
            java.util.Set<String> expected = new java.util.HashSet<>();
            expected.add("impl.ImplA");
            expected.add("impl.ImplB");
            if (!impls.getItems().containsAll(expected) || impls.getItems().size() != 2) {
                throw new AssertionError("Unexpected implementations: " + impls.getItems());
            }
        }
    }

    @Test
    public void findSubclasses_hierarchy_respectsDepth() throws Exception {
        java.nio.file.Path src = java.nio.file.Files.createTempDirectory("subsrc");
        java.nio.file.Path pkg = src.resolve("hier");
        java.nio.file.Files.createDirectories(pkg);
        java.nio.file.Path base = pkg.resolve("Base.java");
        java.nio.file.Files.write(base, "package hier; public class Base {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Path mid = pkg.resolve("Mid.java");
        java.nio.file.Files.write(mid, "package hier; public class Mid extends Base {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Path leaf = pkg.resolve("Leaf.java");
        java.nio.file.Files.write(leaf, "package hier; public class Leaf extends Mid {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java compiler not available");
        int res = compiler.run(null, null, null, base.toString(), mid.toString(), leaf.toString());
        if (res != 0) throw new IllegalStateException("Compilation failed");

        java.io.File jar = java.io.File.createTempFile("subs", ".jar");
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(new java.io.FileOutputStream(jar))) {
            for (String n : new String[]{"hier/Base.class", "hier/Mid.class", "hier/Leaf.class"}) {
                jos.putNextEntry(new java.util.jar.JarEntry(n));
                java.nio.file.Files.copy(pkg.resolve(n.substring(n.lastIndexOf('/') + 1)), jos);
                jos.closeEntry();
            }
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            JarImporter.importJar(jar, driver);

            QueryService service = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> depth1 = service.findSubclasses("hier.Base", 1, null, null, null);
            if (depth1.getItems().size() != 1 || !depth1.getItems().get(0).equals("hier.Mid")) {
                throw new AssertionError("Unexpected depth1 result: " + depth1.getItems());
            }

            tech.softwareologists.core.QueryResult<String> depth2 = service.findSubclasses("hier.Base", 2, null, null, null);
            java.util.Set<String> expected = new java.util.HashSet<>();
            expected.add("hier.Mid");
            expected.add("hier.Leaf");
            if (!depth2.getItems().containsAll(expected) || depth2.getItems().size() != 2) {
                throw new AssertionError("Unexpected depth2 result: " + depth2.getItems());
            }
        }
    }

    @Test
    public void findDependencies_chain_respectsDepth() {
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            try (Session session = driver.session()) {
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'A'})");
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'B'})");
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'C'})");
                session.run("MATCH (a:" + NodeLabel.CLASS + " {name:'A'}), (b:" + NodeLabel.CLASS + " {name:'B'}) CREATE (a)-[:DEPENDS_ON]->(b)");
                session.run("MATCH (b:" + NodeLabel.CLASS + " {name:'B'}), (c:" + NodeLabel.CLASS + " {name:'C'}) CREATE (b)-[:DEPENDS_ON]->(c)");
            }

            QueryService service = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> depth1 = service.findDependencies("A", 1, null, null, null);
            if (depth1.getItems().size() != 1 || !depth1.getItems().get(0).equals("B")) {
                throw new AssertionError("Unexpected depth1 result: " + depth1.getItems());
            }

            tech.softwareologists.core.QueryResult<String> depth2 = service.findDependencies("A", 2, null, null, null);
            java.util.Set<String> expected = new java.util.HashSet<>();
            expected.add("B");
            expected.add("C");
            if (!depth2.getItems().containsAll(expected) || depth2.getItems().size() != 2) {
                throw new AssertionError("Unexpected depth2 result: " + depth2.getItems());
            }
        }
    }

    @Test
    public void findPathBetweenClasses_chain_respectsMaxDepth() {
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            try (Session session = driver.session()) {
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'A'})");
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'B'})");
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'C'})");
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'D'})");
                session.run("MATCH (a:" + NodeLabel.CLASS + " {name:'A'}), (b:" + NodeLabel.CLASS + " {name:'B'}) CREATE (a)-[:DEPENDS_ON]->(b)");
                session.run("MATCH (b:" + NodeLabel.CLASS + " {name:'B'}), (c:" + NodeLabel.CLASS + " {name:'C'}) CREATE (b)-[:DEPENDS_ON]->(c)");
                session.run("MATCH (c:" + NodeLabel.CLASS + " {name:'C'}), (d:" + NodeLabel.CLASS + " {name:'D'}) CREATE (c)-[:DEPENDS_ON]->(d)");
            }

            QueryService service = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> within = service.findPathBetweenClasses("A", "D", 3);
            java.util.List<String> expected = java.util.Arrays.asList("A", "B", "C", "D");
            if (!within.getItems().equals(expected)) {
                throw new AssertionError("Unexpected path: " + within.getItems());
            }

            tech.softwareologists.core.QueryResult<String> tooShort = service.findPathBetweenClasses("A", "D", 2);
            if (!tooShort.getItems().isEmpty()) {
                throw new AssertionError("Path should be empty when exceeding depth: " + tooShort.getItems());
            }
        }
    }

    @Test
    public void findMethodsCallingMethod_singleCaller_returnsSignature() {
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            try (Session session = driver.session()) {
                session.run("CREATE (:" + NodeLabel.METHOD + " {class:'A', signature:'a()V'})");
                session.run("CREATE (:" + NodeLabel.METHOD + " {class:'B', signature:'b()V'})");
                session.run("MATCH (c:" + NodeLabel.METHOD + " {class:'B', signature:'b()V'}), (t:" + NodeLabel.METHOD + " {class:'A', signature:'a()V'}) CREATE (c)-[:CALLS]->(t)");
            }

            QueryService service = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> callers = service.findMethodsCallingMethod("A", "a()V", null, null, null);
            if (callers.getItems().size() != 1 || !callers.getItems().get(0).equals("b()V")) {
                throw new AssertionError("Unexpected callers: " + callers.getItems());
            }
        }
    }

    @Test
    public void findMethodsCallingMethod_multipleCallers_respectsLimit() {
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            try (Session session = driver.session()) {
                session.run("CREATE (:" + NodeLabel.METHOD + " {class:'A', signature:'a()V'})");
                session.run("CREATE (:" + NodeLabel.METHOD + " {class:'B', signature:'b()V'})");
                session.run("CREATE (:" + NodeLabel.METHOD + " {class:'C', signature:'c()V'})");
                session.run("MATCH (b:" + NodeLabel.METHOD + " {class:'B', signature:'b()V'}), (t:" + NodeLabel.METHOD + " {class:'A', signature:'a()V'}) CREATE (b)-[:CALLS]->(t)");
                session.run("MATCH (c:" + NodeLabel.METHOD + " {class:'C', signature:'c()V'}), (t:" + NodeLabel.METHOD + " {class:'A', signature:'a()V'}) CREATE (c)-[:CALLS]->(t)");
            }

            QueryService service = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> limit1 = service.findMethodsCallingMethod("A", "a()V", 1, null, null);
            if (limit1.getItems().size() != 1) {
                throw new AssertionError("Unexpected limit1 result: " + limit1.getItems());
            }
            tech.softwareologists.core.QueryResult<String> all = service.findMethodsCallingMethod("A", "a()V", null, null, null);
            java.util.Set<String> expected = new java.util.HashSet<>();
            expected.add("b()V");
            expected.add("c()V");
            if (!all.getItems().containsAll(expected) || all.getItems().size() != 2) {
                throw new AssertionError("Unexpected all result: " + all.getItems());
            }
        }
    }

    @Test
    public void findMethodsCallingMethod_afterImport_returnsCallers() throws Exception {
        java.nio.file.Path src = java.nio.file.Files.createTempDirectory("callersrc");
        java.nio.file.Path pkg = src.resolve("invoke");
        java.nio.file.Files.createDirectories(pkg);
        java.nio.file.Path callee = pkg.resolve("Callee.java");
        java.nio.file.Files.write(callee, "package invoke; public class Callee { public void methodB() {} }".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Path callerA = pkg.resolve("Caller.java");
        java.nio.file.Files.write(callerA, "package invoke; public class Caller { public void methodA() { new Callee().methodB(); } }".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Path callerB = pkg.resolve("Caller2.java");
        java.nio.file.Files.write(callerB, "package invoke; public class Caller2 { public void methodC() { new Callee().methodB(); } }".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java compiler not available");
        int res = compiler.run(null, null, null, callee.toString(), callerA.toString(), callerB.toString());
        if (res != 0) throw new IllegalStateException("Compilation failed");

        java.io.File jar = java.io.File.createTempFile("invoke", ".jar");
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(new java.io.FileOutputStream(jar))) {
            for (String n : new String[]{"invoke/Callee.class", "invoke/Caller.class", "invoke/Caller2.class"}) {
                jos.putNextEntry(new java.util.jar.JarEntry(n));
                java.nio.file.Files.copy(pkg.resolve(n.substring(n.lastIndexOf('/') + 1)), jos);
                jos.closeEntry();
            }
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            JarImporter.importJar(jar, driver);

            QueryService service = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> all = service.findMethodsCallingMethod("invoke.Callee", "methodB()V", null, null, null);
            java.util.Set<String> expected = new java.util.HashSet<>();
            expected.add("methodA()V");
            expected.add("methodC()V");
            if (!all.getItems().containsAll(expected) || all.getItems().size() != 2) {
                throw new AssertionError("Unexpected all result: " + all.getItems());
            }

            tech.softwareologists.core.QueryResult<String> limit1 = service.findMethodsCallingMethod("invoke.Callee", "methodB()V", 1, null, null);
            if (limit1.getItems().size() != 1 || !expected.contains(limit1.getItems().get(0))) {
                throw new AssertionError("Unexpected limit result: " + limit1.getItems());
            }
        }
    }

    @Test
    public void annotationQueries_afterImport_returnAnnotatedElements() throws Exception {
        java.nio.file.Path src = java.nio.file.Files.createTempDirectory("annsrc");
        java.nio.file.Path pkg = src.resolve("ann");
        java.nio.file.Files.createDirectories(pkg);
        java.nio.file.Path anno = pkg.resolve("MyAnno.java");
        java.nio.file.Files.write(anno,
                "package ann; public @interface MyAnno {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Path cls = pkg.resolve("AnnoClass.java");
        java.nio.file.Files.write(cls,
                "package ann; @MyAnno public class AnnoClass {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Path meth = pkg.resolve("MethodClass.java");
        java.nio.file.Files.write(meth,
                "package ann; public class MethodClass { @MyAnno public void foo(){} }".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java compiler not available");
        int res = compiler.run(null, null, null, anno.toString(), cls.toString(), meth.toString());
        if (res != 0) throw new IllegalStateException("Compilation failed");

        java.io.File jar = java.io.File.createTempFile("ann", ".jar");
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(new java.io.FileOutputStream(jar))) {
            for (String n : new String[]{"ann/MyAnno.class", "ann/AnnoClass.class", "ann/MethodClass.class"}) {
                jos.putNextEntry(new java.util.jar.JarEntry(n));
                java.nio.file.Files.copy(pkg.resolve(n.substring(n.lastIndexOf('/') + 1)), jos);
                jos.closeEntry();
            }
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            JarImporter.importJar(jar, driver);

            QueryService service = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> classes = service.findBeansWithAnnotation("ann.MyAnno", null, null, null);
            if (classes.getItems().size() != 1 || !classes.getItems().get(0).equals("ann.AnnoClass")) {
                throw new AssertionError("Unexpected classes: " + classes.getItems());
            }

            tech.softwareologists.core.QueryResult<String> methods = service.searchByAnnotation("ann.MyAnno", "method", null, null, null);
            if (methods.getItems().size() != 1 || !methods.getItems().get(0).equals("foo()V")) {
                throw new AssertionError("Unexpected methods: " + methods.getItems());
            }
        }
    }

    @Test
    public void findHttpEndpoints_filtersByPathAndMethod() throws Exception {
        java.nio.file.Path src = java.nio.file.Files.createTempDirectory("httpsrc");
        java.nio.file.Path pkg = src.resolve("web");
        java.nio.file.Files.createDirectories(pkg);

        java.nio.file.Path getAnno = pkg.resolveSibling("org/springframework/web/bind/annotation/GetMapping.java");
        java.nio.file.Files.createDirectories(getAnno.getParent());
        java.nio.file.Files.write(getAnno,
                ("package org.springframework.web.bind.annotation;" +
                        "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)" +
                        "@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)" +
                        "public @interface GetMapping { String value(); }").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.nio.file.Path postAnno = pkg.resolveSibling("org/springframework/web/bind/annotation/PostMapping.java");
        java.nio.file.Files.write(postAnno,
                ("package org.springframework.web.bind.annotation;" +
                        "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)" +
                        "@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)" +
                        "public @interface PostMapping { String value(); }").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.nio.file.Path ctrl = pkg.resolve("MyController.java");
        java.nio.file.Files.write(ctrl,
                ("package web;" +
                        "import org.springframework.web.bind.annotation.GetMapping;" +
                        "import org.springframework.web.bind.annotation.PostMapping;" +
                        "public class MyController {" +
                        "  @GetMapping(\"/foo\") public void foo() {}" +
                        "  @PostMapping(\"/bar\") public void bar() {}" +
                        "}").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java compiler not available");
        int res = compiler.run(null, null, null,
                getAnno.toString(), postAnno.toString(), ctrl.toString());
        if (res != 0) throw new IllegalStateException("Compilation failed");

        java.io.File jar = java.io.File.createTempFile("http", ".jar");
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(new java.io.FileOutputStream(jar))) {
            for (String n : new String[]{
                    "org/springframework/web/bind/annotation/GetMapping.class",
                    "org/springframework/web/bind/annotation/PostMapping.class",
                    "web/MyController.class"}) {
                jos.putNextEntry(new java.util.jar.JarEntry(n));
                java.nio.file.Path p = n.endsWith("GetMapping.class") ? getAnno.getParent().resolve("GetMapping.class") :
                        n.endsWith("PostMapping.class") ? postAnno.getParent().resolve("PostMapping.class") : pkg.resolve("MyController.class");
                java.nio.file.Files.copy(p, jos);
                jos.closeEntry();
            }
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            Driver driver = db.getDriver();
            JarImporter.importJar(jar, driver);

            QueryService service = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> get = service.findHttpEndpoints("/foo", "GET", null, null, null);
            if (get.getItems().size() != 1 || !get.getItems().get(0).contains("foo()V")) {
                throw new AssertionError("Unexpected GET result: " + get.getItems());
            }

            tech.softwareologists.core.QueryResult<String> post = service.findHttpEndpoints("/bar", "POST", null, null, null);
            if (post.getItems().size() != 1 || !post.getItems().get(0).contains("bar()V")) {
                throw new AssertionError("Unexpected POST result: " + post.getItems());
            }
        }
    }

    @Test
    public void findControllersUsingService_constructorInjection_returnsController() throws Exception {
        java.nio.file.Path src = java.nio.file.Files.createTempDirectory("ctrlsrc");
        java.nio.file.Path pkg = src.resolve("inj");
        java.nio.file.Files.createDirectories(pkg);

        java.nio.file.Path ctrlAnno = pkg.resolveSibling("org/springframework/stereotype/Controller.java");
        java.nio.file.Files.createDirectories(ctrlAnno.getParent());
        java.nio.file.Files.write(ctrlAnno,
                ("package org.springframework.stereotype;" +
                        "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)" +
                        "@java.lang.annotation.Target(java.lang.annotation.ElementType.TYPE)" +
                        "public @interface Controller {}").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.nio.file.Path service = pkg.resolve("MyService.java");
        java.nio.file.Files.write(service, "package inj; public class MyService {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.nio.file.Path controller = pkg.resolve("MyController.java");
        java.nio.file.Files.write(controller,
                ("package inj;" +
                        "import org.springframework.stereotype.Controller;" +
                        "@Controller public class MyController {" +
                        "  private final MyService svc;" +
                        "  public MyController(MyService svc) { this.svc = svc; }" +
                        "}").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java compiler not available");
        int res = compiler.run(null, null, null, ctrlAnno.toString(), service.toString(), controller.toString());
        if (res != 0) throw new IllegalStateException("Compilation failed");

        java.io.File jar = java.io.File.createTempFile("ctrl", ".jar");
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(new java.io.FileOutputStream(jar))) {
            for (String n : new String[]{
                    "org/springframework/stereotype/Controller.class",
                    "inj/MyService.class",
                    "inj/MyController.class"}) {
                jos.putNextEntry(new java.util.jar.JarEntry(n));
                java.nio.file.Path p = n.contains("Controller.class") && !n.startsWith("inj/")
                        ? ctrlAnno.getParent().resolve("Controller.class")
                        : pkg.resolve(n.substring(n.lastIndexOf('/') + 1));
                java.nio.file.Files.copy(p, jos);
                jos.closeEntry();
            }
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            org.neo4j.driver.Driver driver = db.getDriver();
            JarImporter.importJar(jar, driver);

            QueryService serviceApi = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> ctrls = serviceApi.findControllersUsingService("inj.MyService", null, null, null);
            if (ctrls.getItems().size() != 1 || !ctrls.getItems().get(0).equals("inj.MyController")) {
                throw new AssertionError("Unexpected controllers: " + ctrls.getItems());
            }
        }
    }

    @Test
    public void eventListener_importAndQuery_returnsMethod() throws Exception {
        java.nio.file.Path src = java.nio.file.Files.createTempDirectory("evsrc");
        java.nio.file.Path pkg = src.resolve("evt");
        java.nio.file.Files.createDirectories(pkg);

        java.nio.file.Path listenerAnno = pkg.resolveSibling("org/springframework/context/event/EventListener.java");
        java.nio.file.Files.createDirectories(listenerAnno.getParent());
        java.nio.file.Files.write(listenerAnno,
                ("package org.springframework.context.event;" +
                        "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)" +
                        "@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)" +
                        "public @interface EventListener { Class<?>[] classes() default {}; }").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.nio.file.Path event = pkg.resolve("MyEvent.java");
        java.nio.file.Files.write(event, "package evt; public class MyEvent {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.nio.file.Path listener = pkg.resolve("MyListener.java");
        java.nio.file.Files.write(listener,
                ("package evt;" +
                        "import org.springframework.context.event.EventListener;" +
                        "public class MyListener {" +
                        "  @EventListener(classes=MyEvent.class) public void handle(evt.MyEvent e) {}" +
                        "}").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java compiler not available");
        int res = compiler.run(null, null, null, listenerAnno.toString(), event.toString(), listener.toString());
        if (res != 0) throw new IllegalStateException("Compilation failed");

        java.io.File jar = java.io.File.createTempFile("evt", ".jar");
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(new java.io.FileOutputStream(jar))) {
            for (String n : new String[]{"org/springframework/context/event/EventListener.class", "evt/MyEvent.class", "evt/MyListener.class"}) {
                jos.putNextEntry(new java.util.jar.JarEntry(n));
                java.nio.file.Path p = n.contains("EventListener.class") ? listenerAnno.getParent().resolve("EventListener.class") : pkg.resolve(n.substring(n.lastIndexOf('/') + 1));
                java.nio.file.Files.copy(p, jos);
                jos.closeEntry();
            }
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            org.neo4j.driver.Driver driver = db.getDriver();
            JarImporter.importJar(jar, driver);

            QueryService svc = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> listeners = svc.findEventListeners("evt.MyEvent", null, null, null);
            if (listeners.getItems().size() != 1 || !listeners.getItems().get(0).contains("handle")) {
                throw new AssertionError("Unexpected listeners: " + listeners.getItems());
            }
        }
    }

    @Test
    public void scheduledTask_importAndQuery_returnsMethod() throws Exception {
        java.nio.file.Path src = java.nio.file.Files.createTempDirectory("schedsrc");
        java.nio.file.Path pkg = src.resolve("sch");
        java.nio.file.Files.createDirectories(pkg);

        java.nio.file.Path schedAnno = pkg.resolveSibling("org/springframework/scheduling/annotation/Scheduled.java");
        java.nio.file.Files.createDirectories(schedAnno.getParent());
        java.nio.file.Files.write(schedAnno,
                ("package org.springframework.scheduling.annotation;" +
                        "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)" +
                        "@java.lang.annotation.Target(java.lang.annotation.ElementType.METHOD)" +
                        "public @interface Scheduled { String cron(); }").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.nio.file.Path task = pkg.resolve("Task.java");
        java.nio.file.Files.write(task,
                ("package sch;" +
                        "import org.springframework.scheduling.annotation.Scheduled;" +
                        "public class Task {" +
                        "  @Scheduled(cron=\"0 0 * * * *\") public void run() {}" +
                        "}").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java compiler not available");
        int res2 = compiler.run(null, null, null, schedAnno.toString(), task.toString());
        if (res2 != 0) throw new IllegalStateException("Compilation failed");

        java.io.File jar = java.io.File.createTempFile("sch", ".jar");
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(new java.io.FileOutputStream(jar))) {
            for (String n : new String[]{"org/springframework/scheduling/annotation/Scheduled.class", "sch/Task.class"}) {
                jos.putNextEntry(new java.util.jar.JarEntry(n));
                java.nio.file.Path p = n.contains("Scheduled.class") ? schedAnno.getParent().resolve("Scheduled.class") : pkg.resolve("Task.class");
                java.nio.file.Files.copy(p, jos);
                jos.closeEntry();
            }
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            org.neo4j.driver.Driver driver = db.getDriver();
            JarImporter.importJar(jar, driver);

            QueryService svc = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> tasks = svc.findScheduledTasks(null, null, null);
            if (tasks.getItems().size() != 1 || !tasks.getItems().get(0).contains("run()V|0 0 * * * *")) {
                throw new AssertionError("Unexpected tasks: " + tasks.getItems());
            }
        }
    }

    @Test
    public void configPropertyUsage_importAndQuery_returnsLocations() throws Exception {
        java.nio.file.Path src = java.nio.file.Files.createTempDirectory("cfgsrc");
        java.nio.file.Path pkg = src.resolve("cfg");
        java.nio.file.Files.createDirectories(pkg);

        java.nio.file.Path valueAnno = pkg.resolveSibling("org/springframework/beans/factory/annotation/Value.java");
        java.nio.file.Files.createDirectories(valueAnno.getParent());
        java.nio.file.Files.write(valueAnno,
                ("package org.springframework.beans.factory.annotation;" +
                        "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)" +
                        "@java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD,java.lang.annotation.ElementType.PARAMETER})" +
                        "public @interface Value { String value(); }").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.nio.file.Path confAnno = pkg.resolveSibling("org/example/ConfigurationProperty.java");
        java.nio.file.Files.createDirectories(confAnno.getParent());
        java.nio.file.Files.write(confAnno,
                ("package org.example;" +
                        "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)" +
                        "@java.lang.annotation.Target({java.lang.annotation.ElementType.FIELD,java.lang.annotation.ElementType.PARAMETER})" +
                        "public @interface ConfigurationProperty { String value(); }").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        java.nio.file.Path cls = pkg.resolve("MyClass.java");
        java.nio.file.Files.write(cls,
                ("package cfg;" +
                        "import org.springframework.beans.factory.annotation.Value;" +
                        "import org.example.ConfigurationProperty;" +
                        "public class MyClass {" +
                        "  @Value(\"${app.url}\") String url;" +
                        "  public void set(@ConfigurationProperty(\"app.timeout\") int t) {}" +
                        "}").getBytes(java.nio.charset.StandardCharsets.UTF_8));

        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java compiler not available");
        int res = compiler.run(null, null, null, valueAnno.toString(), confAnno.toString(), cls.toString());
        if (res != 0) throw new IllegalStateException("Compilation failed");

        java.io.File jar = java.io.File.createTempFile("cfg", ".jar");
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(new java.io.FileOutputStream(jar))) {
            for (String n : new String[]{
                    "org/springframework/beans/factory/annotation/Value.class",
                    "org/example/ConfigurationProperty.class",
                    "cfg/MyClass.class"}) {
                jos.putNextEntry(new java.util.jar.JarEntry(n));
                java.nio.file.Path p = n.contains("Value.class") ? valueAnno.getParent().resolve("Value.class") :
                        n.contains("ConfigurationProperty.class") ? confAnno.getParent().resolve("ConfigurationProperty.class") :
                                pkg.resolve("MyClass.class");
                java.nio.file.Files.copy(p, jos);
                jos.closeEntry();
            }
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            org.neo4j.driver.Driver driver = db.getDriver();
            JarImporter.importJar(jar, driver);

            QueryService svc = new QueryServiceImpl(driver);
            tech.softwareologists.core.QueryResult<String> clsRes = svc.findConfigPropertyUsage("app.url", null, null, null);
            if (clsRes.getItems().size() != 1 || !clsRes.getItems().get(0).equals("cfg.MyClass")) {
                throw new AssertionError("Unexpected class usage: " + clsRes.getItems());
            }
            tech.softwareologists.core.QueryResult<String> methRes = svc.findConfigPropertyUsage("app.timeout", null, null, null);
            if (methRes.getItems().size() != 1 || !methRes.getItems().get(0).equals("cfg.MyClass|set(I)V")) {
                throw new AssertionError("Unexpected method usage: " + methRes.getItems());
            }
        }
    }

    @Test
    public void getPackageHierarchy_multiplePackages_returnsTree() throws Exception {
        java.nio.file.Path src = java.nio.file.Files.createTempDirectory("pkgsrc");
        java.nio.file.Path pkgA = src.resolve("p1/a");
        java.nio.file.Path pkgB = src.resolve("p1/b");
        java.nio.file.Path sub = src.resolve("p1/a/sub");
        java.nio.file.Files.createDirectories(sub);
        java.nio.file.Files.createDirectories(pkgB);

        java.nio.file.Path classA = pkgA.resolve("A.java");
        java.nio.file.Files.write(classA, "package p1.a; public class A {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Path classB = pkgB.resolve("B.java");
        java.nio.file.Files.write(classB, "package p1.b; public class B {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.nio.file.Path classC = sub.resolve("C.java");
        java.nio.file.Files.write(classC, "package p1.a.sub; public class C {}".getBytes(java.nio.charset.StandardCharsets.UTF_8));

        javax.tools.JavaCompiler compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java compiler not available");
        int res = compiler.run(null, null, null, classA.toString(), classB.toString(), classC.toString());
        if (res != 0) throw new IllegalStateException("Compilation failed");

        java.io.File jar = java.io.File.createTempFile("pkg", ".jar");
        try (java.util.jar.JarOutputStream jos = new java.util.jar.JarOutputStream(new java.io.FileOutputStream(jar))) {
            jos.putNextEntry(new java.util.jar.JarEntry("p1/a/A.class"));
            java.nio.file.Files.copy(pkgA.resolve("A.class"), jos);
            jos.closeEntry();
            jos.putNextEntry(new java.util.jar.JarEntry("p1/b/B.class"));
            java.nio.file.Files.copy(pkgB.resolve("B.class"), jos);
            jos.closeEntry();
            jos.putNextEntry(new java.util.jar.JarEntry("p1/a/sub/C.class"));
            java.nio.file.Files.copy(sub.resolve("C.class"), jos);
            jos.closeEntry();
        }

        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            org.neo4j.driver.Driver driver = db.getDriver();
            JarImporter.importJar(jar, driver);

            QueryService svc = new QueryServiceImpl(driver);
            String tree = svc.getPackageHierarchy("p1", 2);
            String expected = "{\"name\":\"p1\",\"packages\":[{\"name\":\"p1.a\",\"classes\":[\"p1.a.A\"],\"packages\":[{\"name\":\"p1.a.sub\",\"classes\":[\"p1.a.sub.C\"]}]},{\"name\":\"p1.b\",\"classes\":[\"p1.b.B\"]}]}";
            if (!tree.equals(expected)) {
                throw new AssertionError("Unexpected tree: " + tree);
            }
        }
    }

    @Test
    public void findCallers_paging_returnsOrderedSubsetAndTotal() {
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            org.neo4j.driver.Driver driver = db.getDriver();
            try (org.neo4j.driver.Session session = driver.session()) {
                session.run("CREATE (:" + NodeLabel.CLASS + " {name:'T'})");
                for (String n : new String[]{"A","B","C","D","E"}) {
                    session.run("CREATE (:" + NodeLabel.CLASS + " {name:$n})", java.util.Collections.singletonMap("n", n));
                    session.run("MATCH (c:" + NodeLabel.CLASS + " {name:$n}), (t:" + NodeLabel.CLASS + " {name:'T'}) CREATE (c)-[:" + EdgeType.DEPENDS_ON + "]->(t)", java.util.Collections.singletonMap("n", n));
                }
            }

            QueryService svc = new QueryServiceImpl(driver);
            QueryResult<String> res = svc.findCallers("T", null, 2, 2);
            java.util.List<String> expected = java.util.Arrays.asList("C","D");
            if (!res.getItems().equals(expected) || res.getTotal() != 5 || res.getPage() != 2 || res.getPageSize() != 2) {
                throw new AssertionError("Paging incorrect: " + res.getItems() + " total=" + res.getTotal());
            }
        }
    }

    @Test
    public void findBeansWithAnnotation_paging_ordersAndCounts() {
        try (EmbeddedNeo4j db = new EmbeddedNeo4j()) {
            org.neo4j.driver.Driver driver = db.getDriver();
            try (org.neo4j.driver.Session session = driver.session()) {
                for (String n : new String[]{"C1","C2","C3"}) {
                    java.util.Map<String,Object> m = new java.util.HashMap<>();
                    m.put("name", n);
                    m.put("annotations", java.util.List.of("Ann"));
                    session.run("CREATE (:" + NodeLabel.CLASS + " {name:$name, annotations:$annotations})", m);
                }
            }
            QueryService svc = new QueryServiceImpl(driver);
            QueryResult<String> res = svc.findBeansWithAnnotation("Ann", null, 2, 2);
            if (res.getTotal() != 3 || res.getPage() != 2 || res.getPageSize() != 2 || res.getItems().size() != 1 || !res.getItems().get(0).equals("C3")) {
                throw new AssertionError("Annotation paging incorrect: " + res.getItems());
            }
        }
    }
}
