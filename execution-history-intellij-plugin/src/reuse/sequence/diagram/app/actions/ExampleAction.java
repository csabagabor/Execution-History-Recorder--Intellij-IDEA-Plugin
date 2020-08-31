package reuse.sequence.diagram.app.actions;

import reuse.sequence.diagram.Model;

public class ExampleAction extends ModifiedConfirmAction {

    public ExampleAction(Model model) {
        super("ExampleAction", model, true);
    }

    public boolean doIt() {
        getModel().setText(getResource("exampleModelText"), this);
        getModel().setModified(false);
        return true;
    }
}
