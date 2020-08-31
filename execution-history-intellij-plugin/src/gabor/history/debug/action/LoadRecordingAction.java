
package gabor.history.debug.action;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import gabor.history.helper.PluginHelper;
import gabor.history.saver.RecordingSaver;
import org.jetbrains.annotations.NotNull;

import java.io.File;

public class LoadRecordingAction extends RecordingAction {

    @Override
    public void openFile(Project project, @NotNull File file) {
        if (!file.getAbsolutePath().endsWith(PluginHelper.RECORDING_FILE_EXTENSION)) {
            Messages.showErrorDialog(project, "Select a file which has an extension of ." + PluginHelper.RECORDING_FILE_EXTENSION,
                    "Not a Valid Recording File");
        } else {
            RecordingSaver.loadRecordingFromFile(project, file);
        }
    }
}
