package gabor.history.debug.executor;

import com.intellij.execution.Executor;
import com.intellij.openapi.wm.ToolWindowId;
import gabor.history.ResourcesPlugin;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

import static gabor.history.debug.DebugResources.WITH_HISTORY_DEBUGGER;

public class CustomHistoryDebuggerExecutor extends Executor {

    @NotNull
    @Override
    public String getToolWindowId() {
        return ToolWindowId.DEBUG;
    }

    @NotNull
    @Override
    public Icon getToolWindowIcon() {
        return ResourcesPlugin.DEBUG;
    }

    @Override
    @NotNull
    public Icon getIcon() {
        return ResourcesPlugin.DEBUG;
    }

    @Override
    public Icon getDisabledIcon() {
        return null;
    }

    @Override
    public String getDescription() {
        return WITH_HISTORY_DEBUGGER;
    }

    @Override
    @NotNull
    public String getActionName() {
        return WITH_HISTORY_DEBUGGER;
    }

    @Override
    @NotNull
    public String getId() {
        return ResourcesPlugin.DEBUGGER_NAME;
    }


    @Override
    @NotNull
    public String getStartActionText() {
        return WITH_HISTORY_DEBUGGER;
    }

    @Override
    @NotNull
    public String getStartActionText(@NotNull String configurationName) {
        return WITH_HISTORY_DEBUGGER;
    }

    @Override
    public String getContextActionId() {
        return ResourcesPlugin.DEBUGGER_ACTION;
    }

    @Override
    public String getHelpId() {
        return null;
    }

}
