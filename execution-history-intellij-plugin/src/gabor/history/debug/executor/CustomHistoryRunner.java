package gabor.history.debug.executor;

import com.intellij.execution.configurations.ConfigurationInfoProvider;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.execution.impl.DefaultJavaProgramRunner;
import com.intellij.execution.runners.RunConfigurationWithSuppressedDefaultRunAction;
import gabor.history.ResourcesPlugin;
import org.jetbrains.annotations.NotNull;

public class CustomHistoryRunner extends DefaultJavaProgramRunner {
    @NotNull
    @Override
    public String getRunnerId() {
        return ResourcesPlugin.RUNNER_NAME;
    }

    @Override
    public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
        return executorId.equals(ResourcesPlugin.RUNNER_NAME) && !(profile instanceof RunConfigurationWithSuppressedDefaultRunAction) &&
                profile instanceof RunConfigurationBase;
    }

    @Override
    public RunnerSettings createConfigurationData(@NotNull ConfigurationInfoProvider configurationInfoProvider) {
        return new HistoryExecutorData();
    }
}
