package gabor.history.debug.view;

import com.intellij.openapi.Disposable;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.tree.AsyncTreeModel;
import com.intellij.ui.tree.StructureTreeModel;
import com.intellij.ui.treeStructure.Tree;
import gabor.history.debug.view.node.VariableSimpleTreeStructure;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.awt.*;

public class VariableTab implements DebugStackFrameListener, Disposable {
    private final JPanel panel;
    private final VariableSimpleTreeStructure treeStructure;
    private final StructureTreeModel<VariableSimpleTreeStructure> treeModel;

    public VariableTab() {
        this.treeStructure = new VariableSimpleTreeStructure();
        this.treeModel = new StructureTreeModel<>(this.treeStructure, this);
        JTree tree = new Tree(new DefaultTreeModel(new DefaultMutableTreeNode()));
        tree.setModel(new AsyncTreeModel(this.treeModel, this));
        tree.setRootVisible(false);

        this.panel = new JPanel(new BorderLayout());
        this.panel.add(new JBScrollPane(tree), "Center");
    }

    @Override
    public void onChanged(@NotNull StackFrameManager stackFrameManager) {
        this.treeStructure.setStackFrameManager(stackFrameManager);
        this.treeModel.invalidate();
    }

    public JPanel getComponent() {
        return this.panel;
    }

    @Override
    public void dispose() {
    }
}
