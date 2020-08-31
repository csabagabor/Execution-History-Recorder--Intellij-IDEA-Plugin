package gabor.helper.plugin;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;

public class ToolWindowHelper {
    private ToolWindowHelper() {
    }

    //Intellij 2020+
    public static ToolWindow createToolWindow(Project project, String name, javax.swing.Icon icon) {
        return ToolWindowManager.getInstance(project)
                .registerToolWindow(com.intellij.openapi.wm.RegisterToolWindowTask.closable(name, icon));
    }
}

