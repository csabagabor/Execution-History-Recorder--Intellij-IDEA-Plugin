package gabor.history.debug.view.node;

import com.intellij.ui.treeStructure.SimpleTreeStructure;
import gabor.history.debug.view.StackFrameManager;
import org.jetbrains.annotations.NotNull;

public class VariableSimpleTreeStructure extends SimpleTreeStructure {
    private final VariableRootSimpleNode simpleRoot;

    public VariableSimpleTreeStructure() {
        this.simpleRoot = new VariableRootSimpleNode();
    }

    @NotNull
    @Override
    public VariableRootSimpleNode getRootElement() {
        return this.simpleRoot;
    }

    public void setStackFrameManager(StackFrameManager stackFrameManager) {
        this.simpleRoot.setStackFrameManager(stackFrameManager);
    }
}
