package modified.com.intellij.rt.coverage.instrumentation;

import modified.com.intellij.rt.coverage.data.ClassData;
import org.jetbrains.coverage.org.objectweb.asm.FieldVisitor;
import original.com.intellij.rt.coverage.data.LineData;
import modified.com.intellij.rt.coverage.data.ProjectData;
import original.com.intellij.rt.coverage.instrumentation.JSR45Util;
import original.com.intellij.rt.coverage.util.StringsPool;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.org.objectweb.asm.ClassVisitor;
import org.jetbrains.coverage.org.objectweb.asm.MethodVisitor;

import java.util.HashMap;
import java.util.Map;

public abstract class Instrumenter extends ClassVisitor {
   protected final ProjectData myProjectData;
   protected final ClassVisitor myClassVisitor;
   private final String myClassName;
   private final boolean myShouldCalculateSource;
   protected TIntObjectHashMap<LineData> myLines = new TIntObjectHashMap(4, 0.99F);
   protected int myMaxLineNumber;
   protected ClassData myClassData;
   protected boolean myProcess;
   private boolean myEnum;
   private Map<String, String> fields = new HashMap<>();

   public Instrumenter(ProjectData projectData, ClassVisitor classVisitor, String className, boolean shouldCalculateSource) {
      super(458752, classVisitor);
      this.myProjectData = projectData;
      this.myClassVisitor = classVisitor;
      this.myClassName = className;
      this.myShouldCalculateSource = shouldCalculateSource;
   }

   public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
      this.myEnum = (access & 16384) != 0;
      this.myProcess = (access & 512) == 0;
      this.myClassData = this.myProjectData.getOrCreateClassData(StringsPool.getFromPool(this.myClassName));
      super.visit(version, access, name, signature, superName, interfaces);
   }

   @Override
   public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
      fields.put(name, descriptor);
      return super.visitField(access, name, descriptor, signature, value);
   }

   @Override
   public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      MethodVisitor mv = this.cv.visitMethod(access, name, desc, signature, exceptions);
      if (mv == null) {
         return null;
      } else if ((access & 64) != 0) {
         return mv;
      } else if ((access & 1024) != 0) {
         return mv;
      } else if (this.myEnum && isDefaultEnumMethod(name, desc, signature, this.myClassName)) {
         return mv;
      } else {
         this.myProcess = true;
         try {
            if (name == null || ((ProjectData.INCLUDE_CONSTRUCTORS || (!"<init>".equals(name) && !"<clinit>".equals(name))) &&
                    (ProjectData.INCLUDE_GETTERS_SETTERS || (!isSetterGetter(name, desc) && !isCommonMethod(name, desc))))) {
               return this.createMethodLineEnumerator(mv, name, desc, access, signature, exceptions);
            }else{
               return mv;
            }
         } catch (Exception e) {
            return this.createMethodLineEnumerator(mv, name, desc, access, signature, exceptions);
         }
      }
   }

   private boolean isCommonMethod(String name, String desc) {
      return "toString".equals(name) || "equals".equals(name) || ("hashCode".equals(name) && "()I".equals(desc));//don't check for params, it is slower that way
   }

   /*
   also works for superclass fields
    */
   private boolean isSetterGetter(String name, String desc) {
      try {
         try {
            boolean getter = name.startsWith("get");
            boolean setter = name.startsWith("set");
            boolean isBooleanGetter = name.startsWith("is");

            boolean isParamNrCorrect = true;//check nr of params also

            if (getter || setter || isBooleanGetter) {
               if (isBooleanGetter) {
                  name = name.substring(2);//name is not null, checked before
               } else {
                  name = name.substring(3);//name is not null, checked before
               }
               name = Character.toLowerCase(name.charAt(0)) + name.substring(1);
               String signature = fields.get(name);
               if (signature != null) {
                  int ind1 = desc.indexOf("(");
                  int ind2 = desc.indexOf(")");
                  if (ind1 >= 0 && ind2 >= 0) {
                     String realSign = "";
                     if (getter || isBooleanGetter) {
                        realSign = desc.substring(ind2 + 1);
                        isParamNrCorrect = ind2 - ind1 == 1;//no param "()"
                     } else {//already checked that it is a setter
                        realSign = desc.substring(ind1 + 1, ind2);
                        isParamNrCorrect = desc.substring(ind2).contains("V");//void return type
                     }
                     if (realSign.equals(signature) && isParamNrCorrect) {
                        return true;
                     }
                  }
               }
            }
         } catch (Exception e) {

         }
         return false;
      } catch (Exception e) {
         return false;
      }
   }

   private static boolean isDefaultEnumMethod(String name, String desc, String signature, String className) {
      return name.equals("values") && desc.equals("()[L" + className + ";") || name.equals("valueOf") && desc.equals("(Ljava/lang/String;)L" + className + ";") || name.equals("<init>") && signature != null && signature.equals("()V");
   }

   protected abstract MethodVisitor createMethodLineEnumerator(MethodVisitor var1, String var2, String var3, int var4, String var5, String[] var6);

   public void visitEnd() {
      if (this.myProcess) {
         this.initLineData();
         this.myLines = null;
      }

      super.visitEnd();
   }

   protected abstract void initLineData();

   public void getOrCreateLineData(int line, String name, String desc) {
      if (this.myLines == null) {
         this.myLines = new TIntObjectHashMap();
      }

      LineData lineData = (LineData)this.myLines.get(line);
      if (lineData == null) {
         lineData = new LineData(line, StringsPool.getFromPool(name + desc));
         this.myLines.put(line, lineData);
      }

      if (line > this.myMaxLineNumber) {
         this.myMaxLineNumber = line;
      }

   }

   public void removeLine(int line) {
      this.myLines.remove(line);
   }

   public void visitSource(String source, String debug) {
      super.visitSource(source, debug);
      if (this.myShouldCalculateSource) {
         this.myProjectData.getOrCreateClassData(this.myClassName).setSource(source);
      }

      if (debug != null) {
         this.myProjectData.addLineMaps(this.myClassName, JSR45Util.extractLineMapping(debug, this.myClassName));
      }

   }

   public String getClassName() {
      return this.myClassName;
   }

   public void visitOuterClass(String outerClassName, String methodName, String methodSig) {
      if (this.myShouldCalculateSource) {
         this.myProjectData.getOrCreateClassData(outerClassName).setSource(this.myClassData.getSource());
      }

      super.visitOuterClass(outerClassName, methodName, methodSig);
   }
}
