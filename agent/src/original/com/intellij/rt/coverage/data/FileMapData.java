package original.com.intellij.rt.coverage.data;

public class FileMapData {
   public static final FileMapData[] EMPTY_FILE_MAP = new FileMapData[0];
   private final String myClassName;
   private final LineMapData[] myLines;

   public FileMapData(String className, LineMapData[] lines) {
      this.myClassName = className;
      this.myLines = lines;
   }

   public String getClassName() {
      return this.myClassName;
   }

   public LineMapData[] getLines() {
      return this.myLines;
   }

   public String toString() {
      StringBuilder toString = new StringBuilder();
      LineMapData[] var2 = this.myLines;
      int var3 = var2.length;

      for(int var4 = 0; var4 < var3; ++var4) {
         LineMapData line = var2[var4];
         if (line != null) {
            toString.append("\n").append(line.toString());
         }
      }

      return "class name: " + this.myClassName + "\nlines:" + toString;
   }
}
