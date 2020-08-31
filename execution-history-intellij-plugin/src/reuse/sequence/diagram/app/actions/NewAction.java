package reuse.sequence.diagram.app.actions;

import reuse.sequence.diagram.Model;

public class NewAction extends ModifiedConfirmAction {

    public NewAction(Model model) {
        super("NewAction", model, true);
    }

    public boolean doIt() {
        getModel().loadNew();
        return true;
    }


}
