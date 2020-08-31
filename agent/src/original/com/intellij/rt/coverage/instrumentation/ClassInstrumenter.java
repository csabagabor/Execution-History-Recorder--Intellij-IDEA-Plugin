package original.com.intellij.rt.coverage.instrumentation;

import modified.com.intellij.rt.coverage.instrumentation.Instrumenter;
import original.com.intellij.rt.coverage.data.LineData;
import modified.com.intellij.rt.coverage.data.ProjectData;
import original.com.intellij.rt.coverage.util.LinesUtil;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

public class ClassInstrumenter extends Instrumenter {
   public ClassInstrumenter(ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldCalculateSource) {
      super(projectData, classVisitor, className, shouldCalculateSource);
   }

   protected MethodVisitor createMethodLineEnumerator(MethodVisitor mv, String name, String desc, int access, String signature, String[] exceptions) {
      return new LineEnumerator(this, mv, access, name, desc, signature, exceptions);
   }

   protected void initLineData() {
      this.myClassData.setLines(LinesUtil.calcLineArray(this.myMaxLineNumber, this.myLines));
   }

   public LineData getLineData(int line) {
      return (LineData)this.myLines.get(line);
   }
}
