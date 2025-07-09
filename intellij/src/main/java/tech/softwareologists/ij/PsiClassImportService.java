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

import java.util.Collection;

/**
 * Scans a project for {@link PsiClass} instances and persists them
 * to Neo4j via the core APIs.
 */
public class PsiClassImportService {
    private final Driver driver;

    public PsiClassImportService(Driver driver) {
        this.driver = driver;
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
                        if (qname != null) {
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
}
