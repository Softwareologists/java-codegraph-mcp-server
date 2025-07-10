package tech.softwareologists.core;

import org.junit.Test;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import tech.softwareologists.core.db.EmbeddedNeo4j;
import tech.softwareologists.core.db.NodeLabel;

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
            List<String> callers = service.findCallers("dep.A");
            if (callers.size() != 1 || !callers.get(0).equals("dep.B")) {
                throw new AssertionError("Unexpected callers: " + callers);
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
            java.util.List<String> impls = service.findImplementations("impl.MyIntf");
            java.util.Set<String> expected = new java.util.HashSet<>();
            expected.add("impl.ImplA");
            expected.add("impl.ImplB");
            if (!impls.containsAll(expected) || impls.size() != 2) {
                throw new AssertionError("Unexpected implementations: " + impls);
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
            java.util.List<String> depth1 = service.findSubclasses("hier.Base", 1);
            if (depth1.size() != 1 || !depth1.get(0).equals("hier.Mid")) {
                throw new AssertionError("Unexpected depth1 result: " + depth1);
            }

            java.util.List<String> depth2 = service.findSubclasses("hier.Base", 2);
            java.util.Set<String> expected = new java.util.HashSet<>();
            expected.add("hier.Mid");
            expected.add("hier.Leaf");
            if (!depth2.containsAll(expected) || depth2.size() != 2) {
                throw new AssertionError("Unexpected depth2 result: " + depth2);
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
            List<String> depth1 = service.findDependencies("A", 1);
            if (depth1.size() != 1 || !depth1.get(0).equals("B")) {
                throw new AssertionError("Unexpected depth1 result: " + depth1);
            }

            List<String> depth2 = service.findDependencies("A", 2);
            java.util.Set<String> expected = new java.util.HashSet<>();
            expected.add("B");
            expected.add("C");
            if (!depth2.containsAll(expected) || depth2.size() != 2) {
                throw new AssertionError("Unexpected depth2 result: " + depth2);
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
            List<String> callers = service.findMethodsCallingMethod("A", "a()V", null);
            if (callers.size() != 1 || !callers.get(0).equals("b()V")) {
                throw new AssertionError("Unexpected callers: " + callers);
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
            List<String> limit1 = service.findMethodsCallingMethod("A", "a()V", 1);
            if (limit1.size() != 1) {
                throw new AssertionError("Unexpected limit1 result: " + limit1);
            }
            List<String> all = service.findMethodsCallingMethod("A", "a()V", null);
            java.util.Set<String> expected = new java.util.HashSet<>();
            expected.add("b()V");
            expected.add("c()V");
            if (!all.containsAll(expected) || all.size() != 2) {
                throw new AssertionError("Unexpected all result: " + all);
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
            java.util.List<String> all = service.findMethodsCallingMethod("invoke.Callee", "methodB()V", null);
            java.util.Set<String> expected = new java.util.HashSet<>();
            expected.add("methodA()V");
            expected.add("methodC()V");
            if (!all.containsAll(expected) || all.size() != 2) {
                throw new AssertionError("Unexpected all result: " + all);
            }

            java.util.List<String> limit1 = service.findMethodsCallingMethod("invoke.Callee", "methodB()V", 1);
            if (limit1.size() != 1 || !expected.contains(limit1.get(0))) {
                throw new AssertionError("Unexpected limit result: " + limit1);
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
            java.util.List<String> classes = service.findBeansWithAnnotation("ann.MyAnno");
            if (classes.size() != 1 || !classes.get(0).equals("ann.AnnoClass")) {
                throw new AssertionError("Unexpected classes: " + classes);
            }

            java.util.List<String> methods = service.searchByAnnotation("ann.MyAnno", "method");
            if (methods.size() != 1 || !methods.get(0).equals("foo()V")) {
                throw new AssertionError("Unexpected methods: " + methods);
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
            java.util.List<String> get = service.findHttpEndpoints("/foo", "GET");
            if (get.size() != 1 || !get.get(0).contains("foo()V")) {
                throw new AssertionError("Unexpected GET result: " + get);
            }

            java.util.List<String> post = service.findHttpEndpoints("/bar", "POST");
            if (post.size() != 1 || !post.get(0).contains("bar()V")) {
                throw new AssertionError("Unexpected POST result: " + post);
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
            java.util.List<String> ctrls = serviceApi.findControllersUsingService("inj.MyService");
            if (ctrls.size() != 1 || !ctrls.get(0).equals("inj.MyController")) {
                throw new AssertionError("Unexpected controllers: " + ctrls);
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
            java.util.List<String> listeners = svc.findEventListeners("evt.MyEvent");
            if (listeners.size() != 1 || !listeners.get(0).contains("handle")) {
                throw new AssertionError("Unexpected listeners: " + listeners);
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
            java.util.List<String> tasks = svc.findScheduledTasks();
            if (tasks.size() != 1 || !tasks.get(0).contains("run()V|0 0 * * * *")) {
                throw new AssertionError("Unexpected tasks: " + tasks);
            }
        }
    }
}
