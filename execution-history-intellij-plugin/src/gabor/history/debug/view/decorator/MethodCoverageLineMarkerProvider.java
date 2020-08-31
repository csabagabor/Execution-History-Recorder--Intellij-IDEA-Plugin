
package gabor.history.debug.view.decorator;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProvider;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.markup.GutterIconRenderer.Alignment;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import gabor.history.action.CoverageContext;
import gabor.history.debug.DebugIcons;
import gabor.history.helper.LoggingHelper;
import gabor.history.helper.SequenceAdapter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reuse.sequence.generator.CallStack;
import reuse.sequence.generator.MethodDescription;

import java.util.*;

public class MethodCoverageLineMarkerProvider implements LineMarkerProvider, DumbAware {

    public static final int MAX_STACK_FRAME_SIZE_TO_SHOW = 5;

    @Override
    @Nullable
    public LineMarkerInfo<PsiElement> getLineMarkerInfo(@NotNull PsiElement element) {
        return null;
    }

    @Override
    public void collectSlowLineMarkers(@NotNull List<PsiElement> elements, @NotNull Collection<LineMarkerInfo> result) {
        try {
            if (!elements.isEmpty()) {
                Project project = elements.get(0).getProject();
                PsiFile containingFile = elements.get(0).getContainingFile();
                PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
                Document document = psiDocumentManager.getDocument(containingFile);

                CoverageContext context = CoverageContext.getContextByProject(project);

                if (context == null) {
                    return;
                }

                CallStack callStack = context.getCallStack();
                Map<String, HashMap<String, Set<Integer>>> callCache = context.getCallCache();

                if (callStack == null) {
                    return;
                }

                for (PsiElement element : elements) {
                    if (element instanceof PsiMethod) {
                        PsiMethod method = (PsiMethod) element;
                        PsiClass containingClass = method.getContainingClass();

                        if (containingClass != null) {
                            String qualifiedName = null;
                            PsiElement scope = containingClass.getScope();

                            //containingClass.getQualifiedName() is not null for inner classes
                            if (containingClass instanceof PsiAnonymousClass
                                    || scope instanceof PsiClass) {//PsiFile is returned for normal classes
                                //find outer class
                                PsiElement parent = method;
                                for (int i = 0; i < 20; i++) {//max 20 iterations
                                    parent = parent.getParent();
                                    if (parent == null) {
                                        break;
                                    }

                                    //most outer class which is not an inner or anonymous class
                                    if (parent instanceof PsiClass && ((PsiClass) parent).getQualifiedName() != null) {
                                        PsiElement parentScope = ((PsiClass) parent).getScope();

                                        if (containingClass instanceof PsiAnonymousClass || parentScope instanceof PsiFile) {//if scope was not PsiClass then just continue
                                            qualifiedName = ((PsiClass) parent).getQualifiedName() + "$";//$ to mark it as not simple class
                                        }
                                    }
                                }
                            } else {
                                qualifiedName = containingClass.getQualifiedName();
                            }

                            if (qualifiedName != null) {
                                if (callCache == null) {
                                    String finalQualifiedName = qualifiedName;
                                    result.add(new LineMarkerInfo<>(method, method.getTextRange(), DebugIcons.VARIABLES_TAB_ICON, (o) ->
                                            "Show All stack traces containing this method", (e, elt) -> navigationHandler(method, finalQualifiedName), Alignment.RIGHT));
                                } else if (callCache.get(qualifiedName) != null) {
                                    Set<Integer> lines = callCache.get(qualifiedName).get(method.getName());

                                    if (lines != null) {
                                        TextRange textRange = method.getTextRange();
                                        int startOffset = textRange.getStartOffset();
                                        int endOffset = textRange.getEndOffset();
                                        int lineNumberStart = document.getLineNumber(startOffset) + 1;
                                        int lineNumberEnd = document.getLineNumber(endOffset) + 1;

                                        if (lineNumberStart == -1 || lineNumberEnd == -1) {
                                            return;
                                        }

                                        boolean isMethodGoodOne = lines.stream().anyMatch(line -> line >= lineNumberStart && line <= lineNumberEnd);

                                        if (isMethodGoodOne) {
                                            String finalQualifiedName = qualifiedName;
                                            result.add(new LineMarkerInfo<>(method, method.getTextRange(), DebugIcons.VARIABLES_TAB_ICON, (o) ->
                                                    "Show All stack traces containing this method", (e, elt) -> navigationHandler(method, finalQualifiedName), Alignment.RIGHT));
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LoggingHelper.debug(e);
        }
    }

    private void navigationHandler(PsiMethod method, String qualifiedName) {
        try {
            Project project = method.getProject();
            CoverageContext context = CoverageContext.getContextByProject(project);

            if (context == null) {
                JBPopup message = JBPopupFactory.getInstance().createMessage("First run a program with the history runner");
                message.showInFocusCenter();
                return;
            }

            CallStack callStack = context.getCallStack();

            if (callStack == null) {
                showNoStackFrameMessage();
                return;
            }

            List<CallStack> callStacksToShow = new ArrayList<>();

            int lineNumberStart = -1;
            int lineNumberEnd = -1;
            try {
                PsiFile containingFile = method.getContainingFile();
                PsiDocumentManager psiDocumentManager = PsiDocumentManager.getInstance(project);
                Document document = psiDocumentManager.getDocument(containingFile);

                TextRange textRange = method.getTextRange();
                int startOffset = textRange.getStartOffset();
                int endOffset = textRange.getEndOffset();
                lineNumberStart = document.getLineNumber(startOffset) + 1;
                lineNumberEnd = document.getLineNumber(endOffset) + 1;
            } catch (Exception e) {
                LoggingHelper.debug(e);
            }

            if (lineNumberStart == -1 || lineNumberEnd == -1) {
                return;
            }


//            if (containingClass != null && containingClass.getQualifiedName() != null) {//tested before for notnull
            PsiParameter[] parameters = method.getParameterList().getParameters();//notnull
            findMethodInCallStack(callStack, callStacksToShow, method.getName(), qualifiedName, parameters, lineNumberStart, lineNumberEnd);
//            }

            if (callStacksToShow.size() == 0) {
                showNoStackFrameMessage();
            }

            for (int i = 0; i < callStacksToShow.size() && i < MAX_STACK_FRAME_SIZE_TO_SHOW; i++) {
                SequenceAdapter.showVariableView(project, callStacksToShow.get(i), callStacksToShow.get(i));
            }
        } catch (Throwable e) {
            showNoStackFrameMessage();
            LoggingHelper.error(e);
        }
    }

    private void showNoStackFrameMessage() {
        JBPopup message = JBPopupFactory.getInstance().createMessage("No Stack Frame containing this method!");
        message.showInFocusCenter();
    }

    private void findMethodInCallStack(CallStack callStack, List<CallStack> callStacksToShow, String psiMethodName, String psiClassName,
                                       PsiParameter[] psiParameters, int lineNumber, int lineNumberEnd) {
        if (callStack == null) {
            return;
        }

        if (callStacksToShow.size() > MAX_STACK_FRAME_SIZE_TO_SHOW) {//return when ready
            return;
        }

        MethodDescription callStackMethod = callStack.getMethod();

        try {
            if (callStackMethod != null) {
                String methodName = callStackMethod.getMethodName();
                String className = callStackMethod.getClassDescription().getClassName();

                if (className != null) {
                    int indexDollarSign = className.indexOf("$");
                    if (indexDollarSign > 0) {//inner or anonymous
                        className = className.substring(0, indexDollarSign + 1);
                    }

                    if (psiMethodName.equals(methodName)
                            && className.equals(psiClassName)) {
                        if (callStackMethod.getLine() >= lineNumber && callStackMethod.getLine() <= lineNumberEnd) {
                            callStacksToShow.add(callStack);
                        }
                    }
                }
            }
        } catch (Exception e) {
            LoggingHelper.error(e);
        }


        List<CallStack> calls = callStack.getCalls();

        if (calls != null) {
            for (CallStack call : calls) {
                findMethodInCallStack(call, callStacksToShow, psiMethodName, psiClassName, psiParameters, lineNumber, lineNumberEnd);
            }
        }
    }
}
