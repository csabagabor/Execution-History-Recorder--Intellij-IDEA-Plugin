

package gabor.history.debug.view;

import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.RunnerLayoutUi.Factory;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.icons.AllIcons.Debugger;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.content.Content;
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter;
import gabor.history.debug.DebugResources;
import gabor.history.debug.type.StackFrame;
import gabor.history.debug.view.decorator.ExecutionPointManager;
import gabor.history.helper.LoggingHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class HistoryDebuggerTab implements Disposable {
    private final List<DebugStackFrameListener> listeners;
    private StackFrameManager stackFrameManager;
    private final RunnerLayoutUi layoutUi;
    private final ExecutionPointManager executionPointManager;

    public HistoryDebuggerTab(Project project, ExecutionPointHighlighter executionPointHighlighter) {
        this.listeners = new CopyOnWriteArrayList<>();//CopyOnWriteArrayList is common for listeners inside Intellij Community source code

        this.layoutUi = Factory.getInstance(project).create(DebugResources.HISTORY_RECORDER, DebugResources.HISTORY_RECORDER, DebugResources.HISTORY_RECORDER, this);
        this.executionPointManager = new ExecutionPointManager(project, executionPointHighlighter);
        Disposer.register(this, this.executionPointManager);
        this.listeners.add(this.executionPointManager);

        addFramesTab();
        addVariableTab();
    }

    private void addVariableTab() {
        VariableTab variableTab = new VariableTab();
        Content content = this.layoutUi.createContent(DebugResources.HISTORY_RECORDER_VARIABLES, variableTab.getComponent(), "Variables",
                Debugger.VariablesTab, null);
        content.setCloseable(false);
        this.layoutUi.addContent(content, 0, PlaceInGrid.center, false);

        this.listeners.add(variableTab);
        Disposer.register(this, variableTab);
    }

    private void addFramesTab() {
        FramesTab framesTab = new FramesTab(this);
        Content content = this.layoutUi.createContent(DebugResources.HISTORY_RECORDER_STACK_FRAMES, framesTab.getComponent(), "Frames",
                Debugger.Frame, null);
        content.setCloseable(false);
        this.layoutUi.addContent(content, 0, PlaceInGrid.left, false);

        this.listeners.add(framesTab);
        Disposer.register(this, framesTab);
    }


    public void onStackFrameUpdated() {
        for (DebugStackFrameListener listener : this.listeners) {
            if (listener instanceof VariableTab || listener instanceof ExecutionPointManager) {
                try {
                    listener.onChanged(stackFrameManager);
                } catch (Throwable e) {
                    LoggingHelper.error(e);
                }
            }
        }
    }

    public void showFrames(@NotNull List<StackFrame> stackFrames, @NotNull StackFrame currentFrame) {
        stackFrameManager = new StackFrameManager(stackFrames);
        stackFrameManager.setCurrentFrame(currentFrame);

        for (DebugStackFrameListener listener : this.listeners) {
            try {
                listener.onChanged(stackFrameManager);
            } catch (Throwable e) {
                LoggingHelper.error(e);
            }
        }
    }

    public void showExecutionLine() {
        StackFrameManager stackFrameManager = this.getStackFrameManager();
        if (stackFrameManager != null) {
            try {
                this.executionPointManager.onChanged(stackFrameManager);
            } catch (Throwable e) {
                LoggingHelper.debug(e);
            }
        }
    }

    public StackFrameManager getStackFrameManager() {
        return this.stackFrameManager;
    }

    public JComponent getLayoutComponent() {
        return this.layoutUi.getComponent();
    }

    @Override
    public void dispose() {

    }
}
