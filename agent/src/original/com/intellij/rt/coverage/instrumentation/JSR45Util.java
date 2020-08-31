package original.com.intellij.rt.coverage.instrumentation;

import original.com.intellij.rt.coverage.data.FileMapData;
import original.com.intellij.rt.coverage.data.LineMapData;
import original.com.intellij.rt.coverage.util.ClassNameUtil;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.jetbrains.coverage.gnu.trove.THashSet;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;
import org.jetbrains.coverage.gnu.trove.TObjectFunction;

public class JSR45Util {
   private static final String FILE_SECTION = "*F\n";
   private static final String LINE_SECTION = "*L\n";
   private static final String END_SECTION = "*E";

   private static String checkSMAP(String debug) {
      return debug.startsWith("SMAP") ? debug.substring(4) : null;
   }

   public static FileMapData[] extractLineMapping(String debug, String className) {
      debug = checkSMAP(debug);
      if (debug == null) {
         return null;
      } else {
         TIntObjectHashMap<THashSet<LineMapData>> linesMap = new TIntObjectHashMap();
         int fileSectionIdx = debug.indexOf("*F\n");
         int lineInfoIdx = debug.indexOf("*L\n");
         TIntObjectHashMap<String> fileNames = parseFileNames(debug, fileSectionIdx, lineInfoIdx, className);
         String lineInfo = debug.substring(lineInfoIdx + "*L\n".length(), debug.indexOf("*E"));
         String[] lines = lineInfo.split("\n");
         int fileId = 1;
         String[] var9 = lines;
         int var10 = lines.length;

         int startSrcLine;
         int repeat;
         for(int var11 = 0; var11 < var10; ++var11) {
            String line = var9[var11];
            repeat = 1;
            int outLineInc = 1;
            int idx = line.indexOf(":");
            String srcLine = line.substring(0, idx);
            String outLine = line.substring(idx + 1);
            int srcCommaIdx = srcLine.indexOf(44);
            int sharpIdx = srcLine.indexOf("#");
            if (sharpIdx > -1) {
               startSrcLine = Integer.parseInt(srcLine.substring(0, sharpIdx));
               if (srcCommaIdx > -1) {
                  repeat = Integer.parseInt(srcLine.substring(srcCommaIdx + 1));
                  fileId = Integer.parseInt(srcLine.substring(sharpIdx + 1, srcCommaIdx));
               } else {
                  fileId = Integer.parseInt(srcLine.substring(sharpIdx + 1));
               }
            } else if (srcCommaIdx > -1) {
               repeat = Integer.parseInt(srcLine.substring(srcCommaIdx + 1));
               startSrcLine = Integer.parseInt(srcLine.substring(0, srcCommaIdx));
            } else {
               startSrcLine = Integer.parseInt(srcLine);
            }

            int outCommaIdx = outLine.indexOf(44);
            int startOutLine;
            if (outCommaIdx > -1) {
               outLineInc = Integer.parseInt(outLine.substring(outCommaIdx + 1));
               startOutLine = Integer.parseInt(outLine.substring(0, outCommaIdx));
            } else {
               startOutLine = Integer.parseInt(outLine);
            }

            THashSet<LineMapData> currentFile = (THashSet)linesMap.get(fileId);
            if (currentFile == null) {
               currentFile = new THashSet();
               linesMap.put(fileId, currentFile);
            }

            for(int r = 0; r < repeat; ++r) {
               currentFile.add(new LineMapData(startSrcLine + r, startOutLine + r * outLineInc, startOutLine + (r + 1) * outLineInc - 1));
            }
         }

         List<FileMapData> result = new ArrayList();
         int[] keys = linesMap.keys();
         Arrays.sort(keys);
         int[] var27 = keys;
         int var28 = keys.length;

         for(startSrcLine = 0; startSrcLine < var28; ++startSrcLine) {
            repeat = var27[startSrcLine];
            result.add(new FileMapData((String)fileNames.get(repeat), getLinesMapping((THashSet)linesMap.get(repeat))));
         }

         return (FileMapData[])result.toArray(FileMapData.EMPTY_FILE_MAP);
      }
   }

   private static String[] getFileSectionLines(String debug, int fileSectionIdx, int lineInfoIdx) {
      String fileSection = debug.substring(fileSectionIdx + "*F\n".length(), lineInfoIdx);
      fileSection = fileSection.trim();
      if (fileSection.endsWith("\n")) {
         fileSection = fileSection.substring(0, fileSection.length() - 1);
      }

      return fileSection.split("\n");
   }

