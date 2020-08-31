package original.com.intellij.rt.coverage.util;

import original.com.intellij.rt.coverage.data.LineData;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;

public class LinesUtil {
   public static LineData[] calcLineArray(int maxLineNumber, TIntObjectHashMap lines) {
      LineData[] linesArray = new LineData[maxLineNumber + 1];

      for(int line = 1; line <= maxLineNumber; ++line) {
         LineData lineData = (LineData)lines.get(line);
         if (lineData != null) {
            lineData.fillArrays();
         }

         linesArray[line] = lineData;
      }

      return linesArray;
   }
}
