package reuse.sequence.diagram.app.actions;

import reuse.sequence.diagram.Model;

public class ExitAction extends ModifiedConfirmAction {

    public ExitAction(Model model) {
        super("ExitAction", model, false);
    }

    public boolean doIt() {
        System.exit(0);
        return true;
    }
}
