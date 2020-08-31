package original.com.intellij.rt.coverage.util;

import org.jetbrains.coverage.gnu.trove.TLongObjectHashMap;

public class StringsPool {
   private static final TLongObjectHashMap<String> myReusableStrings = new TLongObjectHashMap(30000);
   private static final String EMPTY = "";

   public static String getFromPool(String value) {
      if (value == null) {
         return null;
      } else if (value.length() == 0) {
         return "";
      } else {
         long hash = StringHash.calc(value);
         String reused = (String)myReusableStrings.get(hash);
         if (reused != null) {
            return reused;
         } else {
            reused = new String(value);
            myReusableStrings.put(hash, reused);
            return reused;
         }
      }
   }
}
