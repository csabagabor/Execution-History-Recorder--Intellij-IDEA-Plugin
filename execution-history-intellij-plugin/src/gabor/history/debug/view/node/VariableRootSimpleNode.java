
package gabor.history.debug.view.node;

import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.containers.hash.LinkedHashMap;
import gabor.history.debug.type.var.HistoryVar;
import gabor.history.debug.view.StackFrameManager;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Map;

public class VariableRootSimpleNode extends SimpleNode {
    private StackFrameManager stackFrameManager;

    public void setStackFrameManager(StackFrameManager currentStackFrameManager) {
        this.stackFrameManager = currentStackFrameManager;
    }

    @NotNull
    @Override
    public SimpleNode[] getChildren() {
        try {
            if (this.stackFrameManager == null || this.stackFrameManager.getCurrentFrame() == null
                    || this.stackFrameManager.getCurrentFrame().getVars() == null) {
                return SimpleNode.NO_CHILDREN;
            } else {
                List<HistoryVar> vars = this.stackFrameManager.getCurrentFrame().getVars();
                Map<String, VariableSimpleNode> simpleNodeMap = new LinkedHashMap<>();

                try {
                    for (HistoryVar var : vars) {
                        if (!"".equals(var.getName()) && var.getName() != null) {
                            simpleNodeMap.put(var.getName(), new VariableSimpleNode(var));
                        }
                    }
                    return simpleNodeMap.values().toArray(new SimpleNode[0]);
                } catch (Exception e) {//return as many vars as possible even in case of an exception
                    return simpleNodeMap.values().toArray(new SimpleNode[0]);
                }
            }
        } catch (Throwable e) {
            return SimpleNode.NO_CHILDREN;
        }
    }
}
