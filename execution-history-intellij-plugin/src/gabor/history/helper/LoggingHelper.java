package gabor.history.helper;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

public class LoggingHelper {
    private static LoggingFrame frame;
    private static int nrErrorsWritten = 0;
    private static final Logger log = Logger.getInstance(LoggingHelper.class);
    private static boolean isInDevelopment = false;

    public static void enable() {//works only inside a single project but it's okay because it is for development
        try {
            isInDevelopment = true;//could be removed but it is there to simply set it to false permanently if it won't be shipped to the user
            if (frame == null) {
                frame = new LoggingFrame();
            }
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }

    public static void disable() {
        try {
            isInDevelopment = false;
            if (frame != null) {
                frame.setVisible(false);
                frame.dispose();
                frame = null;
            }
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }

    public static void info(String msg) {
        try {
            if (nrErrorsWritten < 200) {//don't write to manny logs else log file fills up
                nrErrorsWritten++;
                log.info(msg);
            }

            writeToPanel(msg);
        } catch (Throwable e) {
        }
    }

    public static void debug(String msg) {
        try {
            if (isInDevelopment) {
                log.debug(msg);
                writeToPanel(msg);
            }
        } catch (Throwable e) {
        }
    }

    public static void error(Throwable throwable) {
        try {
            if (nrErrorsWritten < 200) {//don't write to manny errors else log file fills up
                nrErrorsWritten++;
                log.error(throwable);
            }

            if (throwable != null) {
                writeToPanel(throwable);
            }
        } catch (Throwable e) {
        }
    }

    public static void debug(Throwable throwable) {
        try {
            //log.debug(throwable);
            if (throwable != null) {
                writeToPanel(throwable);
            }
        } catch (Throwable e) {
        }
    }

    private static class LoggingScrollPanel extends JScrollPane {
        private final JTextArea textArea = new JTextArea();

        public LoggingScrollPanel(@NotNull JPanel panel) {
            super(panel);
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.add(textArea);
            TitledBorder titledBorder = BorderFactory.createTitledBorder("");
            setBorder(BorderFactory.createTitledBorder(titledBorder, "See Logs", TitledBorder.TOP, TitledBorder.TOP,
                    new Font("Serif", Font.BOLD, 12), JBColor.RED));

            this.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

            this.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            getVerticalScrollBar().setUnitIncrement(20);
            setPreferredSize(new Dimension(300, 400));
        }

        public void writeMsg(String msg) {
            textArea.append("\n" + getCurrentTimeStamp() + ":" + msg);
        }

        public void clear() {
            textArea.setText("");
        }

        private String getCurrentTimeStamp() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS").format(new Date());
        }
    }

    private static class LoggingFrame extends JFrame {
        private final LoggingScrollPanel loggingScrollPanel = new LoggingScrollPanel(new JPanel());

        public LoggingFrame() {
            super();
            setResizable(true);
            setTitle("See Plugin Logs");

            this.setPreferredSize(new Dimension(1000, 1000));
            this.pack();
            this.setLocationRelativeTo(null);
            this.setVisible(true);
            this.getContentPane().add(loggingScrollPanel);

            this.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                   isInDevelopment = false;
                }
            });
        }

        public void writeToPanel(String msg) {
            loggingScrollPanel.writeMsg(msg);
        }

        public void clear() {
            loggingScrollPanel.clear();
        }
    }

    private static void writeToPanel(String msg) {
        if (isInDevelopment) {
            System.out.println(msg);
            if (frame != null) {
                frame.writeToPanel(msg);
            }
        }
    }

    private static void writeToPanel(Throwable throwable) {
        if (isInDevelopment) {
            throwable.printStackTrace();
            if (frame != null) {
                frame.writeToPanel("exception::" + throwable.getMessage());
                frame.writeToPanel("trace::" + Arrays.toString(throwable.getStackTrace()));
            }
        }
    }
}
