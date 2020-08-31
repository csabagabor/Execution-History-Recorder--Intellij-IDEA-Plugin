package gabor.helper.plugin;

import com.intellij.ide.plugins.IdeaPluginDescriptor;

import java.nio.file.Path;

public class PluginPathHelper {
    private PluginPathHelper() {
    }

    public static Path getPluginPath(IdeaPluginDescriptor plugin) {
        return plugin.getPluginPath();
    }
}
