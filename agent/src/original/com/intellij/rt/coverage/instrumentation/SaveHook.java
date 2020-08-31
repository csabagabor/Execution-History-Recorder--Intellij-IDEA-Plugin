package original.com.intellij.rt.coverage.instrumentation;

import modified.com.intellij.rt.coverage.data.ClassData;
import original.com.intellij.rt.coverage.data.LineData;
import modified.com.intellij.rt.coverage.data.ProjectData;
import original.com.intellij.rt.coverage.util.CoverageIOUtil;
import original.com.intellij.rt.coverage.util.DictionaryLookup;
import original.com.intellij.rt.coverage.util.ErrorReporter;
import original.com.intellij.rt.coverage.util.LinesUtil;
import original.com.intellij.rt.coverage.util.StringsPool;
import original.com.intellij.rt.coverage.util.classFinder.ClassEntry;
import original.com.intellij.rt.coverage.util.classFinder.ClassFinder;

import java.io.*;
import java.util.*;
import java.util.Map.Entry;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.gnu.trove.TIntObjectProcedure;
import org.jetbrains.coverage.gnu.trove.TObjectIntHashMap;
import org.jetbrains.coverage.org.objectweb.asm.ClassReader;

public class SaveHook implements Runnable {
   private final File myDataFile;
   private File mySourceMapFile;
   private final boolean myAppendUnloaded;
   private final ClassFinder myClassFinder;

   public SaveHook(File dataFile, boolean appendUnloaded, ClassFinder classFinder) {
      this.myDataFile = dataFile;
      this.myAppendUnloaded = appendUnloaded;
      this.myClassFinder = classFinder;
   }

   public void run() {
      this.save(ProjectData.getProjectData());
   }

   public void save(ProjectData projectData) {
      projectData.stop();

      try {
         if (this.myAppendUnloaded) {
            this.appendUnloaded(projectData);
         }

         DataOutputStream os = null;

         try {
            os = CoverageIOUtil.openFile(this.myDataFile);
            projectData.checkLineMappings();
            TObjectIntHashMap<String> dict = new TObjectIntHashMap();
            Map<String, ClassData> classes = new HashMap(projectData.getClasses());
            CoverageIOUtil.writeINT(os, classes.size());
            saveDictionary(os, dict, classes);
            saveData(os, dict, classes);
            saveSourceMap(classes, this.mySourceMapFile);
         } catch (IOException var15) {
            ErrorReporter.reportError("Error writing file " + this.myDataFile.getPath(), var15);
         } finally {
            try {
               if (os != null) {
                  os.close();
               }
            } catch (IOException var14) {
               ErrorReporter.reportError("Error writing file " + this.myDataFile.getPath(), var14);
            }

         }
      } catch (OutOfMemoryError var17) {
         ErrorReporter.reportError("Out of memory error occurred, try to increase memory available for the JVM, or make include / exclude patterns more specific", var17);
      } catch (Throwable var18) {
         ErrorReporter.reportError("Unexpected error", var18);
      }

   }

   public static void saveSourceMap(Map str_clData_classes, File sourceMapFile) {
      if (sourceMapFile != null) {
         Map readNames = Collections.emptyMap();

         try {
            if (sourceMapFile.exists()) {
               readNames = loadSourceMapFromFile(str_clData_classes, sourceMapFile);
            }
         } catch (IOException var5) {
            ErrorReporter.reportError("Error loading source map from " + sourceMapFile.getPath(), var5);
         }

         try {
            doSaveSourceMap(readNames, sourceMapFile, str_clData_classes);
         } catch (IOException var4) {
            ErrorReporter.reportError("Error writing source map " + sourceMapFile.getPath(), var4);
         }
      }

   }

   public static Map<Object, Object> loadSourceMapFromFile(Map classes, File mySourceMapFile) throws IOException {
      DataInputStream in = null;

      try {
         in = new DataInputStream(new FileInputStream(mySourceMapFile));
         int classNumber = CoverageIOUtil.readINT(in);
         HashMap<Object, Object> readNames = new HashMap(classNumber);

         for(int i = 0; i < classNumber; ++i) {
            String className = CoverageIOUtil.readUTFFast(in);
            String classSource = CoverageIOUtil.readUTFFast(in);
            if (!"".equals(classSource)) {
               ClassData data = (ClassData)classes.get(className);
               if (data == null) {
                  readNames.put(className, classSource);
               } else if (data.getSource() == null || !data.getSource().equals(classSource)) {
                  readNames.put(className, classSource);
               }
            }
         }

         HashMap var12 = readNames;
         return var12;
      } finally {
         if (in != null) {
            in.close();
         }

      }
   }

