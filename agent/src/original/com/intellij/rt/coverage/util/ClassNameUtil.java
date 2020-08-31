package original.com.intellij.rt.coverage.util;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

public class ClassNameUtil {
   public static String getOuterClassName(String className) {
      int idx = className.indexOf(36);
      return idx == -1 ? className : className.substring(0, idx);
   }

   public static boolean shouldExclude(String className, List<Pattern> excludePatterns) {
      Iterator var2 = excludePatterns.iterator();

      Pattern excludePattern;
      do {
         if (!var2.hasNext()) {
            return false;
         }

         excludePattern = (Pattern)var2.next();
      } while(!excludePattern.matcher(className).matches());

      return true;
   }

   public static String convertToFQName(String className) {
      return className.replace('\\', '.').replace('/', '.');
   }
}
