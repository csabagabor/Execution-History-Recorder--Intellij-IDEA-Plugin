package gabor.history.marker;

import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.markup.LineMarkerRendererEx;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class DefaultLineMarkerRenderer implements LineMarkerRendererEx {
    private final int thickness;
    private final int depth;
    private final Position position;

    private final Color color;

    public DefaultLineMarkerRenderer(int thickness, Color color, Position position) {
        this.thickness = thickness;
        depth = 0;
        this.position = position;
        this.color = color;
    }

    @Override
    public void paint(Editor editor, Graphics g, Rectangle r) {
        if (color == null) return;

        g.setColor(color);
        g.fillRect(r.x, r.y, thickness, r.height);
        g.fillRect(r.x + thickness, r.y, depth, thickness);
        g.fillRect(r.x + thickness, r.y + r.height - thickness, depth, thickness);
    }

    @NotNull
    @Override
    public Position getPosition() {
        return position;
    }
}
