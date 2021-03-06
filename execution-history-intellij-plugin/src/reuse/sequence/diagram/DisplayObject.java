package reuse.sequence.diagram;

import com.intellij.ui.JBColor;
import reuse.sequence.config.Configuration;
import org.apache.log4j.Logger;
import reuse.sequence.config.ColorSupport;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class DisplayObject extends ScreenObject {
    private static final Logger LOGGER = Logger.getLogger(DisplayObject.class);

    private static final Paint BORDER_COLOR = JBColor.foreground();
    private static final Paint TEXT_COLOR = Color.DARK_GRAY;
    private static final Paint LINE_COLOR =  JBColor.foreground();
    private static final Paint SHADOW_COLOR = JBColor.LIGHT_GRAY;
    private static final Stroke DASH_STROKE = new BasicStroke(1.0f,
            BasicStroke.CAP_SQUARE,
            BasicStroke.JOIN_MITER,
            12.0f,
            new float[]{12.0f, 6.0f},
            0.0f);

    private int _x = -1;
    private int _y = -1;
    private TextBox _textBox;
    private int _width = -1;
    private int _fullHeight;
    private int _fullWidth;
    private ObjectInfo _objectInfo;

    private List<DisplayLink> _calls = new ArrayList<>();
    private List<DisplayLink> _returns = new ArrayList<DisplayLink>();
    private List<DisplayMethod> _methods = new ArrayList<>();

    DisplayObject(ObjectInfo objectInfo) {
        _objectInfo = objectInfo;
        _textBox = new TextBox(objectInfo.getName());
    }

    void initializeGraphics(Graphics2D g2) {
        _textBox.init(g2);
        for (DisplayLink call : _calls) {
            call.initOne(g2);
        }
        for (DisplayLink aReturn : _returns) {
            aReturn.initOne(g2);
        }
    }

    void translate(int increment) {
        if(LOGGER.isDebugEnabled())
            LOGGER.debug(this + " translate(" + increment + ")");
        _x += increment;
    }

    void addCall(DisplayCall c) {
        _calls.add(c);
    }

    void addCall(DisplaySelfCall c) {
        _calls.add(c);
    }

    List<DisplayLink> getCalls() {
        return _calls;
    }

    void addCallReturn(DisplayCallReturn cr) {
        _returns.add(cr);
    }

    void addCallReturn(DisplaySelfCallReturn cr) {
        _returns.add(cr);
    }

    void addMethod(DisplayMethod displayMethod) {
        if(LOGGER.isDebugEnabled())
            LOGGER.debug("DisplayObject addMethod(" + displayMethod + ")");
        if(_methods.isEmpty()) {
            displayMethod.setHorizontalSeq(0);
        } else {
            int enclosingCount = 0;
            for (DisplayMethod otherMb : _methods) {
                if ((otherMb.getStartSeq() < displayMethod.getStartSeq()) &&
                        (otherMb.getEndSeq() > displayMethod.getEndSeq()))
                    ++enclosingCount;
            }
            displayMethod.setHorizontalSeq(enclosingCount);
        }
        _methods.add(displayMethod);
    }

    public ObjectInfo getObjectInfo() {
        return _objectInfo;
    }

    int getSeq() {
        return _objectInfo.getSeq();
    }

    void setX(int x) {
        _x = x;
    }

    public int getX() {
        return _x;
    }

    void setY(int y) {
        _y = y;
    }

    public int getY() {
        return _y;
    }

    public int getWidth() {
        if(_width == -1)
            return _textBox.getWidth();
        else
            return _width;
    }

    void setWidth(int width) {
        _width = width;
    }

    int getTextWidth() {
        return _textBox.getWidth();
    }

    public int getHeight() {
        return _textBox.getHeight();
    }

    int getCenterX() {
        return getX() + (_textBox.getWidth() / 2);
    }

    int getLeftX(int seq) {
        return getCenterX() - 4;
    }

    int getRightX(int seq) {
//        return getCenterX() + (getMethodDepth(seq) * 4);
        return getCenterX() + getMethodDepth(seq) * 3 + 2;
    }

    public int getFullHeight() {
        return _fullHeight;
    }

    public void setFullHeight(int height) {
        _fullHeight = height;
    }

    public int getFullWidth() {
        return _fullWidth;
    }

    public void setFullWidth(int fullWidth) {
        _fullWidth = fullWidth;
    }

    public String getToolTip() {
        return _objectInfo.getFullName();
    }

    int calcCurrentGap(DisplayObject displayObject, int verticalSeq) {
        if(getSeq() < displayObject.getSeq()) {
            return displayObject.getLeftX(verticalSeq) - getRightX(verticalSeq);
        } else {
            return getLeftX(verticalSeq) - displayObject.getRightX(verticalSeq);
        }
    }

    public int getMethodDepth(int seq) {
        if(LOGGER.isDebugEnabled())
            LOGGER.debug("getMethodDepth(" + seq + ")");
        int depth = 0;
        for (DisplayMethod displayMethod : _methods) {
            if ((displayMethod.getStartSeq() <= seq) && (displayMethod.getEndSeq() >= seq))
                ++depth;
        }
        return depth;
    }

    public DisplayMethod findMethod(int x, int y) {
        DisplayMethod selectedMethodBox = null;
        for (DisplayMethod methodBox : _methods) {
            if (methodBox.isInRange(x, y))
                if ((selectedMethodBox == null || selectedMethodBox.getX() < methodBox.getX()))
                    selectedMethodBox = methodBox;
        }
        return selectedMethodBox;
    }

    public void paint(Graphics2D g2) {
        if(isInClipArea(g2, _fullHeight)) {
            g2.setPaint(LINE_COLOR);
            Stroke oldStroke = g2.getStroke();
            g2.setStroke(DASH_STROKE);
            g2.drawLine(getCenterX(), 0, getCenterX(), _fullHeight);
            g2.setStroke(oldStroke);

            for (DisplayMethod methodBox : _methods) {
                methodBox.paint(g2);
            }
        }

        for (DisplayLink displayLink : _calls) {
//            if (displayLink.getLink().isBootstrap())
//                continue;
            displayLink.paint(g2);
        }
        for (DisplayLink displayLink : _returns) {
            // todo make it configurable
            if (displayLink instanceof DisplaySelfCallReturn /*|| displayLink.getLink().isBootstrap()*/)
                continue;
            if (!Configuration.getInstance().SHOW_RETURN_ARROWS && displayLink instanceof DisplayCallReturn)
                continue;
            if (displayLink.getTo().getObjectInfo().getName().equals(ObjectInfo.ACTOR_NAME))
                continue;
            displayLink.paint(g2);
        }
    }

    private boolean isInClipArea(Graphics2D g2, int height) {
        Rectangle clipBounds = g2.getClipBounds();
        if(clipBounds == null)
            return true;
        return clipBounds.intersects(getX(), 0, getX() + _fullWidth, height);
    }

    public void paintHeader(Graphics2D g2) {
        if(!isInClipArea(g2, getPreferredHeaderHeight()))
            return;
        Configuration configuration = Configuration.getInstance();
        if(configuration.USE_3D_VIEW) {
            g2.setPaint(SHADOW_COLOR);
            g2.fillRect(_x + 2, _y + 2, _textBox.getWidth(), _textBox.getHeight());
        }
        g2.setPaint(determineBackgroundPaintForObject(configuration));
        g2.fillRect(_x, _y, _textBox.getWidth(), _textBox.getHeight());

        g2.setPaint(BORDER_COLOR);
        Stroke oldStroke = g2.getStroke();
        if(isSelected()) {
            g2.setStroke(new BasicStroke(2));
        }
        g2.drawRect(_x, _y, _textBox.getWidth() - 1, _textBox.getHeight() - 1);
        g2.setStroke(oldStroke);

        ColorSupport.lookupMappedColorFor(configuration, _objectInfo.getFullName())
        .ifPresent(paint->{
            // draw a colored overlay, as per user's color mapping config
            int overlayBoxSize = _textBox.getHeight()/2;
            g2.setPaint(ColorSupport.withTransparency((Color)paint,0.8f));
            g2.fillRect(_x-2, _y-2, overlayBoxSize, overlayBoxSize);
        });

        g2.setPaint(TEXT_COLOR);
        Font oldFont = g2.getFont();
        if(_objectInfo.hasAttribute(Info.ABSTRACT_ATTRIBUTE))
            g2.setFont(new Font(oldFont.getFontName(), Font.ITALIC, oldFont.getSize()));
        g2.drawString(_objectInfo.getName(), _x + _textBox.getPad(), _y + _textBox.getTextOffset());
        g2.setFont(oldFont);
    }

    public int getPreferredHeaderHeight() {
        int yDelta = Configuration.getInstance().USE_3D_VIEW? 2: 0;
        return _y + _textBox.getHeight() + yDelta;
    }

    public int getPreferredHeaderWidth() {
        return _x + _textBox.getWidth();
    }

    public String toString() {
        return "DisplayObject " + _objectInfo.getName() + " seq " + _objectInfo.getSeq();
    }

    private Paint determineBackgroundPaintForObject(Configuration configuration) {
        return _objectInfo.hasAttribute("highlight:true")
                ? JBColor.RED
                : configuration.CLASS_COLOR;

    }


}