   private static void saveData(DataOutputStream os, final TObjectIntHashMap<String> dict, Map classes) throws IOException {
      Iterator var3 = classes.values().iterator();

      while(var3.hasNext()) {
         Object o = var3.next();
         ((ClassData)o).save(os, new DictionaryLookup() {
            public int getDictionaryIndex(String className) {
               return dict.containsKey(className) ? dict.get(className) : -1;
            }
         });
      }

   }

   private static void saveDictionary(DataOutputStream os, TObjectIntHashMap<String> dict, Map classes) throws IOException {
      int i = 0;
      Iterator var4 = classes.keySet().iterator();

      while(var4.hasNext()) {
         Object o = var4.next();
         String className = (String)o;
         dict.put(className, i++);
         CoverageIOUtil.writeUTF(os, className);
      }

   }

   public static void doSaveSourceMap(Map<Object, Object> str_str_readNames, File sourceMapFile, Map str_clData_classes) throws IOException {
      HashMap<Object, Object> str_str_merged_map = new HashMap(str_str_readNames);
      Iterator var4 = str_clData_classes.values().iterator();

      while(var4.hasNext()) {
         Object o1 = var4.next();
         ClassData classData = (ClassData)o1;
         if (!str_str_merged_map.containsKey(classData.getName())) {
            str_str_merged_map.put(classData.getName(), classData.getSource());
         }
      }

      DataOutputStream out = null;

      try {
         out = CoverageIOUtil.openFile(sourceMapFile);
         CoverageIOUtil.writeINT(out, str_str_merged_map.size());
         Iterator var13 = str_str_merged_map.entrySet().iterator();

         while(var13.hasNext()) {
            Object o = var13.next();
            Entry str_str_entry = (Entry)o;
            CoverageIOUtil.writeUTF(out, (String)str_str_entry.getKey());
            String value = (String)str_str_entry.getValue();
            CoverageIOUtil.writeUTF(out, value != null ? value : "");
         }
      } finally {
         if (out != null) {
            CoverageIOUtil.close(out);
         }

      }

   }

   private void appendUnloaded(ProjectData projectData) {
      Collection<ClassEntry> matchedClasses = this.myClassFinder.findMatchedClasses();
      Iterator var3 = matchedClasses.iterator();

      while(var3.hasNext()) {
         Object matchedClass = var3.next();
         ClassEntry classEntry = (ClassEntry)matchedClass;
         ClassData cd = projectData.getClassData(classEntry.getClassName());
         if (cd == null) {
            try {
               ClassReader reader = new ClassReader(classEntry.getClassInputStream());
               if (this.mySourceMapFile != null) {
                  cd = projectData.getOrCreateClassData(classEntry.getClassName());
               }

               SourceLineCounter slc = new SourceLineCounter(cd, !projectData.isSampling(), this.mySourceMapFile != null ? projectData : null);
               reader.accept(slc, 0);
               if (slc.getNSourceLines() > 0) {
                  final TIntObjectHashMap<LineData> lines = new TIntObjectHashMap(4, 0.99F);
                  final int[] maxLine = new int[]{1};
                  final ClassData classData = projectData.getOrCreateClassData(StringsPool.getFromPool(classEntry.getClassName()));
                  slc.getSourceLines().forEachEntry(new TIntObjectProcedure<String>() {
                     public boolean execute(int line, String methodSig) {
                        LineData ld = new LineData(line, StringsPool.getFromPool(methodSig));
                        lines.put(line, ld);
                        if (line > maxLine[0]) {
                           maxLine[0] = line;
                        }

                        classData.registerMethodSignature(ld);
                        ld.setStatus((byte)0);
                        return true;
                     }
                  });
                  classData.setLines(LinesUtil.calcLineArray(maxLine[0], lines));
               }
            } catch (Throwable var12) {
               var12.printStackTrace();
               ErrorReporter.reportError("Failed to process class: " + classEntry.getClassName() + ", error: " + var12.getMessage(), var12);
            }
         }
      }

   }

   public void setSourceMapFile(File sourceMapFile) {
      this.mySourceMapFile = sourceMapFile;
   }

   public void saveStackFrames(List<ProjectData.BreakPointEvent> breakpoints, File file) {
      if (file.exists()) {
         file.delete();
      }

      //System.out.println("SAVING" + file.getAbsolutePath());
      file.getParentFile().mkdirs();

      try {
         PrintWriter printWriter = new PrintWriter(file);

         for (ProjectData.BreakPointEvent breakpoint : breakpoints) {
            printWriter.println("##");
            for (int i = breakpoint.breakPointInfo.size() - 1; i >= 0; i--) {
               String breakPointInfo = breakpoint.breakPointInfo.get(i);
               printWriter.println(breakPointInfo);
            }

         }

         printWriter.close();
      } catch (Throwable e) {
         e.printStackTrace();
      }
   }
}
