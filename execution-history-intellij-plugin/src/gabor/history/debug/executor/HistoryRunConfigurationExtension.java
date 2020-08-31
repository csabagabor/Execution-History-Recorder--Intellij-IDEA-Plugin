package gabor.history.debug.executor;

import com.intellij.execution.CommonJavaRunConfigurationParameters;
import com.intellij.execution.RunConfigurationExtension;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.execution.configurations.RunnerSettings;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.WriteExternalException;
import gabor.history.action.CoverageContext;
import gabor.history.debug.DebugResources;
import gabor.history.helper.LoggingHelper;
import gabor.history.helper.PluginHelper;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HistoryRunConfigurationExtension extends RunConfigurationExtension {

    @Override
    public <T extends RunConfigurationBase> void updateJavaParameters(@NotNull T configuration, @NotNull JavaParameters javaParameters, RunnerSettings runnerSettings) {
        try {
            HistoryConfigurationSettingsStore configSettings = HistoryConfigurationSettingsStore.getByConfiguration(configuration);
            CoverageContext context = CoverageContext.createContextByProjectAndFilters(configuration.getProject(), configSettings);

            if (runnerSettings instanceof HistoryExecutorData) {
                context.createAllCoverageFiles();//create files only when using the custom runner, do not slow down user's normal runner/debugger
                context.setLoadTimeAttach(true);
                javaParameters.getVMParametersList().addAll(getStartupParameters(context));
            }
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }


    @Override
    protected void readExternal(@NotNull RunConfigurationBase configurationBase, @NotNull Element element) throws InvalidDataException {
        if (!isApplicableFor(configurationBase)) {
            return;
        }
        HistoryConfigurationSettingsStore configSettings = new HistoryConfigurationSettingsStore();
        configSettings.readExternal(element);
        configurationBase.putCopyableUserData(HistoryConfigurationSettingsStore.KEY, configSettings);
    }

    @Override
    protected void writeExternal(@NotNull RunConfigurationBase runConfiguration, @NotNull Element element) throws WriteExternalException {
        if (!isApplicableFor(runConfiguration)) {
            return;
        }
        HistoryConfigurationSettingsStore configSettings = HistoryConfigurationSettingsStore.getByConfiguration(runConfiguration);
        configSettings.writeExternal(element);
    }


    @Override
    public boolean isApplicableFor(@NotNull RunConfigurationBase configurationBase) {
        return configurationBase instanceof CommonJavaRunConfigurationParameters;
    }

    @Nullable
    @Override
    protected SettingsEditor<RunConfigurationBase> createEditor(@NotNull RunConfigurationBase configuration) {
        return new HistorySettingsEditor(configuration.getProject());
    }

    @Nullable
    @Override
    protected String getEditorTitle() {
        return DebugResources.CONFIGURATION_SERIALIZATION_ID;
    }

    @NotNull
    @Override
    protected String getSerializationId() {
        return DebugResources.CONFIGURATION_SERIALIZATION_ID;
    }

    public List<String> getStartupParameters(CoverageContext coverageContext) {
        Optional<String> pluginPath = PluginHelper.getAgentPath();

        List<String> res = new ArrayList<>();
        if (pluginPath.isPresent()) {
            res.add("-javaagent:" + pluginPath.get() + "=" + coverageContext.getAgentArgString(pluginPath.get()));

            //ClassData and ProjectData needs to be loaded in the client app's code, this prevents NoClassDefFoundError
            res.add("-Xbootclasspath/a:" + pluginPath.get());
            res.add("-noverify");//works for java 8+
        }
        return res;
    }
}
