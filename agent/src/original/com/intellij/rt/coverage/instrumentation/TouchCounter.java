package original.com.intellij.rt.coverage.instrumentation;

import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.Type;

public class TouchCounter extends MethodVisitor implements Opcodes {
   private final int myVariablesCount;
   private final LineEnumerator myEnumerator;
   private Label myStartLabel;
   private Label myEndLabel;
   private int myCurrentLine;
   private int myCurrentJumpIdx;
   private int myCurrentSwitchIdx;
   private int myLastJump = -1;
   private int myLastLineJump = -1;
   private static final byte SEEN_NOTHING = 0;
   private static final byte GETSTATIC_SEEN = 1;
   private byte myState;

   public TouchCounter(LineEnumerator enumerator, int access, String desc) {
      super(458752, enumerator.getWV());
      this.myEnumerator = enumerator;
      int variablesCount = (8 & access) != 0 ? 0 : 1;
      Type[] args = Type.getArgumentTypes(desc);
      Type[] var6 = args;
      int var7 = args.length;

      for(int var8 = 0; var8 < var7; ++var8) {
         Type arg = var6[var8];
         variablesCount += arg.getSize();
      }

      this.myVariablesCount = variablesCount;
   }

   public void visitLineNumber(int line, Label start) {
      this.myCurrentLine = line;
      this.myCurrentJumpIdx = 0;
      this.myCurrentSwitchIdx = 0;
      this.mv.visitVarInsn(25, this.getCurrentClassDataNumber());
      this.pushLineNumber(line);
      this.mv.visitMethodInsn(184, "modified/com/intellij/rt/coverage/data/ProjectData", "trace", "(Ljava/lang/Object;I)V", false);
      super.visitLineNumber(line, start);
   }

   public void visitLabel(Label label) {
      if (this.myStartLabel == null) {
         this.myStartLabel = label;
      }

      this.myEndLabel = label;
      super.visitLabel(label);
      boolean isJump = this.myEnumerator.isJump(label);
      Label l;
      if (this.myLastJump != -1) {
         l = new Label();
         this.mv.visitVarInsn(21, this.getLineVariableNumber());
         this.pushLineNumber(this.myLastLineJump);
         this.mv.visitJumpInsn(160, l);
         this.mv.visitVarInsn(21, this.getJumpVariableNumber());
         this.mv.visitIntInsn(17, this.myLastJump);
         this.mv.visitJumpInsn(160, l);
         this.touchLastJump();
         if (isJump) {
            Label l1 = new Label();
            this.mv.visitJumpInsn(167, l1);
            this.mv.visitLabel(l);
            this.mv.visitVarInsn(21, this.getJumpVariableNumber());
            this.mv.visitJumpInsn(155, l1);
            this.touchBranch(true);
            this.mv.visitLabel(l1);
         } else {
            this.mv.visitLabel(l);
         }
      } else if (isJump) {
         this.mv.visitVarInsn(21, this.getJumpVariableNumber());
         l = new Label();
         this.mv.visitJumpInsn(155, l);
         this.touchBranch(true);
         this.mv.visitLabel(l);
      }

      Integer key = this.myEnumerator.getSwitchKey(label);
      if (key != null) {
         this.mv.visitVarInsn(25, this.getCurrentClassDataNumber());
         this.mv.visitVarInsn(21, this.getLineVariableNumber());
         this.mv.visitVarInsn(21, this.getSwitchVariableNumber());
         this.mv.visitIntInsn(17, key);
         this.mv.visitMethodInsn(184, "modified/com/intellij/rt/coverage/data/ProjectData", "touchSwitch", "(Ljava/lang/Object;III)V", false);
      }

   }

   private void touchBranch(boolean trueHit) {
      this.mv.visitVarInsn(25, this.getCurrentClassDataNumber());
      this.mv.visitVarInsn(21, this.getLineVariableNumber());
      this.mv.visitVarInsn(21, this.getJumpVariableNumber());
      this.mv.visitInsn(trueHit ? 3 : 4);
      this.mv.visitMethodInsn(184, "modified/com/intellij/rt/coverage/data/ProjectData", "touchJump", "(Ljava/lang/Object;IIZ)V", false);
      this.mv.visitIntInsn(17, -1);
      this.mv.visitVarInsn(54, this.getJumpVariableNumber());
   }

   private void touchLastJump() {
      if (this.myLastJump != -1) {
         this.myLastJump = -1;
         this.touchBranch(false);
      }

      this.myState = 0;
   }

   public void visitJumpInsn(int opcode, Label label) {
      byte state = this.myState;
      this.touchLastJump();
      if (opcode != 167 && opcode != 168 && !this.myEnumerator.getMethodName().equals("<clinit>") && this.myEnumerator.isJump(label) && (state != 1 || opcode != 154)) {
         this.myLastJump = this.myCurrentJumpIdx;
         this.myLastLineJump = this.myCurrentLine;
         this.pushLineNumber(this.myCurrentLine);
         this.mv.visitVarInsn(54, this.getLineVariableNumber());
         this.mv.visitIntInsn(17, this.myCurrentJumpIdx++);
         this.mv.visitVarInsn(54, this.getJumpVariableNumber());
      }

      super.visitJumpInsn(opcode, label);
   }

