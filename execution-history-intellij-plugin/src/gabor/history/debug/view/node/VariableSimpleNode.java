package gabor.history.debug.view.node;

import com.intellij.debugger.engine.DebuggerUtils;
import com.intellij.icons.AllIcons;
import com.intellij.icons.AllIcons.Debugger;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.treeStructure.SimpleNode;
import com.intellij.util.containers.ContainerUtil;
import gabor.history.action.CoverageContext;
import gabor.history.debug.type.ComplexType;
import gabor.history.debug.type.PlainType;
import gabor.history.debug.type.var.*;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.List;

public class VariableSimpleNode extends SimpleNode {
    private final HistoryVar variable;

    public VariableSimpleNode(HistoryVar variable) {
        this.variable = variable;
    }

    @NotNull
    @Override
    public SimpleNode[] getChildren() {
        List<HistoryVar> fieldVariables = this.variable.getFieldVariables();
        return ContainerUtil.map2Array(fieldVariables, new SimpleNode[fieldVariables.size()],
                VariableSimpleNode::new);
    }

    @Override
    protected void update(@NotNull PresentationData presentation) {
        if (this.variable.getName() != null) {
            if (this.variable.getName().equals(CoverageContext.NO_VARIABLE_NAME)) {
                presentation.addText("Recording does not contain variable information. To include variable information please change the Settings in the Run Configuration menu (slower execution time)", new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN,
                        new JBColor(new Color(255, 141, 129), new Color(255, 141, 129))));
            } else if (this.variable.getName().equals(CoverageContext.NO_VARIABLE_NAME_SAVED)) {
                presentation.addText("File was not saved with variable information", new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN,
                        new JBColor(new Color(255, 141, 129), new Color(255, 141, 129))));
            } else if (this.variable.getName().length() > 0) {
                presentation.addText(this.variable.getName() + " = ", new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN,
                        new JBColor(new Color(255, 141, 129), new Color(255, 141, 129))));
            }
        }

        Object value = this.variable.getValue();
        String name;
        if (value instanceof String) {
            presentation.addText((String) value, new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, JBColor.green));
        } else if (value instanceof ComplexType) {
            name = ((ComplexType) value).getName();
            presentation.addText("{" + name + "}", SimpleTextAttributes.GRAY_ATTRIBUTES);

            if (variable.getSize() >= 0) {
                presentation.addText(" size = " + variable.getSize(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
            }

        } else if (value instanceof PlainType) {
            name = ((PlainType) value).getName();
            presentation.addText(name, SimpleTextAttributes.REGULAR_ATTRIBUTES);
        } else {
            presentation.addText(DebuggerUtils.convertToPresentationString(value.toString()), SimpleTextAttributes.REGULAR_ATTRIBUTES);
        }

        if (value instanceof String) {
            presentation.setTooltip(value.toString());
        }

        if (variable instanceof HistoryEntryVariable) {
            presentation.setIcon(Debugger.Value);
        } else if (variable instanceof HistoryArrayVariable) {
            presentation.setIcon(Debugger.Db_array);
        } else if (variable instanceof HistoryPrimitiveVariable) {
            presentation.setIcon(Debugger.Db_primitive);
        } else if (variable instanceof HistoryEnumVariable) {
            presentation.setIcon(AllIcons.Nodes.Enum);
        } else {
            presentation.setIcon(Debugger.Value);
        }
    }
}
