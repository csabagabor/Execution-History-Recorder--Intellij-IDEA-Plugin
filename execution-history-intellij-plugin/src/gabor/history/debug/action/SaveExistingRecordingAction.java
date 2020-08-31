
package gabor.history.debug.action;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.rt.coverage.data.ProjectData;
import gabor.history.action.CoverageContext;
import gabor.history.helper.PluginHelper;
import gabor.history.saver.RecordingSaver;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.File;

public class SaveExistingRecordingAction extends RecordingAction {

    @Override
    public void update(@NotNull AnActionEvent e) {
        Presentation presentation = e.getPresentation();
        Project project = PlatformDataKeys.PROJECT.getData(e.getDataContext());

        if (project == null) {
            presentation.setEnabled(false);
            super.update(e);
            return;
        }

        CoverageContext context = CoverageContext.getContextByProject(project);

        if (context == null) {
            presentation.setEnabled(false);
            super.update(e);
            return;
        }

        ProjectData data = context.getData();

        if (data == null) {//check only data for null (mode = 0 )
            presentation.setEnabled(false);
            super.update(e);
            return;
        }

        presentation.setEnabled(true);
        super.update(e);
    }

    @Override
    public void openFile(Project project, @NotNull File file) {
        if (file.getAbsolutePath().endsWith(PluginHelper.RECORDING_FILE_EXTENSION)) {
            int dialogResult = JOptionPane.showConfirmDialog(null, "Would you like to override it?", "Warning", JOptionPane.YES_NO_OPTION);
            if (dialogResult == JOptionPane.YES_OPTION) {
                RecordingSaver.saveRecordingToFile(project, file);
            }
        } else if (!file.isDirectory()) {
            Messages.showErrorDialog(project, "Please select a folder!",
                    "Not a Directory");
        } else {
            RecordingSaver.saveRecordingToFile(project, file);
        }
    }
}
