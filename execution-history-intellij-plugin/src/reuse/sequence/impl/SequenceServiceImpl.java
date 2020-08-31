package reuse.sequence.impl;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.psi.search.searches.DefinitionsScopedSearch;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.util.Query;
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter;
import gabor.helper.plugin.ToolWindowHelper;
import gabor.history.action.RemoveHistoryRunnerConsoleAction;
import gabor.history.debug.DebugResources;
import gabor.history.helper.LoggingHelper;
import reuse.sequence.SequencePanel;
import reuse.sequence.SequenceService;
import reuse.sequence.generator.CallStack;
import reuse.sequence.generator.SequenceParams;
import reuse.sequence.generator.filters.CompositeMethodFilter;
import reuse.sequence.generator.filters.MethodFilter;
import reuse.sequence.util.PsiUtil;
import icons.SequencePluginIcons;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

/**
 * &copy; fanhuagang@gmail.com
 * Created by van on 2020/2/23.
 */
public class SequenceServiceImpl implements SequenceService {
    public static final String SEQUENCE_DIAGRAM_TOOLWINDOW_NAME = "History Sequence Diagram";
    private static final Icon S_ICON = SequencePluginIcons.SEQUENCE_ICON_13;

    private final Project _project;
    private ToolWindow _toolWindow;
    private final ExecutionPointHighlighter executionPointHighlighter;

    public SequenceServiceImpl(Project project) {

        _project = project;
        //*************************************************************
        //2020.+
//        _toolWindow = ToolWindowManager.getInstance(_project)
//                .registerToolWindow(com.intellij.openapi.wm.RegisterToolWindowTask.closable(PLUGIN_NAME, S_ICON));

        //*************************************************************
        //2019.3+
        try {
            _toolWindow = ToolWindowManager.getInstance(project).
                    registerToolWindow(SEQUENCE_DIAGRAM_TOOLWINDOW_NAME, true, ToolWindowAnchor.BOTTOM, project, true);
            _toolWindow.setIcon(S_ICON);
        } catch (Throwable e) {
            //2020.+
//        _toolWindow = ToolWindowManager.getInstance(_project)
//                .registerToolWindow(com.intellij.openapi.wm.RegisterToolWindowTask.closable(PLUGIN_NAME, S_ICON));

            LoggingHelper.error(e);
            ToolWindowHelper.createToolWindow(project, SEQUENCE_DIAGRAM_TOOLWINDOW_NAME, S_ICON);
        }
        //*************************************************************

        _toolWindow.setAvailable(false, null);
        _toolWindow.getContentManager().addContentManagerListener(new ContentManagerListener() {

            @Override
            public void contentAdded(@NotNull ContentManagerEvent contentManagerEvent) {

            }

            @Override
            public void contentRemoved(@NotNull ContentManagerEvent event) {
                if (_toolWindow.getContentManager().getContentCount() == 0) {
                    _toolWindow.setAvailable(false, null);

                    //if user doesn't know about RemoveHistoryRunnerConsoleAction action
                    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(DebugResources.HISTORY_TAB_NAME);
                    if (toolWindow == null || !toolWindow.isAvailable()) {
                        int dialogResult = JOptionPane.showConfirmDialog(null,
                                "Do you also want to remove highlighters(green/red lines, icons etc) from source files?(if not you can go to Tools -> History Recorder -> " +
                                        " Remove History Recorder highlights anytime", "Sequence Diagram Closed", JOptionPane.YES_NO_OPTION);
                        if (dialogResult == JOptionPane.YES_OPTION) {
                            RemoveHistoryRunnerConsoleAction.removeHighlighters(project);
                        }
                    }
                }
            }

            @Override
            public void contentRemoveQuery(@NotNull ContentManagerEvent contentManagerEvent) {

            }

            @Override
            public void selectionChanged(@NotNull ContentManagerEvent contentManagerEvent) {

            }

        });

        executionPointHighlighter = new ExecutionPointHighlighter(_project);
    }

