
package gabor.history.debug.view;

import gabor.history.debug.type.StackFrame;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class StackFrameManager {
    private StackFrame currentFrame;
    private final List<StackFrame> stackFrames;

    public StackFrameManager(@NotNull List<StackFrame> stackFrames) {
        this.stackFrames = stackFrames;
    }

    public StackFrame getCurrentFrame() {
        return this.currentFrame;
    }

    public List<StackFrame> getStackFrames() {
        return stackFrames;
    }

    public void setCurrentFrame(StackFrame currentFrame) {
        this.currentFrame = currentFrame;
    }
}
