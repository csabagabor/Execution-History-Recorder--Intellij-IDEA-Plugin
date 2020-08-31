package gabor.history;

import com.intellij.openapi.util.IconLoader;

import javax.swing.*;

public abstract class ResourcesPlugin {
    public static final Icon RUN = IconLoader.getIcon("/icons/history/run.svg");
    public static final Icon DEBUG = IconLoader.getIcon("/icons/history/debug.svg");
    public static final Icon NO_RECORD_CONSOLE = IconLoader.getIcon("/icons/history/attach-agent.svg");
    public static final Icon RECORD_CONSOLE = IconLoader.getIcon("/icons/history/remove-agent.svg");
    public static final Icon WAITING_CONSOLE = IconLoader.getIcon("/icons/history/waiting-agent.svg");
    public static final String PLUGIN_NAME = "Execution-History-Recorder";

    public static final String RUNNER_NAME = "History-Runner";
    public static final String DEBUGGER_NAME = "History-Debugger";

    public static final String DEBUGGER_ACTION = "History-Debugger-Action";
    public static final String RUNNER_ACTION = "History-Runner-Action";
}
