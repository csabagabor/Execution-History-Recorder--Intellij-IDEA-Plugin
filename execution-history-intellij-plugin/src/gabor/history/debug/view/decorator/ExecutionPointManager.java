package gabor.history.debug.view.decorator;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiManager;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter;
import gabor.history.action.CoverageContext;
import gabor.history.debug.type.StackFrame;
import gabor.history.debug.type.var.HistoryVar;
import gabor.history.debug.view.DebugStackFrameListener;
import gabor.history.debug.view.StackFrameManager;
import gabor.history.helper.LoggingHelper;
import org.jetbrains.annotations.NotNull;
import reuse.sequence.util.PsiUtil;

import java.util.List;

public class ExecutionPointManager implements DebugStackFrameListener, Disposable {
    private final Project project;
    private final ExecutionPointHighlighter executionPointHighlighter;

    public ExecutionPointManager(Project project, @NotNull ExecutionPointHighlighter executionPointHighlighter) {
        this.project = project;
        this.executionPointHighlighter = executionPointHighlighter;
    }

    @Override
    public void onChanged(@NotNull StackFrameManager stackFrameManager) {
        StackFrame currentFrame = stackFrameManager.getCurrentFrame();

        String fromClass = currentFrame.getFullClassName();

        //check for inner class
        int indexOfDollarSign = fromClass.indexOf("$");
        if (indexOfDollarSign >= 0) {
            fromClass = fromClass.substring(0, indexOfDollarSign);
        }


        PsiClass containingClass = PsiUtil.findPsiClass(PsiManager.getInstance(project), fromClass);

        if (containingClass == null) {
            return;
        }

        VirtualFile virtualFile = PsiUtil.findVirtualFile(containingClass);
        if (virtualFile == null) {
            return;
        }

        Document document = FileDocumentManager.getInstance().getDocument(virtualFile);

        if (document == null) {
            return;
        }

        int lineStartOffset = document.getLineStartOffset(currentFrame.getLine());
        try {
            FileEditorManager.getInstance(project).openTextEditor(new OpenFileDescriptor(project,
                    virtualFile, lineStartOffset), true);

            executionPointHighlighter.hide();
            executionPointHighlighter.show(XSourcePositionImpl.createByOffset(virtualFile, lineStartOffset),
                    false, null);
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }

        try {
            int minLine = currentFrame.getLine() - 100;
            if (minLine <= 1) {
                minLine = 1;
            }
            int minOffset = document.getLineNumber(minLine);
            //show inline variable information
            CoverageContext context = CoverageContext.getContextByProject(project);

            if (context == null) {
                return;
            }

            List<HistoryVar> vars = currentFrame.getVars();

            //first remove hovering
            context.resetVarCache();

            String methodName = currentFrame.getName();
            if (vars != null && !vars.isEmpty() && methodName != null) {
                context.calcVarCache(virtualFile, vars, lineStartOffset, minOffset, containingClass, methodName);
            }
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }

    }

    @Override
    public void dispose() {
        executionPointHighlighter.hide();
    }
}
