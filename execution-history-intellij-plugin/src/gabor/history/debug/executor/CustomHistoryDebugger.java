package gabor.history.debug.executor;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.debugger.impl.GenericDebuggerRunnerSettings;
import com.intellij.execution.configurations.ConfigurationInfoProvider;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import gabor.history.ResourcesPlugin;
import org.jetbrains.annotations.NotNull;

public class CustomHistoryDebugger extends GenericDebuggerRunner {

    @NotNull
    @Override
    public String getRunnerId() {
        return ResourcesPlugin.DEBUGGER_NAME;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(ResourcesPlugin.DEBUGGER_NAME) && !(profile instanceof RunConfigurationWithSuppressedDefaultRunAction) &&
                profile instanceof RunConfigurationBase;
    }

    @Override
    public GenericDebuggerRunnerSettings createConfigurationData(@NotNull ConfigurationInfoProvider settingsProvider) {
        return new HistoryExecutorData();
    }
}