    @Override
    public void showSequence(SequenceParams params, CallStack callStack, Project project) {
        _toolWindow.setAvailable(true, null);

        if (callStack == null || callStack.getCalls() == null || callStack.getCalls().size() == 0) {
            return;
        }

        try {
            final SequencePanel sequencePanel = new SequencePanel(this, callStack, params, project);

            Runnable postAction = () -> {
                sequencePanel.generate();
                addSequencePanel(sequencePanel);
            };
            if (_toolWindow.isActive())
                _toolWindow.show(postAction);
            else
                _toolWindow.activate(postAction);
        } catch (Exception e) {

        }
    }

    @Override
    public void openClassInEditor(final String className) {
        Query<PsiClass> search = AllClassesSearch.search(GlobalSearchScope.projectScope(_project), _project, className::endsWith);
        PsiClass psiClass = search.findFirst();
        if (psiClass == null)
            return;
        openInEditor(psiClass, psiClass);
    }

    @Override
    public void openMethodInEditor(String className, String methodName, List<String> argTypes) {
        PsiMethod psiMethod = PsiUtil.findPsiMethod(getPsiManager(), className, methodName, argTypes);
        if (psiMethod == null)
            return;
        openInEditor(psiMethod.getContainingClass(), psiMethod);
    }

    @Override
    public boolean isInsideAMethod() {
        return getCurrentPsiMethod() != null;
    }

    @Override
    public void openStackFrameInEditor(MethodFilter filter, String fromClass, String fromMethod, List<String> fromArgTypes,
                                       String toClass, String toMethod, List<String> toArgType, int callNo, int line) {

        PsiMethod fromPsiMethod = PsiUtil.findPsiMethod(getPsiManager(), fromClass, fromMethod, fromArgTypes);
        if (fromPsiMethod == null) {
            return;
        }
        PsiMethod toPsiMethod = PsiUtil.findPsiMethod(getPsiManager(), toClass, toMethod, toArgType);
        if (toPsiMethod == null) {
            return;
        }
        PsiClass containingClass = fromPsiMethod.getContainingClass();

        PsiElement psiElement = PsiUtil.findPsiCallExpression(filter, fromPsiMethod, toPsiMethod, callNo);
        if (psiElement == null) {
            openInEditorByLineNumber(containingClass, line);
            return;
        }


        openInEditor(containingClass, psiElement);
    }

    @Override
    public void openLambdaExprInEditor(String fromClass, String methodName, List<String> methodArgTypes, List<String> argTypes, String returnType) {
        PsiClass containingClass = PsiUtil.findPsiClass(getPsiManager(), fromClass);

        PsiMethod psiMethod = PsiUtil.findPsiMethod(containingClass, methodName, methodArgTypes);
        if (psiMethod == null) return;

        PsiElement psiElement = PsiUtil.findLambdaExpression(psiMethod, argTypes, returnType);

        openInEditor(containingClass, psiElement);

    }

    @Override
    public void openStackFrameInsideLambdaExprInEditor(CompositeMethodFilter methodFilter, String fromClass,
                                                       String enclosedMethodName, List<String> enclosedMethodArgTypes,
                                                       List<String> argTypes, String returnType,
                                                       String toClass, String toMethod, List<String> toArgTypes, int callNo) {
        PsiClass containingClass = PsiUtil.findPsiClass(getPsiManager(), fromClass);

        PsiMethod psiMethod = PsiUtil.findPsiMethod(containingClass, enclosedMethodName, enclosedMethodArgTypes);
        if (psiMethod == null) return;

        PsiLambdaExpression lambdaPsiElement = (PsiLambdaExpression) PsiUtil.findLambdaExpression(psiMethod, argTypes, returnType);

        PsiMethod toPsiMethod = PsiUtil.findPsiMethod(getPsiManager(), toClass, toMethod, toArgTypes);
        if (toPsiMethod == null) {
            return;
        }

        PsiElement psiElement = PsiUtil.findPsiCallExpression(methodFilter, lambdaPsiElement, toPsiMethod, callNo);
        if (psiElement == null) {
            return;
        }

        openInEditor(containingClass, psiElement);
    }

