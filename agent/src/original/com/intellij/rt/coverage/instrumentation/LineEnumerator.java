package original.com.intellij.rt.coverage.instrumentation;

import original.com.intellij.rt.coverage.data.LineData;
import original.com.intellij.rt.coverage.util.ClassNameUtil;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.jetbrains.coverage.org.objectweb.asm.Label;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;
import org.jetbrains.coverage.org.objectweb.asm.Opcodes;
import org.jetbrains.coverage.org.objectweb.asm.tree.MethodNode;

public class LineEnumerator extends MethodVisitor implements Opcodes {
   private final ClassInstrumenter myClassInstrumenter;
   private final int myAccess;
   private final String myMethodName;
   private final String mySignature;
   private final MethodNode methodNode;
   private int myCurrentLine;
   private int myCurrentJump;
   private int myCurrentSwitch;
   private Label myLastJump;
   private boolean myHasExecutableLines = false;
   private Set<Label> myJumps;
   private Map<Label, Integer> mySwitches;
   private final MethodVisitor myWriterMethodVisitor;
   private static final byte SEEN_NOTHING = 0;
   private static final byte DUP_SEEN = 1;
   private static final byte IFNONNULL_SEEN = 2;
   private static final byte PARAM_CONST_SEEN = 3;
   private static final byte ASSERTIONS_DISABLED_STATE = 5;
   private byte myState = 0;
   private boolean myHasInstructions;

   public LineEnumerator(ClassInstrumenter classInstrumenter, MethodVisitor mv, int access, String name, String desc, String signature, String[] exceptions) {
      super(458752, new MethodNode(access, name, desc, signature, exceptions));
      this.myClassInstrumenter = classInstrumenter;
      this.myWriterMethodVisitor = mv;
      this.myAccess = access;
      this.myMethodName = name;
      this.mySignature = desc;
      this.methodNode = (MethodNode)this.mv;
   }

   public void visitEnd() {
      super.visitEnd();
      this.methodNode.accept((MethodVisitor)(!this.myHasExecutableLines ? this.myWriterMethodVisitor : new TouchCounter(this, this.myAccess, this.mySignature)));
   }

   public void visitLineNumber(int line, Label start) {
      super.visitLineNumber(line, start);
      this.myHasInstructions = false;
      this.myCurrentLine = line;
      this.myCurrentJump = 0;
      this.myCurrentSwitch = 0;
      this.myHasExecutableLines = true;
      this.myClassInstrumenter.getOrCreateLineData(this.myCurrentLine, this.myMethodName, this.mySignature);
   }

   public String getClassName() {
      return this.myClassInstrumenter.getClassName();
   }

   public MethodVisitor getWV() {
      return this.myWriterMethodVisitor;
   }

   public void visitJumpInsn(int opcode, Label label) {
      if (!this.myHasExecutableLines) {
         super.visitJumpInsn(opcode, label);
      } else {
         LineData lineData;
         if (opcode != 167 && opcode != 168 && !this.myMethodName.equals("<clinit>")) {
            if (this.myJumps == null) {
               this.myJumps = new HashSet();
            }

            this.myJumps.add(label);
            this.myLastJump = label;
            lineData = this.myClassInstrumenter.getLineData(this.myCurrentLine);
            if (lineData != null) {
               lineData.addJump(this.myCurrentJump++);
            }
         }

         if (this.myState == 5 && opcode == 154) {
            this.myState = 0;
            lineData = this.myClassInstrumenter.getLineData(this.myCurrentLine);
            if (lineData != null && this.isJump(label)) {
               lineData.removeJump(this.myCurrentJump--);
               this.myJumps.remove(this.myLastJump);
               this.myLastJump = null;
            }
         }

         if (this.myState == 1 && opcode == 199) {
            this.myState = 2;
         } else {
            this.myState = 0;
         }

         this.myHasInstructions = true;
         super.visitJumpInsn(opcode, label);
      }
   }

   public boolean isJump(Label jump) {
      return this.myJumps != null && this.myJumps.contains(jump);
   }