   public void visitCode() {
      this.mv.visitInsn(3);
      this.mv.visitVarInsn(54, this.getLineVariableNumber());
      this.mv.visitIntInsn(17, -1);
      this.mv.visitVarInsn(54, this.getJumpVariableNumber());
      this.mv.visitInsn(3);
      this.mv.visitVarInsn(54, this.getSwitchVariableNumber());
      this.mv.visitLdcInsn(this.myEnumerator.getClassName());
      this.mv.visitMethodInsn(184, "modified/com/intellij/rt/coverage/data/ProjectData", "loadClassData", "(Ljava/lang/String;)Ljava/lang/Object;", false);
      this.mv.visitVarInsn(58, this.getCurrentClassDataNumber());
      super.visitCode();
   }

   public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      this.touchLastJump();
      this.storeSwitchDescriptor();
      super.visitLookupSwitchInsn(dflt, keys, labels);
   }

   public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
      this.touchLastJump();
      this.storeSwitchDescriptor();
      super.visitTableSwitchInsn(min, max, dflt, labels);
   }

   private void storeSwitchDescriptor() {
      this.pushLineNumber(this.myCurrentLine);
      this.mv.visitVarInsn(54, this.getLineVariableNumber());
      this.mv.visitIntInsn(17, this.myCurrentSwitchIdx++);
      this.mv.visitVarInsn(54, this.getSwitchVariableNumber());
   }

   private void pushLineNumber(int line) {
      if (line <= 32767) {
         this.mv.visitIntInsn(17, line);
      } else {
         this.mv.visitLdcInsn(line);
      }

   }

   public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      this.touchLastJump();
      if (opcode == 178 && name.equals("$assertionsDisabled")) {
         this.myState = 1;
      }

      super.visitFieldInsn(opcode, owner, name, desc);
   }

   public void visitInsn(int opcode) {
      this.touchLastJump();
      super.visitInsn(opcode);
   }

   public void visitIntInsn(int opcode, int operand) {
      this.touchLastJump();
      super.visitIntInsn(opcode, operand);
   }

   public void visitLdcInsn(Object cst) {
      this.touchLastJump();
      super.visitLdcInsn(cst);
   }

   public void visitMultiANewArrayInsn(String desc, int dims) {
      this.touchLastJump();
      super.visitMultiANewArrayInsn(desc, dims);
   }

   public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
      this.touchLastJump();
      super.visitTryCatchBlock(start, end, handler, type);
   }

   public void visitTypeInsn(int opcode, String desc) {
      this.touchLastJump();
      super.visitTypeInsn(opcode, desc);
   }

   public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      this.touchLastJump();
      super.visitMethodInsn(opcode, owner, name, desc, itf);
   }

   public void visitVarInsn(int opcode, int var) {
      this.touchLastJump();
      this.mv.visitVarInsn(opcode, this.adjustVariable(var));
   }

   public void visitIincInsn(int var, int increment) {
      this.touchLastJump();
      this.mv.visitIincInsn(this.adjustVariable(var), increment);
   }

   public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
      this.touchLastJump();
      this.mv.visitLocalVariable(name, desc, signature, start, end, this.adjustVariable(index));
   }

   private int adjustVariable(int var) {
      return var >= this.getLineVariableNumber() ? var + 4 : var;
   }

   public int getLineVariableNumber() {
      return this.myVariablesCount;
   }

   private int getJumpVariableNumber() {
      return this.myVariablesCount + 1;
   }

   private int getSwitchVariableNumber() {
      return this.myVariablesCount + 2;
   }

   public int getCurrentClassDataNumber() {
      return this.myVariablesCount + 3;
   }

   public void visitMaxs(int maxStack, int maxLocals) {
      if (this.myStartLabel != null && this.myEndLabel != null) {
         this.mv.visitLocalVariable("__line__number__", "I", (String)null, this.myStartLabel, this.myEndLabel, this.getLineVariableNumber());
         this.mv.visitLocalVariable("__jump__number__", "I", (String)null, this.myStartLabel, this.myEndLabel, this.getJumpVariableNumber());
         this.mv.visitLocalVariable("__switch__number__", "I", (String)null, this.myStartLabel, this.myEndLabel, this.getSwitchVariableNumber());
         this.mv.visitLocalVariable("__class__data__", "Ljava/lang/Object;", (String)null, this.myStartLabel, this.myEndLabel, this.getCurrentClassDataNumber());
      }

      super.visitMaxs(maxStack, maxLocals);
   }
}
