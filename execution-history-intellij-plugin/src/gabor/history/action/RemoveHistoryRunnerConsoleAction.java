package gabor.history.action;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.project.Project;
import gabor.history.debug.view.HistoryToolWindowService;
import gabor.history.helper.LoggingHelper;

public class RemoveHistoryRunnerConsoleAction extends AnAction {

    @Override
    public void actionPerformed(final AnActionEvent event) {
        final Project project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
        removeHighlighters(project);

        super.update(event);
    }

    public static void removeHighlighters(Project project) {
        try {
            CoverageContext context = CoverageContext.getContextByProject(project);
            if (context != null) {
                context.reset();
                context.removeHighlighters();
            }
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }

        //remove covered classes next to classes' name
        try {
            ProjectView.getInstance(project).refresh();
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }

        //remove stack frame button next to methods
        try {
            com.intellij.codeInsight.daemon.DaemonCodeAnalyzer.getInstance(project).restart();
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }


        //remove execution line shower
        try {
            HistoryToolWindowService.getInstance(project).removeExecutionShower();
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }

    @Override
    public String toString() {
        return "Remove History Recorder";
    }
}
