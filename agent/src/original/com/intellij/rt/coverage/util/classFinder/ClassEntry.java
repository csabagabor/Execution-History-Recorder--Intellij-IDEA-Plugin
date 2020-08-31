package original.com.intellij.rt.coverage.util.classFinder;

import java.io.InputStream;

public class ClassEntry {
   private final String myClassName;
   private final ClassLoader myClassLoader;

   public ClassEntry(String className, ClassLoader classLoader) {
      this.myClassName = className;
      this.myClassLoader = classLoader;
   }

   public String getClassName() {
      return this.myClassName;
   }

   public InputStream getClassInputStream() {
      String resourceName = this.myClassName.replace('.', '/') + ".class";
      InputStream is = this.getResourceStream(resourceName);
      return is != null ? is : this.getResourceStream("/" + resourceName);
   }

   private InputStream getResourceStream(String resourceName) {
      return this.myClassLoader == null ? this.getClass().getResourceAsStream(resourceName) : this.myClassLoader.getResourceAsStream(resourceName);
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ClassEntry that = (ClassEntry)o;
         if (this.myClassLoader != null) {
            if (this.myClassLoader.equals(that.myClassLoader)) {
               return this.myClassName.equals(that.myClassName);
            }
         } else if (that.myClassLoader == null) {
            return this.myClassName.equals(that.myClassName);
         }

         return false;
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.myClassName.hashCode();
      result = 31 * result + (this.myClassLoader != null ? this.myClassLoader.hashCode() : 0);
      return result;
   }
}
