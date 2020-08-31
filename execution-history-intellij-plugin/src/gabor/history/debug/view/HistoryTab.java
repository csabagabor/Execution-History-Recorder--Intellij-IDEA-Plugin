package gabor.history.debug.view;

import com.intellij.openapi.Disposable;

import javax.swing.*;

public interface HistoryTab extends DebugStackFrameListener, Disposable {
    JComponent getCenterComponent();
}
