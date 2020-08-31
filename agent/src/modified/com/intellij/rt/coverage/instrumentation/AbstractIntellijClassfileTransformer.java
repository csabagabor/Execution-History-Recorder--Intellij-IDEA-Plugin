
package modified.com.intellij.rt.coverage.instrumentation;

import modified.com.intellij.rt.coverage.data.ProjectData;
import original.com.intellij.rt.coverage.util.ClassNameUtil;
import original.com.intellij.rt.coverage.util.ErrorReporter;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import org.jetbrains.coverage.gnu.trove.THashMap;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.ClassWriter;

public abstract class AbstractIntellijClassfileTransformer implements ClassFileTransformer {
    private final boolean computeFrames = this.computeFrames();
    private final WeakHashMap<ClassLoader, Map<String, ClassReader>> classReaders = new WeakHashMap();
    private long ourTime;
    private int ourClassCount;
    public static final Set<String> TRANSFORMED_CLASSES = new HashSet<>();

    protected AbstractIntellijClassfileTransformer() {
    }

    public final byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classFileBuffer) {
        //long s = System.currentTimeMillis();

        //System.out.println("tansforming: " + className);
        byte[] var8 = null;
        try {
            var8 = this.transformInner(loader, className, classFileBuffer);
        } finally {
//            if (var8 != null) {
//                System.out.println("transformed2: " + Thread.currentThread() + ":" + className);
//            }else {
//                System.out.println("NOT transformed2: " + className);
//            }
//            if (var8 != null) {
//                System.out.println("transformed2: " + Thread.currentThread() + ":" + className);
//                ++this.ourClassCount;
//                if (ourClassCount % 50 == 0) {
//                    //System.out.println("thread="+Thread.currentThread());
//                    long allTime;
//                    synchronized (AbstractIntellijClassfileTransformer.class) {
//                        allTime = this.ourTime;
//                    }
//                    System.out.println("Class transformation2 time: " + allTime + "s for " + AbstractIntellijClassfileTransformer.this.ourClassCount + " classes or " + allTime / (double) AbstractIntellijClassfileTransformer.this.ourClassCount + "s per class");
//                }
//                synchronized (AbstractIntellijClassfileTransformer.class) {
//                    this.ourTime += System.currentTimeMillis() - s;
//                }
//            }
        }

        return var8;
    }

    private byte[] transformInner(ClassLoader loader, String className, byte[] classFileBuffer) {
            try {
//                if(true) {
//                    return null;
//                }


                if (className == null) {
                    return null;
                }

                if (className.endsWith(".class")) {
                    className = className.substring(0, className.length() - 6);
                }


                className = ClassNameUtil.convertToFQName(className);

                //**********Added code

                if (className.equals("modified.com.intellij.rt.coverage.data.Redirector")) {
                    return this.instrument(classFileBuffer, className, loader, this.computeFrames);
                }

                //**********Added code

                synchronized (TRANSFORMED_CLASSES) {
                    if (TRANSFORMED_CLASSES.contains(className)) {
                        return null;//do not instrument class twice
                    } else {
                        TRANSFORMED_CLASSES.add(className);
                    }
                }

                if (className.startsWith("modified.com.intellij.rt.") ||
                className.startsWith("original.com.intellij.rt.") ||
                        className.startsWith("java.") || className.startsWith("sun.") ||
                        className.startsWith("com.sun.") || className.startsWith("jdk.") ||
                        className.startsWith("org.jetbrains.coverage.gnu.trove.") ||
                        className.startsWith("org.jetbrains.coverage.org.objectweb.")) {
                    return null;
                }

                //if inner class, check if parent is instrumentable
                int indexOfDollarSign = className.indexOf("$");
                if (indexOfDollarSign >= 0) {
                    className = className.substring(0, indexOfDollarSign);
                }

                if (this.shouldExclude(className)) {
                    return null;
                }

                if (ProjectData.CLASSES_PATTERNS.contains(className)) {
                    return this.instrument(classFileBuffer, className, loader, this.computeFrames);
                }

                this.visitClassLoader(loader);
                AbstractIntellijClassfileTransformer.InclusionPattern inclusionPattern = this.getInclusionPattern();

                if (inclusionPattern == null){
                    return null;
                }

                if (inclusionPattern.accept(className)) {
                    return this.instrument(classFileBuffer, className, loader, this.computeFrames);
                }
            } catch (Throwable var5) {
                ErrorReporter.reportError("Error during class instrumentation: " + className, var5);
            }

            return null;
    }

    public byte[] instrument(byte[] classfileBuffer, String className, ClassLoader loader, boolean computeFrames) {
        ClassReader cr = new ClassReader(classfileBuffer);
        AbstractIntellijClassfileTransformer.MyClassWriter cw;
        if (computeFrames) {
            int version = getClassFileVersion(cr);
            int flags = (version & '\uffff') >= 50 && version != 196653 ? 2 : 1;
            cw = new AbstractIntellijClassfileTransformer.MyClassWriter(flags, loader);
        } else {
            cw = new AbstractIntellijClassfileTransformer.MyClassWriter(1, loader);
        }

        ClassVisitor cv = this.createClassVisitor(className, loader, cr, cw);
        cr.accept(cv, 0);
        return cw.toByteArray();
    }

    protected abstract ClassVisitor createClassVisitor(String var1, ClassLoader var2, ClassReader var3, ClassWriter var4);

    protected abstract boolean shouldExclude(String var1);

    protected AbstractIntellijClassfileTransformer.InclusionPattern getInclusionPattern() {
        return null;
    }

    protected void visitClassLoader(ClassLoader classLoader) {
    }

    protected boolean isStopped() {
        return false;
    }

    private boolean computeFrames() {
        return System.getProperty("idea.coverage.no.frames") == null;
    }

    private static int getClassFileVersion(ClassReader reader) {
        return reader.readInt(4);
    }

    private synchronized ClassReader getOrLoadClassReader(String className, ClassLoader classLoader) throws IOException {
        Map<String, ClassReader> loaderClassReaders = (Map)this.classReaders.get(classLoader);
        if (loaderClassReaders == null) {
            this.classReaders.put(classLoader, loaderClassReaders = new THashMap());
        }

        ClassReader classReader = (ClassReader)((Map)loaderClassReaders).get(className);
        if (classReader == null) {
            InputStream is = null;

            try {
                is = classLoader.getResourceAsStream(className + ".class");
                ((Map)loaderClassReaders).put(className, classReader = new ClassReader(is));
            } finally {
                if (is != null) {
                    is.close();
                }

            }
        }

        return classReader;
    }

    private class MyClassWriter extends ClassWriter {
        private static final String JAVA_LANG_OBJECT = "java/lang/Object";
        private final ClassLoader classLoader;

        MyClassWriter(int flags, ClassLoader classLoader) {
            super(flags);
            this.classLoader = classLoader;
        }

        protected String getCommonSuperClass(String type1, String type2) {
            try {
                ClassReader info1 = AbstractIntellijClassfileTransformer.this.getOrLoadClassReader(type1, this.classLoader);
                ClassReader info2 = AbstractIntellijClassfileTransformer.this.getOrLoadClassReader(type2, this.classLoader);
                String superType = this.checkImplementInterface(type1, type2, info1, info2);
                if (superType != null) {
                    return superType;
                } else {
                    superType = this.checkImplementInterface(type2, type1, info2, info1);
                    if (superType != null) {
                        return superType;
                    } else {
                        StringBuilder b1 = this.typeAncestors(type1, info1);
                        StringBuilder b2 = this.typeAncestors(type2, info2);
                        String result = "java/lang/Object";
                        int end1 = b1.length();
                        int end2 = b2.length();

                        while(true) {
                            int start1 = b1.lastIndexOf(";", end1 - 1);
                            int start2 = b2.lastIndexOf(";", end2 - 1);
                            if (start1 == -1 || start2 == -1 || end1 - start1 != end2 - start2) {
                                return result;
                            }

                            String p1 = b1.substring(start1 + 1, end1);
                            String p2 = b2.substring(start2 + 1, end2);
                            if (!p1.equals(p2)) {
                                return result;
                            }

                            result = p1;
                            end1 = start1;
                            end2 = start2;
                        }
                    }
                }
            } catch (IOException var15) {
                throw new RuntimeException(var15.toString());
            }
        }

        private String checkImplementInterface(String type1, String type2, ClassReader info1, ClassReader info2) throws IOException {
            if ((info1.getAccess() & 512) != 0) {
                return this.typeImplements(type2, info2, type1) ? type1 : "java/lang/Object";
            } else {
                return null;
            }
        }

        private StringBuilder typeAncestors(String type, ClassReader info) throws IOException {
            StringBuilder b;
            for(b = new StringBuilder(); !"java/lang/Object".equals(type); info = AbstractIntellijClassfileTransformer.this.getOrLoadClassReader(type, this.classLoader)) {
                b.append(';').append(type);
                type = info.getSuperName();
            }

            return b;
        }

        private boolean typeImplements(String type, ClassReader classReader, String interfaceName) throws IOException {
            while(!"java/lang/Object".equals(type)) {
                String[] interfaces = classReader.getInterfaces();
                String[] var5 = interfaces;
                int var6 = interfaces.length;

                int var7;
                String itf;
                for(var7 = 0; var7 < var6; ++var7) {
                    itf = var5[var7];
                    if (itf.equals(interfaceName)) {
                        return true;
                    }
                }

                var5 = interfaces;
                var6 = interfaces.length;

                for(var7 = 0; var7 < var6; ++var7) {
                    itf = var5[var7];
                    if (this.typeImplements(itf, AbstractIntellijClassfileTransformer.this.getOrLoadClassReader(itf, this.classLoader), interfaceName)) {
                        return true;
                    }
                }

                type = classReader.getSuperName();
                classReader = AbstractIntellijClassfileTransformer.this.getOrLoadClassReader(type, this.classLoader);
            }

            return false;
        }
    }

    public interface InclusionPattern {
        boolean accept(String var1);
    }
}
