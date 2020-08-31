package original.com.intellij.rt.coverage.instrumentation;

import modified.com.intellij.rt.coverage.data.ClassData;
import modified.com.intellij.rt.coverage.data.ProjectData;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

public class SourceLineCounter extends ClassVisitor {
   private final boolean myExcludeLines;
   private final ClassData myClassData;
   private final ProjectData myProjectData;
   private final TIntObjectHashMap<String> myNSourceLines = new TIntObjectHashMap();
   private final Set<String> myMethodsWithSourceCode = new HashSet();
   private int myTotalBranches = 0;
   private int myCurrentLine;
   private boolean myInterface;
   private boolean myEnum;

   public SourceLineCounter(ClassData classData, boolean excludeLines, ProjectData projectData) {
      super(458752, new ClassVisitor(458752) {
      });
      this.myProjectData = projectData;
      this.myClassData = classData;
      this.myExcludeLines = excludeLines;
   }

   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.myInterface = (access & 512) != 0;
      this.myEnum = (access & 16384) != 0;
      super.visit(version, access, name, signature, superName, interfaces);
   }

   public void visitSource(String sourceFileName, String debug) {
      if (this.myProjectData != null) {
         this.myClassData.setSource(sourceFileName);
      }

      super.visitSource(sourceFileName, debug);
   }

   public void visitOuterClass(String outerClassName, String methodName, String methodSig) {
      if (this.myProjectData != null) {
         this.myProjectData.getOrCreateClassData(outerClassName).setSource(this.myClassData.getSource());
      }

      super.visitOuterClass(outerClassName, methodName, methodSig);
   }

   public MethodVisitor visitMethod(int access, final String name, final String desc, String signature, String[] exceptions) {
      MethodVisitor v = this.cv.visitMethod(access, name, desc, signature, exceptions);
      if (this.myInterface) {
         return v;
      } else if ((access & 64) != 0) {
         return v;
      } else {
         if (this.myEnum) {
            if (name.equals("values") && desc.startsWith("()[L")) {
               return v;
            }

            if (name.equals("valueOf") && desc.startsWith("(Ljava/lang/String;)L")) {
               return v;
            }

            if (name.equals("<init>") && signature != null && signature.equals("()V")) {
               return v;
            }
         }

         return new MethodVisitor(458752, v) {
            private boolean myHasInstructions;

            public void visitLineNumber(int line, Label start) {
               this.myHasInstructions = false;
               SourceLineCounter.this.myCurrentLine = line;
               if (!SourceLineCounter.this.myExcludeLines || SourceLineCounter.this.myClassData == null || SourceLineCounter.this.myClassData.getStatus(name + desc) != null || !name.equals("<init>") && !name.equals("<clinit>")) {
                  SourceLineCounter.this.myNSourceLines.put(line, name + desc);
                  SourceLineCounter.this.myMethodsWithSourceCode.add(name + desc);
               }

            }

            public void visitInsn(int opcode) {
               if (SourceLineCounter.this.myExcludeLines) {
                  if (opcode == 177 && !this.myHasInstructions) {
                     SourceLineCounter.this.myNSourceLines.remove(SourceLineCounter.this.myCurrentLine);
                  } else {
                     this.myHasInstructions = true;
                  }
               }

            }

            public void visitIntInsn(int opcode, int operand) {
               super.visitIntInsn(opcode, operand);
               this.myHasInstructions = true;
            }

            public void visitVarInsn(int opcode, int var) {
               super.visitVarInsn(opcode, var);
               this.myHasInstructions = true;
            }

            public void visitTypeInsn(int opcode, String type) {
               super.visitTypeInsn(opcode, type);
               this.myHasInstructions = true;
            }

            public void visitFieldInsn(int opcode, String owner, String namex, String descx) {
               super.visitFieldInsn(opcode, owner, namex, descx);
               this.myHasInstructions = true;
            }

            public void visitMethodInsn(int opcode, String owner, String namex, String descx, boolean itf) {
               super.visitMethodInsn(opcode, owner, namex, descx, itf);
               this.myHasInstructions = true;
            }

            public void visitJumpInsn(int opcode, Label label) {
               super.visitJumpInsn(opcode, label);
               this.myHasInstructions = true;
               SourceLineCounter.this.myTotalBranches++;
            }

            public void visitLdcInsn(Object cst) {
               super.visitLdcInsn(cst);
               this.myHasInstructions = true;
            }

            public void visitIincInsn(int var, int increment) {
               super.visitIincInsn(var, increment);
               this.myHasInstructions = true;
            }

            public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
               super.visitTableSwitchInsn(min, max, dflt, labels);
               this.myHasInstructions = true;
               SourceLineCounter.this.myTotalBranches++;
            }

            public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
               super.visitLookupSwitchInsn(dflt, keys, labels);
               this.myHasInstructions = true;
               SourceLineCounter.this.myTotalBranches++;
            }

            public void visitMultiANewArrayInsn(String descx, int dims) {
               super.visitMultiANewArrayInsn(descx, dims);
               this.myHasInstructions = true;
            }

            public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
               super.visitTryCatchBlock(start, end, handler, type);
               this.myHasInstructions = true;
            }
         };
      }
   }

   public int getNSourceLines() {
      return this.myNSourceLines.size();
   }

   public TIntObjectHashMap<String> getSourceLines() {
      return this.myNSourceLines;
   }

   public Set<String> getMethodsWithSourceCode() {
      return this.myMethodsWithSourceCode;
   }

   public int getNMethodsWithCode() {
      return this.myMethodsWithSourceCode.size();
   }

   public boolean isInterface() {
      return this.myInterface;
   }

   public int getTotalBranches() {
      return this.myTotalBranches;
   }
}
