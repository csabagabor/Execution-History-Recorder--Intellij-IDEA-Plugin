
package modified.com.intellij.rt.coverage.instrumentation;

import original.com.intellij.rt.coverage.data.LineData;
import original.com.intellij.rt.coverage.util.LinesUtil;
import modified.com.intellij.rt.coverage.data.ProjectData;
import org.jetbrains.coverage.org.objectweb.asm.*;

import static org.jetbrains.coverage.org.objectweb.asm.Opcodes.*;

public class ProjectDataInstrumenter extends Instrumenter {
    private static final String OBJECT_TYPE = "Ljava/lang/Object;";

    public ProjectDataInstrumenter(modified.com.intellij.rt.coverage.data.ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldCalculateSource) {
        super(projectData, classVisitor, className, shouldCalculateSource);
    }

    protected MethodVisitor createMethodLineEnumerator(MethodVisitor mv, final String name, final String desc, int access, String signature, String[] exceptions) {
        if ("touchLine".equals(name) || "checkFlagAtEnd".equals(name)) {
            //returns 4
//             int arg = Type.getArgumentsAndReturnSizes(desc) >> 2 - 1;
//             System.out.println("arg=" + arg);

            if (ProjectData.DISABLED) {
                return new EmptyMethodVisitor(mv);
            } else {
                return new OriginalMethodVisitor(name, mv);
            }
        } else {
            return new MethodVisitor(Opcodes.API_VERSION, mv) {
                @Override
                public void visitCode() {
                    super.visitCode();
                }
            };
        }
    }

    protected void initLineData() {
        LineData[] lines = LinesUtil.calcLineArray(this.myMaxLineNumber, this.myLines);
        this.myClassData.initLineMask(lines);
        this.myClassData.setLines(lines);
    }

    public static class EmptyMethodVisitor extends MethodVisitor {
        private final MethodVisitor targetWriter;

        public EmptyMethodVisitor(MethodVisitor targetWriter) {
            super(ASM4, null);
            this.targetWriter = targetWriter;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            targetWriter.visitMaxs(0, 4);
        }

        @Override
        public void visitCode() {
            targetWriter.visitCode();
            targetWriter.visitInsn(Opcodes.RETURN);// our new code
        }

        @Override
        public void visitEnd() {
            targetWriter.visitEnd();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return targetWriter.visitAnnotation(desc, visible);
        }

        @Override
        public void visitParameter(String name, int access) {
            targetWriter.visitParameter(name, access);
        }
    }

    public static class OriginalMethodVisitor extends MethodVisitor {
        private String name;
        private final MethodVisitor targetWriter;

        public OriginalMethodVisitor(String name, MethodVisitor targetWriter) {
            super(ASM4, null);
            this.name = name;
            this.targetWriter = targetWriter;
        }

        @Override
        public void visitMaxs(int maxStack, int maxLocals) {
            targetWriter.visitMaxs(0, 4);
        }

        @Override
        public void visitEnd() {
            targetWriter.visitEnd();
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            return targetWriter.visitAnnotation(desc, visible);
        }

        @Override
        public void visitParameter(String name, int access) {
            targetWriter.visitParameter(name, access);
        }

        @Override
        public void visitCode() {
            /*
            bytecode for:
             public void touchLine(int line) {
                this.myLineMask[line]++;
            }
             */
            if ("checkFlagAtEnd".equals(name)) {
                targetWriter.visitCode();
                Label label0 = new Label();
                targetWriter.visitLabel(label0);
                targetWriter.visitLineNumber(6, label0);
                targetWriter.visitVarInsn(ALOAD, 0);
                targetWriter.visitVarInsn(ILOAD, 1);
                targetWriter.visitMethodInsn(INVOKEVIRTUAL, "modified/com/intellij/rt/coverage/data/ClassData", "checkFlagAtEnd2", "(I)V", false);
                Label label1 = new Label();
                targetWriter.visitLabel(label1);
                targetWriter.visitLineNumber(7, label1);
                targetWriter.visitInsn(RETURN);
                Label label2 = new Label();
                targetWriter.visitLabel(label2);
                targetWriter.visitLocalVariable("classData", "Lmodified/com/intellij/rt/coverage/data/ClassData;", null, label0, label2, 0);
                targetWriter.visitLocalVariable("currentFlag", "I", null, label0, label2, 1);
                targetWriter.visitMaxs(2, 2);
                targetWriter.visitEnd();
            } else {
                targetWriter.visitCode();
                Label label0 = new Label();
                targetWriter.visitLabel(label0);
                targetWriter.visitLineNumber(143, label0);
                targetWriter.visitVarInsn(Opcodes.ALOAD, 0);
                targetWriter.visitFieldInsn(Opcodes.GETFIELD, "modified/com/intellij/rt/coverage/data/ClassData", "myLineMask", "[I");
                targetWriter.visitVarInsn(Opcodes.ILOAD, 1);
                targetWriter.visitInsn(Opcodes.DUP2);
                targetWriter.visitInsn(Opcodes.IALOAD);
                targetWriter.visitInsn(Opcodes.ICONST_1);
                targetWriter.visitInsn(Opcodes.IADD);
                targetWriter.visitInsn(Opcodes.IASTORE);
                Label label1 = new Label();
                targetWriter.visitLabel(label1);
                targetWriter.visitLineNumber(144, label1);
                targetWriter.visitInsn(Opcodes.RETURN);
                Label label2 = new Label();
                targetWriter.visitLabel(label2);
                targetWriter.visitLocalVariable("this", "Lmodified/com/intellij/rt/coverage/data/ClassData;", null, label0, label2, 0);
                targetWriter.visitLocalVariable("line", "I", null, label0, label2, 1);
                targetWriter.visitMaxs(4, 2);
                targetWriter.visitEnd();
            }
        }
    }
}
