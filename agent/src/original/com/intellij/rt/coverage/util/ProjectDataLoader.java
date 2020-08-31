package original.com.intellij.rt.coverage.util;

import modified.com.intellij.rt.coverage.data.ClassData;
import original.com.intellij.rt.coverage.data.LineData;
import modified.com.intellij.rt.coverage.data.ProjectData;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import org.jetbrains.coverage.gnu.trove.TIntObjectHashMap;

public class ProjectDataLoader {
   public static ProjectData load(File sessionDataFile) {
      ProjectData projectInfo = new ProjectData();
      DataInputStream in = null;

      ProjectData var4;
      try {
         in = new DataInputStream(new BufferedInputStream(new FileInputStream(sessionDataFile)));
         TIntObjectHashMap<ClassData> dict = new TIntObjectHashMap(1000, 0.99F);
         int classCount = CoverageIOUtil.readINT(in);

         int c;
         ClassData classInfo;
         for(c = 0; c < classCount; ++c) {
            classInfo = projectInfo.getOrCreateClassData(StringsPool.getFromPool(CoverageIOUtil.readUTFFast(in)));
            dict.put(c, classInfo);
         }

         for(c = 0; c < classCount; ++c) {
            classInfo = (ClassData)dict.get(CoverageIOUtil.readINT(in));
            int methCount = CoverageIOUtil.readINT(in);
            TIntObjectHashMap<LineData> lines = new TIntObjectHashMap(4, 0.99F);
            int maxLine = 1;

            for(int m = 0; m < methCount; ++m) {
               String methodSig = expand(in, dict);
               int lineCount = CoverageIOUtil.readINT(in);

               for(int l = 0; l < lineCount; ++l) {
                  int line = CoverageIOUtil.readINT(in);
                  LineData lineInfo = (LineData)lines.get(line);
                  if (lineInfo == null) {
                     lineInfo = new LineData(line, StringsPool.getFromPool(methodSig));
                     lines.put(line, lineInfo);
                     if (line > maxLine) {
                        maxLine = line;
                     }
                  }

                  classInfo.registerMethodSignature(lineInfo);
                  String testName = CoverageIOUtil.readUTFFast(in);
                  if (testName != null && testName.length() > 0) {
                     lineInfo.setTestName(testName);
                  }

                  int hits = CoverageIOUtil.readINT(in);
                  lineInfo.setHits(hits);
                  if (hits > 0) {
                     int jumpsNumber = CoverageIOUtil.readINT(in);

                     int switchesNumber;
                     for(switchesNumber = 0; switchesNumber < jumpsNumber; ++switchesNumber) {
                        lineInfo.setTrueHits(switchesNumber, CoverageIOUtil.readINT(in));
                        lineInfo.setFalseHits(switchesNumber, CoverageIOUtil.readINT(in));
                     }

                     switchesNumber = CoverageIOUtil.readINT(in);

                     for(int s = 0; s < switchesNumber; ++s) {
                        int defaultHit = CoverageIOUtil.readINT(in);
                        int keysLength = CoverageIOUtil.readINT(in);
                        int[] keys = new int[keysLength];
                        int[] keysHits = new int[keysLength];

                        for(int k = 0; k < keysLength; ++k) {
                           keys[k] = CoverageIOUtil.readINT(in);
                           keysHits[k] = CoverageIOUtil.readINT(in);
                        }

                        lineInfo.setDefaultHits(s, keys, defaultHit);
                        lineInfo.setSwitchHits(s, keys, keysHits);
                     }
                  }

                  lineInfo.fillArrays();
               }
            }

            classInfo.setLines(LinesUtil.calcLineArray(maxLine, lines));
         }

         return projectInfo;
      } catch (Exception var34) {
         ErrorReporter.reportError("Failed to load coverage data from file: " + sessionDataFile.getAbsolutePath(), var34);
         var4 = projectInfo;
      } finally {
         try {
            in.close();
         } catch (IOException var33) {
            ErrorReporter.reportError("Failed to close file: " + sessionDataFile.getAbsolutePath(), var33);
         }

      }

      return var4;
   }

   private static String expand(DataInputStream in, final TIntObjectHashMap<ClassData> dict) throws IOException {
      return CoverageIOUtil.processWithDictionary(CoverageIOUtil.readUTFFast(in), new CoverageIOUtil.Consumer() {
         protected String consume(String type) {
            int typeIdx;
            try {
               typeIdx = Integer.parseInt(type);
            } catch (NumberFormatException var4) {
               return type;
            }

            return ((ClassData)dict.get(typeIdx)).getName();
         }
      });
   }
}