    @Override
    public List<String> findImplementations(String className) {
        PsiClass psiClass = PsiUtil.findPsiClass(getPsiManager(), className);

        if (PsiUtil.isAbstract(psiClass)) {
            PsiElement[] psiElements = DefinitionsScopedSearch.search(psiClass).toArray(PsiElement.EMPTY_ARRAY);
            ArrayList<String> result = new ArrayList<>();

            for (PsiElement element : psiElements) {
                if (element instanceof PsiClass) {
                    PsiClass implClass = (PsiClass) element;
                    result.add(implClass.getQualifiedName());
                }
            }

            return result;
        }
        return new ArrayList<>();

    }

    @Override
    public List<String> findImplementations(String className, String methodName, List<String> argTypes) {
        ArrayList<String> result = new ArrayList<>();

        PsiMethod psiMethod = PsiUtil.findPsiMethod(getPsiManager(), className, methodName, argTypes);
        if (psiMethod == null) return result;

        PsiClass containingClass = psiMethod.getContainingClass();
        if (containingClass == null) {
            containingClass = (PsiClass) psiMethod.getParent().getContext();
        }
        if (PsiUtil.isAbstract(containingClass)) {
            PsiElement[] psiElements = DefinitionsScopedSearch.search(psiMethod).toArray(PsiElement.EMPTY_ARRAY);

            for (PsiElement element : psiElements) {
                if (element instanceof PsiMethod) {

                    PsiMethod method = (PsiMethod) element;
                    PsiClass implClass = method.getContainingClass();
                    if (implClass == null) {
                        implClass = (PsiClass) method.getParent().getContext();
                    }
                    if (implClass != null) {
                        result.add(implClass.getQualifiedName());
                    }
                }
            }

            return result;
        }
        return result;
    }

    private PsiMethod getCurrentPsiMethod() {
        Editor editor = getSelectedEditor();
        if (editor == null)
            return null;
        VirtualFile virtualFile = FileDocumentManager.getInstance().getFile(editor.getDocument());
        if (virtualFile == null)
            return null;
        PsiFile psiFile = getPsiFile(virtualFile);
        return PsiUtil.getEnclosingMethod(psiFile, editor.getCaretModel().getOffset());
    }

    private Editor getSelectedEditor() {
        return getFileEditorManager().getSelectedTextEditor();
    }

    private FileEditorManager getFileEditorManager() {
        return FileEditorManager.getInstance(_project);
    }

    private PsiFile getPsiFile(VirtualFile virtualFile) {
        return PsiManager.getInstance(_project).findFile(virtualFile);
    }

    private void addSequencePanel(final SequencePanel sequencePanel) {
        final Content content = ServiceManager.getService(ContentFactory.class).createContent(sequencePanel, sequencePanel.getTitleName(), false);
        _toolWindow.getContentManager().addContent(content);
        _toolWindow.getContentManager().setSelectedContent(content);
    }

    private PsiManager getPsiManager() {
        return PsiManager.getInstance(_project);
    }

    private void openInEditorByLineNumber(PsiClass psiClass, int line) {
//        VirtualFile virtualFile = PsiUtil.findVirtualFile(psiClass);
//        if (virtualFile == null)
//            return;
//
//        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);
//        int lineStartOffset = document.getLineStartOffset(line);
//
//        getFileEditorManager().openTextEditor(new OpenFileDescriptor(_project,
//                virtualFile, lineStartOffset), true);
//
//        executionPointHighlighter.hide();
//        executionPointHighlighter.show(XSourcePositionImpl.createByOffset(virtualFile, lineStartOffset),
//                false, null);
    }

    private void openInEditor(PsiClass psiClass, PsiElement psiElement) {
        VirtualFile virtualFile = PsiUtil.findVirtualFile(psiClass);
        if (virtualFile == null)
            return;
        getFileEditorManager().openTextEditor(new OpenFileDescriptor(_project,
                virtualFile, psiElement.getTextOffset()), true);
    }
}
