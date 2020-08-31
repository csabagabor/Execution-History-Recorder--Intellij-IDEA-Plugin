package gabor.history.debug.executor;

import com.intellij.execution.Executor;
import com.intellij.openapi.wm.ToolWindowId;
import gabor.history.ResourcesPlugin;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static gabor.history.debug.DebugResources.WITH_HISTORY_RUNNER;

public class CustomHistoryRunnerExecutor extends Executor {

    @Override
    @NotNull
    public String getToolWindowId() {
        return ToolWindowId.RUN;
    }

    @Override
    @NotNull
    public Icon getToolWindowIcon() {
        return ResourcesPlugin.RUN;
    }

    @Override
    @NotNull
    public Icon getIcon() {
        return ResourcesPlugin.RUN;
    }

    @Override
    public Icon getDisabledIcon() {
        return null;
    }

    @Override
    public String getDescription() {
        return WITH_HISTORY_RUNNER;
    }

    @Override
    @NotNull
    public String getActionName() {
        return WITH_HISTORY_RUNNER;
    }

    @Override
    @NotNull
    public String getId() {
        return ResourcesPlugin.RUNNER_NAME;
    }


    @Override
    @NotNull
    public String getStartActionText() {
        return WITH_HISTORY_RUNNER;
    }

    @Override
    @NotNull
    public String getStartActionText(@NotNull String configurationName) {
        return WITH_HISTORY_RUNNER;
    }

    @Override
    public String getContextActionId() {
        return ResourcesPlugin.RUNNER_ACTION;
    }


    @Override
    public String getHelpId() {
        return null;
    }
}
