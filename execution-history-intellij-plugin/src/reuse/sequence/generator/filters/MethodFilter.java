package reuse.sequence.generator.filters;

import com.intellij.psi.PsiMethod;

public interface MethodFilter {
    boolean allow(PsiMethod psiMethod);
}