   public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      super.visitLookupSwitchInsn(dflt, keys, labels);
      if (this.myHasExecutableLines) {
         this.rememberSwitchLabels(dflt, labels);
         LineData lineData = this.myClassInstrumenter.getLineData(this.myCurrentLine);
         if (lineData != null) {
            lineData.addSwitch(this.myCurrentSwitch++, keys);
         }

         this.myState = 0;
         this.myHasInstructions = true;
      }
   }

   public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
      super.visitTableSwitchInsn(min, max, dflt, labels);
      if (this.myHasExecutableLines) {
         this.rememberSwitchLabels(dflt, labels);
         LineData lineData = this.myClassInstrumenter.getLineData(this.myCurrentLine);
         if (lineData != null) {
            lineData.addSwitch(this.myCurrentSwitch++, min, max);
         }

         this.myState = 0;
         this.myHasInstructions = true;
      }
   }

   private void rememberSwitchLabels(Label dflt, Label[] labels) {
      if (this.mySwitches == null) {
         this.mySwitches = new HashMap();
      }

      this.mySwitches.put(dflt, -1);

      for(int i = labels.length - 1; i >= 0; --i) {
         this.mySwitches.put(labels[i], i);
      }

   }

   public Integer getSwitchKey(Label label) {
      return this.mySwitches == null ? null : (Integer)this.mySwitches.get(label);
   }

   public String getMethodName() {
      return this.myMethodName;
   }

   public void visitInsn(int opcode) {
      super.visitInsn(opcode);
      if (this.myHasExecutableLines) {
         if (opcode == 177 && !this.myHasInstructions) {
            this.myClassInstrumenter.removeLine(this.myCurrentLine);
         } else {
            this.myHasInstructions = true;
         }

         if (opcode == 89) {
            this.myState = 1;
         } else if (this.myState != 2 || (opcode < 3 || opcode > 8) && opcode != 16 && opcode != 17) {
            this.myState = 0;
         } else {
            this.myState = 3;
         }

      }
   }

   public void visitIntInsn(int opcode, int operand) {
      super.visitIntInsn(opcode, operand);
      if (this.myHasExecutableLines) {
         this.myState = 0;
         this.myHasInstructions = true;
      }
   }

   public void visitVarInsn(int opcode, int var) {
      super.visitVarInsn(opcode, var);
      if (this.myHasExecutableLines) {
         this.myState = 0;
         this.myHasInstructions = true;
      }
   }

   public void visitTypeInsn(int opcode, String type) {
      super.visitTypeInsn(opcode, type);
      if (this.myHasExecutableLines) {
         this.myState = 0;
         this.myHasInstructions = true;
      }
   }

   public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (this.myHasExecutableLines) {
         if (opcode == 178 && name.equals("$assertionsDisabled")) {
            this.myState = 5;
         } else {
            this.myState = 0;
         }

         this.myHasInstructions = true;
      }
   }

   public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      super.visitMethodInsn(opcode, owner, name, desc, itf);
      if (this.myHasExecutableLines) {
         if (this.myState == 3 && opcode == 184 && name.startsWith("$$$reportNull$$$") && ClassNameUtil.convertToFQName(owner).equals(this.myClassInstrumenter.getClassName())) {
            LineData lineData = this.myClassInstrumenter.getLineData(this.myCurrentLine);
            if (lineData != null) {
               lineData.removeJump(this.myCurrentJump--);
               this.myJumps.remove(this.myLastJump);
            }

            this.myState = 0;
         } else {
            this.myState = 0;
         }

         this.myHasInstructions = true;
      }
   }

   public void visitLdcInsn(Object cst) {
      super.visitLdcInsn(cst);
      if (this.myHasExecutableLines) {
         this.myState = 0;
         this.myHasInstructions = true;
      }
   }

   public void visitIincInsn(int var, int increment) {
      super.visitIincInsn(var, increment);
      if (this.myHasExecutableLines) {
         this.myState = 0;
         this.myHasInstructions = true;
      }
   }

   public void visitMultiANewArrayInsn(String desc, int dims) {
      super.visitMultiANewArrayInsn(desc, dims);
      if (this.myHasExecutableLines) {
         this.myState = 0;
         this.myHasInstructions = true;
      }
   }
}
