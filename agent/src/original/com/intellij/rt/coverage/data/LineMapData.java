package original.com.intellij.rt.coverage.data;

public class LineMapData {
   private final int mySourceLineNumber;
   private final int myTargetMinLine;
   private final int myTargetMaxLine;

   public LineMapData(int sourceLineNumber, int targetMinLine, int targetMaxLine) {
      this.mySourceLineNumber = sourceLineNumber;
      this.myTargetMinLine = targetMinLine;
      this.myTargetMaxLine = targetMaxLine;
   }

   public int getTargetMinLine() {
      return this.myTargetMinLine;
   }

   public int getTargetMaxLine() {
      return this.myTargetMaxLine;
   }

   public int getSourceLineNumber() {
      return this.mySourceLineNumber;
   }

   public String toString() {
      return "src: " + this.mySourceLineNumber + ", min: " + this.myTargetMinLine + ", max: " + this.myTargetMaxLine;
   }
}