   private static TIntObjectHashMap<String> parseFileNames(String debug, int fileSectionIdx, int lineInfoIdx, String className) {
      final String defaultPrefix = getClassPackageName(className);
      String[] fileNameIdx = getFileSectionLines(debug, fileSectionIdx, lineInfoIdx);
      TIntObjectHashMap<String> result = new TIntObjectHashMap();
      boolean generatedPrefix = true;

      for(int i = 0; i < fileNameIdx.length; ++i) {
         String fileName = fileNameIdx[i];
         String idAndName = fileName;
         String path = null;
         if (fileName.startsWith("+ ")) {
            idAndName = fileName.substring(2);
            ++i;
            path = fileNameIdx[i];
         }

         int idx = idAndName.indexOf(" ");
         int key = Integer.parseInt(idAndName.substring(0, idx));
         String currentClassName = idAndName.substring(idx + 1);
         path = path == null ? currentClassName : processRelative(path);
         int lastDot = path.lastIndexOf(".");
         String fileNameWithDots;
         if (lastDot < 0) {
            fileNameWithDots = path;
         } else {
            fileNameWithDots = path.substring(0, lastDot) + "_" + path.substring(lastDot + 1);
         }

         fileNameWithDots = ClassNameUtil.convertToFQName(fileNameWithDots);
         generatedPrefix &= !fileNameWithDots.startsWith(defaultPrefix);
         result.put(key, fileNameWithDots);
      }

      if (generatedPrefix) {
         result.transformValues(new TObjectFunction<String, String>() {
            public String execute(String selfValue) {
               return defaultPrefix + selfValue;
            }
         });
      }

      return result;
   }

   public static String processRelative(String fileName) {
      int idx;
      while((idx = fileName.indexOf("..")) > -1) {
         String rest = fileName.substring(idx + "..".length());
         String start = fileName.substring(0, idx);
         if (!start.endsWith("/")) {
            return fileName;
         }

         start = start.substring(0, start.length() - 1);
         int endIndex = start.lastIndexOf(47);
         if (endIndex > -1) {
            fileName = start.substring(0, endIndex) + rest;
         } else {
            fileName = rest.startsWith("/") ? rest.substring(1) : rest;
         }
      }

      return fileName;
   }

   public static String getClassPackageName(String className) {
      String generatePrefix = "";
      int fqnLastDotIdx = className.lastIndexOf(".");
      if (fqnLastDotIdx > -1) {
         generatePrefix = className.substring(0, fqnLastDotIdx + 1);
      }

      return generatePrefix;
   }

   private static LineMapData[] getLinesMapping(THashSet<LineMapData> linesMap) {
      int max = 0;
      Iterator var2 = linesMap.iterator();

      while(var2.hasNext()) {
         Object aLinesMap1 = var2.next();
         LineMapData lmd = (LineMapData)aLinesMap1;
         if (max < lmd.getSourceLineNumber()) {
            max = lmd.getSourceLineNumber();
         }
      }

      LineMapData[] result = new LineMapData[max + 1];

      LineMapData lmd;
      for(Iterator var7 = linesMap.iterator(); var7.hasNext(); result[lmd.getSourceLineNumber()] = lmd) {
         Object aLinesMap = var7.next();
         lmd = (LineMapData)aLinesMap;
      }

      return result;
   }

   public static List<String> parseSourcePaths(String debug) {
      debug = checkSMAP(debug);
      if (debug != null) {
         String[] fileNameIdx = getFileSectionLines(debug, debug.indexOf("*F\n"), debug.indexOf("*L\n"));
         List<String> paths = new ArrayList();

         for(int i = 0; i < fileNameIdx.length; ++i) {
            String fileName = fileNameIdx[i];
            String idAndName = fileName;
            String path = null;
            if (fileName.startsWith("+ ")) {
               idAndName = fileName.substring(2);
               ++i;
               path = fileNameIdx[i];
            }

            int idx = idAndName.indexOf(" ");
            String currentClassName = idAndName.substring(idx + 1);
            if (path == null) {
               path = currentClassName;
            } else {
               path = processRelative(path);
               int lastSlashIdx = path.lastIndexOf("/");
               if (lastSlashIdx > 0) {
                  StringBuilder var10000 = new StringBuilder();
                  ++lastSlashIdx;
                  path = var10000.append(path.substring(0, lastSlashIdx)).append(currentClassName).toString();
               }
            }

            paths.add(path);
         }

         return paths;
      } else {
         return Collections.emptyList();
      }
   }
}
