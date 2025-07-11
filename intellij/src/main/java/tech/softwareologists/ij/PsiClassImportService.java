package tech.softwareologists.ij;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiJavaFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.openapi.vfs.VirtualFile;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import tech.softwareologists.core.db.NodeLabel;
import tech.softwareologists.core.db.EdgeType;

import java.util.Collection;

/**
 * Scans a project for {@link PsiClass} instances and persists them
 * to Neo4j via the core APIs.
 */
public class PsiClassImportService {
    private final Driver driver;
    private final java.util.Set<String> filters;

    public PsiClassImportService(Driver driver, String filterString) {
        this.driver = driver;
        this.filters = parseFilters(filterString);
    }

    private static java.util.Set<String> parseFilters(String filterString) {
        java.util.Set<String> set = new java.util.HashSet<>();
        if (filterString != null && !filterString.isBlank()) {
            for (String part : filterString.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    set.add(trimmed);
                }
            }
        }
        return set;
    }

    private boolean allowed(String qname) {
        if (filters.isEmpty()) return true;
        for (String f : filters) {
            if (qname.startsWith(f)) return true;
        }
        return false;
    }

    /**
     * Scan the given project and persist discovered classes.
     *
     * @param project project to scan
     * @return number of classes imported
     */
    public int importProjectClasses(Project project) {
        PsiManager psiManager = PsiManager.getInstance(project);
        Collection<VirtualFile> files = FilenameIndex.getAllFilesByExt(project, "java",
                GlobalSearchScope.projectScope(project));
        int count = 0;
        try (Session session = driver.session()) {
            for (VirtualFile vf : files) {
                PsiFile file = psiManager.findFile(vf);
                if (file instanceof PsiJavaFile) {
                    PsiClass[] classes = ((PsiJavaFile) file).getClasses();
                    for (PsiClass cls : classes) {
                        String qname = cls.getQualifiedName();
                        if (qname != null && allowed(qname)) {
                            session.run("MERGE (c:" + NodeLabel.CLASS + " {name:$name})",
                                    Values.parameters("name", qname));
                            count++;
                        }
                    }
                }
            }
        }
        return count;
    }

    /**
     * Import a single {@link PsiClass} into the graph.
     *
     * @param cls class to import
     */
    public void importPsiClass(PsiClass cls) {
        String qname = cls.getQualifiedName();
        if (qname == null || !allowed(qname)) {
            return;
        }
        try (Session session = driver.session()) {
            session.run("MERGE (c:" + NodeLabel.CLASS + " {name:$name})",
                    Values.parameters("name", qname));

            java.util.Set<String> seen = new java.util.HashSet<>();
            for (com.intellij.psi.PsiClassType t : cls.getExtendsListTypes()) {
                addEdge(qname, t, session, seen);
            }
            for (com.intellij.psi.PsiClassType t : cls.getImplementsListTypes()) {
                addEdge(qname, t, session, seen);
            }
        }
    }

    private void addEdge(String src, com.intellij.psi.PsiClassType type,
                          Session session, java.util.Set<String> seen) {
        com.intellij.psi.PsiClass resolved = type.resolve();
        String depName = resolved != null ? resolved.getQualifiedName() : type.getCanonicalText();
        if (depName == null || src.equals(depName) || !seen.add(depName)) {
            return;
        }
        session.run("MERGE (d:" + NodeLabel.CLASS + " {name:$dep})",
                Values.parameters("dep", depName));
        session.run("MATCH (s:" + NodeLabel.CLASS + " {name:$src}), (t:" + NodeLabel.CLASS +
                        " {name:$tgt}) MERGE (s)-[:" + EdgeType.DEPENDS_ON + "]->(t)",
                Values.parameters("src", src, "tgt", depName));
    }
}
