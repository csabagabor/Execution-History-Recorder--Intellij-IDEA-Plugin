package reuse.sequence.diagram.app.actions;

import reuse.sequence.diagram.Display;
import reuse.sequence.diagram.app.Sequence;
import gabor.history.helper.LoggingHelper;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

public class ExportAction extends SequenceAction {

    private Display _display = null;

    public ExportAction(Display display) {
        super("ExportAction", true);
        _display = display;
    }

    public void actionPerformed(ActionEvent e) {
        final JFileChooser chooser = new JFileChooser();
        chooser.setDialogType(JFileChooser.SAVE_DIALOG);
        chooser.setDialogTitle(getResource("dialogTitle"));

        int returnVal = chooser.showOpenDialog(Sequence.getInstance());
        if(returnVal == JFileChooser.APPROVE_OPTION) {
            export(chooser.getSelectedFile());
        }
    }

    private void export(File file) {
        try {
            _display.saveImageToFile(file);
        } catch(IOException e) {
            LoggingHelper.error(e);
        }
    }
}
