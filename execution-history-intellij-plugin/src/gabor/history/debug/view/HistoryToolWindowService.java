
package gabor.history.debug.view;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory.SERVICE;
import com.intellij.ui.content.ContentManager;
import com.intellij.ui.content.ContentManagerEvent;
import com.intellij.ui.content.ContentManagerListener;
import com.intellij.xdebugger.impl.ui.ExecutionPointHighlighter;
import gabor.helper.plugin.ToolWindowHelper;
import gabor.history.ResourcesPlugin;
import gabor.history.action.CoverageContext;
import gabor.history.action.RemoveHistoryRunnerConsoleAction;
import gabor.history.debug.DebugIcons;
import gabor.history.debug.DebugResources;
import gabor.history.debug.type.StackFrame;
import gabor.history.helper.LoggingHelper;
import org.jetbrains.annotations.NotNull;
import reuse.sequence.impl.SequenceServiceImpl;

import javax.swing.*;
import java.util.List;

public class HistoryToolWindowService {
    private final Project project;
    private final ExecutionPointHighlighter executionPointHighlighter;
    private ToolWindow _toolWindow;

    public static HistoryToolWindowService getInstance(Project project) {
        return ServiceManager.getService(project, HistoryToolWindowService.class);
    }

    public HistoryToolWindowService(Project project) {
        this.project = project;
        executionPointHighlighter = new ExecutionPointHighlighter(project);


        //2019.3+
        try {
            _toolWindow = ToolWindowManager.getInstance(project).
                    registerToolWindow(DebugResources.HISTORY_TAB_NAME, true, ToolWindowAnchor.BOTTOM, project, true);
            _toolWindow.setIcon(DebugIcons.VARIABLES_TAB_ICON);
        } catch (Throwable e) {
            LoggingHelper.error(e);

            //2020+
//        _toolWindow = ToolWindowManager.getInstance(project)
//                .registerToolWindow(com.intellij.openapi.wm.RegisterToolWindowTask.closable(DebugResources.HISTORY_TAB_NAME, DebugIcons.VARIABLES_TAB_ICON));

            _toolWindow = ToolWindowHelper.createToolWindow(project, DebugResources.HISTORY_TAB_NAME, DebugIcons.VARIABLES_TAB_ICON);
        }

        //*************************************************************
        _toolWindow.getContentManager().addContentManagerListener(new ContentManagerListener() {
            @Override
            public void contentAdded(@NotNull ContentManagerEvent contentManagerEvent) {

            }

            @Override
            public void contentRemoved(@NotNull ContentManagerEvent event) {
                try {
                    if (_toolWindow.getContentManager().getContentCount() == 0) {
                        _toolWindow.setAvailable(false, null);
                    }
                    //remove hovering
                    CoverageContext context = CoverageContext.getContextByProject(project);
                    if (context != null) {
                        context.resetVarCache();
                    }

                    //if user doesn't know about RemoveHistoryRunnerConsoleAction action
                    ToolWindow toolWindow = ToolWindowManager.getInstance(project).getToolWindow(SequenceServiceImpl.SEQUENCE_DIAGRAM_TOOLWINDOW_NAME);
                    if (toolWindow == null || !toolWindow.isAvailable()) {
                        int dialogResult = JOptionPane.showConfirmDialog(null,
                                "Do you also want to remove highlighters(green/red lines, icons etc) from source files?(if not you can go to Tools -> History Recorder -> " +
                                        " Remove History Recorder highlights anytime", "Stack Trace Window Closed", JOptionPane.YES_NO_OPTION);
                        if (dialogResult == JOptionPane.YES_OPTION) {
                            RemoveHistoryRunnerConsoleAction.removeHighlighters(project);
                        }
                    }

                } catch (Throwable e) {
                    LoggingHelper.error(e);
                }
            }

            @Override
            public void contentRemoveQuery(@NotNull ContentManagerEvent contentManagerEvent) {

            }

            @Override
            public void selectionChanged(@NotNull ContentManagerEvent event) {
                try {
                    Disposable disposable = event.getContent().getDisposer();
                    if (disposable instanceof HistoryDebuggerTab) {
                        if (event.getOperation() == ContentManagerEvent.ContentOperation.add) {
                            //change execution point depending on opened tab
                            ((HistoryDebuggerTab) disposable).showExecutionLine();
                        }
                    }
                } catch (Throwable e) {
                    LoggingHelper.error(e);
                }
            }
        });
    }

    public void showToolWindow(@NotNull List<StackFrame> stackFrames, @NotNull StackFrame currentFrame) {
        //hide all previously marked lines
        removeExecutionShower();

        HistoryDebuggerTab debuggerTab = new HistoryDebuggerTab(this.project, this.executionPointHighlighter);
        debuggerTab.showFrames(stackFrames, currentFrame);

        Content content = SERVICE.getInstance().createContent(debuggerTab.getLayoutComponent(), DebugResources.TAB_NAME, false);
        content.setDisposer(debuggerTab);

        ContentManager contentManager = _toolWindow.getContentManager();
        contentManager.addContent(content);
        contentManager.setSelectedContent(content);
        _toolWindow.setAvailable(true, null);
        if (_toolWindow.isActive()) {
            _toolWindow.show(null);
        } else {
            _toolWindow.activate(null);
        }
    }

    public void removeExecutionShower() {
        executionPointHighlighter.hide();
    }
}
