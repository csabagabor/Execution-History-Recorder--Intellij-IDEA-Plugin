

package modified.com.intellij.rt.coverage.instrumentation;

import modified.com.intellij.rt.coverage.data.ProjectData;
import org.jetbrains.coverage.org.objectweb.asm.*;
import original.com.intellij.rt.coverage.data.LineData;
import original.com.intellij.rt.coverage.util.LinesUtil;

public class SamplingInstrumenter extends Instrumenter {
    private static final String OBJECT_TYPE = "Ljava/lang/Object;";

    public SamplingInstrumenter(ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldCalculateSource) {
        super(projectData, classVisitor, className, shouldCalculateSource);
    }

    protected MethodVisitor createMethodLineEnumerator(MethodVisitor mv, final String name, final String desc, int access, String signature, String[] exceptions) {
        int variablesCount = (8 & access) != 0 ? 0 : 1;
        Type[] args = Type.getArgumentTypes(desc);
        Type[] var9 = args;
        int var10 = args.length;

        for (int var11 = 0; var11 < var10; ++var11) {
            Type arg = var9[var11];
            variablesCount += arg.getSize();
        }

        final int varCount = variablesCount;

        int thisMethodId = ProjectData.METHOD_ID++;
        return new MethodVisitor(458752, mv) {
            private Label myStartLabel;
            private Label myEndLabel;

            @Override
            public void visitLabel(Label label) {
                if (this.myStartLabel == null) {
                    this.myStartLabel = label;
                }

                this.myEndLabel = label;
                super.visitLabel(label);
            }

            @Override
            public void visitInsn(int opcode) {
                //whenever we find a RETURN, we instert the code, here only crazy example code
                if (ProjectData.MODE >= 1) {
                    if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
                        this.mv.visitVarInsn(Opcodes.ALOAD, this.getCurrentClassDataNumber());
                        if (thisMethodId <= Short.MAX_VALUE) {
                            this.mv.visitIntInsn(Opcodes.SIPUSH, thisMethodId);
                        } else {
                            this.mv.visitLdcInsn(thisMethodId);
                        }


                        this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "modified/com/intellij/rt/coverage/data/ClassData", "checkFlagAtEnd", "(I)V", false);
                    }
                }
                super.visitInsn(opcode);
            }

            @Override
            public void visitEnd() {
                super.visitEnd();
            }

            public void visitLineNumber(int line, Label start) {
                SamplingInstrumenter.this.getOrCreateLineData(line, name, desc);
                this.mv.visitVarInsn(Opcodes.ALOAD, this.getCurrentClassDataNumber());
                if (line <= Short.MAX_VALUE) {
                    this.mv.visitIntInsn(Opcodes.SIPUSH, line);
                } else {
                    this.mv.visitLdcInsn(line);
                }

                this.mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "modified/com/intellij/rt/coverage/data/ClassData", "touchLine", "(I)V", false);

                super.visitLineNumber(line, start);
            }

            public void visitCode() {
                this.mv.visitLdcInsn(SamplingInstrumenter.this.getClassName());
                this.mv.visitMethodInsn(Opcodes.INVOKESTATIC, "modified/com/intellij/rt/coverage/data/ProjectData", "loadClassData", "(Ljava/lang/String;)Lmodified/com/intellij/rt/coverage/data/ClassData;", false);
                this.mv.visitVarInsn(Opcodes.ASTORE, this.getCurrentClassDataNumber());

                if (ProjectData.MODE >= 1) {
                    this.mv.visitLdcInsn(new Integer(thisMethodId));
                    this.mv.visitFieldInsn(Opcodes.PUTSTATIC, "modified/com/intellij/rt/coverage/data/ProjectData", "FLAG", "I");
                }
                super.visitCode();
            }

            public void visitLocalVariable(String namex, String descx, String signature, Label start, Label end, int index) {
                super.visitLocalVariable(namex, descx, signature, start, end, this.adjustVariable(index));
            }

            public void visitIincInsn(int var, int increment) {
                super.visitIincInsn(this.adjustVariable(var), increment);
            }

            public void visitVarInsn(int opcode, int var) {
                super.visitVarInsn(opcode, this.adjustVariable(var));
            }

            private int adjustVariable(int var) {
                return var >= varCount ? var + 1 : var;
            }

            private int getCurrentClassDataNumber() {
                return varCount;
            }

            public void visitMaxs(int maxStack, int maxLocals) {
//                if (this.myStartLabel != null && this.myEndLabel != null) {
//                    this.mv.visitLocalVariable("__class__data__", "Ljava/lang/Object;", (String) null, this.myStartLabel, this.myEndLabel, this.getCurrentClassDataNumber());
//                }

                super.visitMaxs(maxStack, maxLocals);
            }
        };
    }

    protected void initLineData() {
        LineData[] lines = LinesUtil.calcLineArray(this.myMaxLineNumber, this.myLines);
        this.myClassData.initLineMask(lines);
        this.myClassData.setLines(lines);
    }
}
