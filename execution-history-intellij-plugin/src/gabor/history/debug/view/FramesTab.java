

package gabor.history.debug.view;

import com.intellij.icons.AllIcons.Debugger;
import com.intellij.openapi.Disposable;
import com.intellij.ui.CollectionListModel;
import com.intellij.ui.ColoredListCellRenderer;
import com.intellij.ui.JBColor;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import gabor.history.debug.type.StackFrame;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class FramesTab implements DebugStackFrameListener, Disposable {
    private final JList<StackFrame> stackFrameList;
    private final HistoryDebuggerTab tab;
    private final JPanel panel;

    public FramesTab(HistoryDebuggerTab tab) {
        this.tab = tab;
        this.panel = new JPanel(new BorderLayout());

        this.stackFrameList = new JBList<>();
        this.stackFrameList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        addListeners();

        this.panel.add(new JBScrollPane(this.stackFrameList), "Center");
    }

    public JPanel getComponent() {
        return this.panel;
    }

    @Override
    public void onChanged(@NotNull StackFrameManager stackFrameManager) {
        StackFrame currentFrame = stackFrameManager.getCurrentFrame();
        this.stackFrameList.setModel(new CollectionListModel<>(stackFrameManager.getStackFrames()));
        this.stackFrameList.setSelectedValue(currentFrame, true);
    }

    private void addListeners() {
        this.stackFrameList.setCellRenderer(new ColoredListCellRenderer<StackFrame>() {
            @Override
            protected void customizeCellRenderer(@NotNull JList<? extends StackFrame> jList, StackFrame stackFrame, int i, boolean b, boolean b1) {
                this.setIcon(Debugger.Frame);

                if (stackFrame.isProjectClass()) {
                    this.append(stackFrame.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES);
                } else {
                    this.append(stackFrame.toString(), SimpleTextAttributes.GRAY_ATTRIBUTES);
                }
            }
        });

        this.stackFrameList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                StackFrame stackFrame = this.stackFrameList.getSelectedValue();

                if (stackFrame != null) {
                    this.stackFrameList.setSelectedValue(stackFrame, true);
                    tab.getStackFrameManager().setCurrentFrame(stackFrame);
                    tab.onStackFrameUpdated();
                }
            }
        });
    }

    @Override
    public void dispose() {

    }
}
