package original.com.intellij.rt.coverage.util.classFinder;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLDecoder;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class ClassFinder {
   private final List<Pattern> myIncludePatterns;
   private final List<Pattern> myExcludePatterns;
   private final Set<ClassLoader> myClassloaders;

   public ClassFinder(List<Pattern> includePatterns, List<Pattern> excludePatterns) {
      this.myIncludePatterns = includePatterns;
      this.myExcludePatterns = excludePatterns;
      this.myClassloaders = new HashSet();
   }

   public void addClassLoader(ClassLoader cl) {
      if (cl != null) {
         if (cl.getClass().getName().equals("jetbrains.buildServer.agent.AgentClassLoader")) {
            return;
         }

         if (cl instanceof URLClassLoader) {
            this.myClassloaders.add(cl);
         }

         if (cl.getParent() != null) {
            this.addClassLoader(cl.getParent());
         }
      }

   }

   public Collection<ClassEntry> findMatchedClasses() {
      Set<ClassEntry> classes = new HashSet();
      Iterator var2 = this.getClassPathEntries().iterator();

      while(var2.hasNext()) {
         ClassPathEntry entry = (ClassPathEntry)var2.next();

         try {
            classes.addAll(entry.getClassesIterator(this.myIncludePatterns, this.myExcludePatterns));
         } catch (IOException var5) {
            var5.printStackTrace();
         }
      }

      return classes;
   }

   protected Collection<ClassPathEntry> getClassPathEntries() {
      Set<ClassPathEntry> result = new HashSet();
      result.addAll(extractEntries(System.getProperty("java.class.path")));
      result.addAll(extractEntries(System.getProperty("sun.boot.class.path")));
      this.collectClassloaderEntries(result);
      return result;
   }

   private void collectClassloaderEntries(Set<ClassPathEntry> result) {
      Iterator var2 = this.myClassloaders.iterator();

      while(var2.hasNext()) {
         Object myClassloader = var2.next();
         URLClassLoader cl = (URLClassLoader)myClassloader;

         try {
            URL[] urls = cl.getURLs();
            URL[] var6 = urls;
            int var7 = urls.length;

            for(int var8 = 0; var8 < var7; ++var8) {
               URL url = var6[var8];
               if ("file".equals(url.getProtocol())) {
                  String path = this.fixPath(url.getPath());
                  if (path != null) {
                     result.add(new ClassPathEntry(path, cl));
                  }
               }
            }
         } catch (Exception var11) {
            System.out.println("Exception occurred on trying collect ClassPath URLs. One of possible reasons is shutting down Tomcat before finishing tests. Coverage won't be affected but some of uncovered classes could be missing from the report.");
            var11.printStackTrace();
         }
      }

   }

   private String fixPath(String path) {
      String result = path;

      try {
         result = URLDecoder.decode(path, "UTF-8");
      } catch (UnsupportedEncodingException var4) {
         var4.printStackTrace();
      }

      if (result.length() == 0) {
         return result;
      } else {
         if (result.charAt(0) == '/' && result.length() > 3 && result.charAt(2) == ':') {
            result = result.substring(1);
         }

         return result;
      }
   }

   private static Collection<ClassPathEntry> extractEntries(String classPath) {
      if (classPath == null) {
         return Collections.emptyList();
      } else {
         String[] entries = classPath.split(System.getProperty("path.separator"));
         Set<ClassPathEntry> result = new HashSet();
         String[] var3 = entries;
         int var4 = entries.length;

         for(int var5 = 0; var5 < var4; ++var5) {
            String entry = var3[var5];
            result.add(new ClassPathEntry(entry, (ClassLoader)null));
         }

         return result;
      }
   }
}
