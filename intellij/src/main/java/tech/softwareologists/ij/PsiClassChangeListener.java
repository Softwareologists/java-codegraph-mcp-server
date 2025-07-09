package tech.softwareologists.ij;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

/**
 * Listens for PSI events related to Java classes and updates the
 * underlying graph using {@link PsiClassImportService}.
 */
public class PsiClassChangeListener extends PsiTreeChangeAdapter {
    private static final Logger LOG = Logger.getInstance(PsiClassChangeListener.class);

    private final Project project;
    private final PsiClassImportService importService;

    public PsiClassChangeListener(Project project, PsiClassImportService importService) {
        this.project = project;
        this.importService = importService;
    }

    @Override
    public void childAdded(@NotNull PsiTreeChangeEvent event) {
        handleEvent(event.getChild());
    }

    @Override
    public void childrenChanged(@NotNull PsiTreeChangeEvent event) {
        handleEvent(event.getParent());
    }

    private void handleEvent(PsiElement element) {
        PsiClass psiClass = PsiTreeUtil.getParentOfType(element, PsiClass.class, false);
        if (psiClass != null && psiClass.getQualifiedName() != null) {
            String qname = psiClass.getQualifiedName();
            LOG.info("Detected change in class " + qname);
            importService.importPsiClass(psiClass);
        }
    }
}
