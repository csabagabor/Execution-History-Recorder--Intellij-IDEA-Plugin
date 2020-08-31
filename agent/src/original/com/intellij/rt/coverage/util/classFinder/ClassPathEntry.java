package original.com.intellij.rt.coverage.util.classFinder;

import original.com.intellij.rt.coverage.util.ClassNameUtil;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class ClassPathEntry {
   private final ClassLoader myClassLoader;
   private final String myClassPathEntry;
   private static final ClassPathEntry.DirectoryEntryProcessor myDirectoryProcessor = new ClassPathEntry.DirectoryEntryProcessor();
   private static final ClassPathEntry.ZipEntryProcessor myZipProcessor = new ClassPathEntry.ZipEntryProcessor();
   private static final String CLASS_FILE_SUFFIX = ".class";

   public ClassPathEntry(String classPathEntry, ClassLoader classLoader) {
      this.myClassPathEntry = classPathEntry;
      this.myClassLoader = classLoader;
   }

   Collection<ClassEntry> getClassesIterator(List<Pattern> includePatterns, List<Pattern> excludePatterns) throws IOException {
      ClassPathEntry.ClassPathEntryProcessor processor = createEntryProcessor(this.myClassPathEntry);
      if (processor == null) {
         return Collections.emptyList();
      } else {
         processor.setFilter(includePatterns, excludePatterns);
         processor.setClassLoader(this.myClassLoader);
         return processor.findClasses(this.myClassPathEntry);
      }
   }

   private static ClassPathEntry.ClassPathEntryProcessor createEntryProcessor(String entry) {
      File file = new File(entry);
      if (file.isDirectory()) {
         return myDirectoryProcessor;
      } else {
         return !file.isFile() || !file.getName().endsWith(".jar") && !file.getName().endsWith(".zip") ? null : myZipProcessor;
      }
   }

   private static String removeClassSuffix(String name) {
      return name.substring(0, name.length() - ".class".length());
   }

   public boolean equals(Object o) {
      if (this == o) {
         return true;
      } else if (o != null && this.getClass() == o.getClass()) {
         ClassPathEntry that = (ClassPathEntry)o;
         if (this.myClassLoader != null) {
            if (this.myClassLoader.equals(that.myClassLoader)) {
               return this.myClassPathEntry.equals(that.myClassPathEntry);
            }
         } else if (that.myClassLoader == null) {
            return this.myClassPathEntry.equals(that.myClassPathEntry);
         }

         return false;
      } else {
         return false;
      }
   }

   public int hashCode() {
      int result = this.myClassLoader != null ? this.myClassLoader.hashCode() : 0;
      result = 31 * result + this.myClassPathEntry.hashCode();
      return result;
   }

   private static class ZipEntryProcessor extends ClassPathEntry.AbstractClassPathEntryProcessor {
      private ZipEntryProcessor() {
         super(null);
      }

      public Collection<String> extractClassNames(String classPathEntry) throws IOException {
         List<String> result = new ArrayList(100);
         ZipFile zipFile = new ZipFile(new File(classPathEntry));

         try {
            Enumeration zenum = zipFile.entries();

            while(zenum.hasMoreElements()) {
               ZipEntry ze = (ZipEntry)zenum.nextElement();
               if (!ze.isDirectory() && ze.getName().endsWith(".class")) {
                  result.add(ClassNameUtil.convertToFQName(ClassPathEntry.removeClassSuffix(ze.getName())));
               }
            }
         } finally {
            zipFile.close();
         }

         return result;
      }

      // $FF: synthetic method
      ZipEntryProcessor(Object x0) {
         this();
      }
   }

   private static class DirectoryEntryProcessor extends ClassPathEntry.AbstractClassPathEntryProcessor {
      private DirectoryEntryProcessor() {
         super(null);
      }

      protected Collection<String> extractClassNames(String classPathEntry) {
         File dir = new File(classPathEntry);
         List<String> result = new ArrayList(100);
         String curPath = "";
         collectClasses(curPath, dir, result);
         return result;
      }

      private static void collectClasses(String curPath, File parent, List<String> result) {
         File[] files = parent.listFiles();
         if (files != null) {
            String prefix = curPath.length() == 0 ? "" : curPath + ".";
            File[] var5 = files;
            int var6 = files.length;

            for(int var7 = 0; var7 < var6; ++var7) {
               File f = var5[var7];
               String name = f.getName();
               if (name.endsWith(".class")) {
                  result.add(prefix + ClassPathEntry.removeClassSuffix(name));
               } else if (f.isDirectory()) {
                  collectClasses(prefix + name, f, result);
               }
            }
         }

      }

      // $FF: synthetic method
      DirectoryEntryProcessor(Object x0) {
         this();
      }
   }

   private interface ClassPathEntryProcessor {
      void setFilter(List<Pattern> var1, List<Pattern> var2);

      void setClassLoader(ClassLoader var1);

      Collection<ClassEntry> findClasses(String var1) throws IOException;
   }

   private abstract static class AbstractClassPathEntryProcessor implements ClassPathEntry.ClassPathEntryProcessor {
      private List<Pattern> myIncludePatterns;
      private List<Pattern> myExcludePatterns;
      private ClassLoader myClassLoader;

      private AbstractClassPathEntryProcessor() {
      }

      public void setFilter(List<Pattern> includePatterns, List<Pattern> excludePatterns) {
         this.myIncludePatterns = includePatterns;
         this.myExcludePatterns = excludePatterns;
      }

      public void setClassLoader(ClassLoader classLoader) {
         this.myClassLoader = classLoader;
      }

      protected abstract Collection<String> extractClassNames(String var1) throws IOException;

      public Collection<ClassEntry> findClasses(String classPathEntry) throws IOException {
         Set<ClassEntry> includedClasses = new HashSet();
         Iterator var3 = this.extractClassNames(classPathEntry).iterator();

         while(var3.hasNext()) {
            Object o = var3.next();
            String className = (String)o;
            if (this.shouldInclude(className)) {
               includedClasses.add(new ClassEntry(className, this.myClassLoader));
            }
         }

         return includedClasses;
      }

      private boolean shouldInclude(String className) {
         if (ClassNameUtil.shouldExclude(className, this.myExcludePatterns)) {
            return false;
         } else {
            String outerClassName = ClassNameUtil.getOuterClassName(className);
            Iterator var3 = this.myIncludePatterns.iterator();

            Pattern e;
            do {
               if (!var3.hasNext()) {
                  return this.myIncludePatterns.isEmpty();
               }

               Object myIncludePattern = var3.next();
               e = (Pattern)myIncludePattern;
            } while(!e.matcher(outerClassName).matches());

            return true;
         }
      }

      // $FF: synthetic method
      AbstractClassPathEntryProcessor(Object x0) {
         this();
      }
   }
}
