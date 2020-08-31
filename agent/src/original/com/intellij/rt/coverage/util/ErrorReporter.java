package original.com.intellij.rt.coverage.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ErrorReporter {
   private static final String ERROR_FILE = "coverage-error.log";
   private static final SimpleDateFormat myDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
   private static String basePath;

   public static synchronized void reportError(String message) {
      PrintStream os = null;

      try {
         os = getErrorLogStream();
         StringBuffer buf = prepareMessage(message);
         System.err.println(buf.toString());
         os.println(buf.toString());
      } catch (IOException var6) {
         System.err.println("Failed to write to error log file: " + var6.toString());
      } finally {
         if (os != null) {
            os.close();
         }

      }

   }

   public static synchronized void reportError(String message, Throwable t) {
      PrintStream os = null;

      try {
         os = getErrorLogStream();
         StringBuffer buf = prepareMessage(message);
         System.err.println(buf.toString() + ": " + t.toString());
         os.println(buf.toString());
         t.printStackTrace(os);
      } catch (IOException var7) {
         System.err.println("Failed to write to error log file: " + var7.toString());
         System.err.println("Initial stack trace: " + t.toString());
      } finally {
         if (os != null) {
            os.close();
         }

      }

   }

   public static synchronized void logError(String message) {
      PrintStream os = null;

      try {
         os = getErrorLogStream();
         StringBuffer buf = prepareMessage(message);
         os.println(buf.toString());
      } catch (IOException var6) {
         System.err.println("Failed to write to error log file: " + var6.toString());
      } finally {
         if (os != null) {
            os.close();
         }

      }

   }

   private static PrintStream getErrorLogStream() throws FileNotFoundException {
      return new PrintStream(new FileOutputStream(basePath != null ? new File(basePath, "coverage-error.log") : new File("coverage-error.log"), true));
   }

   private static StringBuffer prepareMessage(String message) {
      StringBuffer buf = new StringBuffer();
      buf.append("[");
      buf.append(myDateFormat.format(new Date()));
      buf.append("] (Coverage): ");
      buf.append(message);
      return buf;
   }

   public static void setBasePath(String basePath) {
      ErrorReporter.basePath = basePath;
   }
}
