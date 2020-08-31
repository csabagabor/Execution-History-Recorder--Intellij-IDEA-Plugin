
package modified.com.intellij.rt.coverage.data;

import original.com.intellij.rt.coverage.data.CoverageData;
import original.com.intellij.rt.coverage.data.LineData;
import original.com.intellij.rt.coverage.data.LineMapData;
import original.com.intellij.rt.coverage.util.CoverageIOUtil;
import original.com.intellij.rt.coverage.util.DictionaryLookup;
import original.com.intellij.rt.coverage.util.ErrorReporter;

import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

public class ClassData implements CoverageData {
    private final String myClassName;
    private LineData[] myLinesArray;
    private Map<String, Integer> myStatus;
    private int[] myLineMask;
    private String mySource;
    private Map<Integer, Integer> breakpointHitsPerMethod = new HashMap<>();

    public ClassData(String name) {
        this.myClassName = name;
    }

    public String getName() {
        return this.myClassName;
    }

    public void reset() {
        try {
            if (myLinesArray != null) {
                for (LineData lineData : myLinesArray) {
                    if (lineData != null) {
                        lineData.setHits(0);
                    }
                }
            }

            if (myLineMask != null) {
                Arrays.fill(myLineMask, 0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void save(DataOutputStream os, DictionaryLookup dictionaryLookup) throws IOException {
        CoverageIOUtil.writeINT(os, dictionaryLookup.getDictionaryIndex(this.myClassName));
        Map<String, List<LineData>> sigLines = this.prepareSignaturesMap(dictionaryLookup);
        Set<String> sigs = sigLines.keySet();
        CoverageIOUtil.writeINT(os, sigs.size());
        Iterator var5 = sigs.iterator();

        while (var5.hasNext()) {
            Object sig1 = var5.next();
            String sig = (String) sig1;
            CoverageIOUtil.writeUTF(os, sig);
            List<LineData> lines = (List) sigLines.get(sig);
            CoverageIOUtil.writeINT(os, lines.size());
            Iterator var9 = lines.iterator();

            while (var9.hasNext()) {
                Object line = var9.next();
                ((LineData) line).save(os);
            }
        }

    }

    private Map<String, List<LineData>> prepareSignaturesMap(DictionaryLookup dictionaryLookup) {
        Map<String, List<LineData>> sigLines = new HashMap();
        if (this.myLinesArray == null) {
            return sigLines;
        } else {
            LineData[] var3 = this.myLinesArray;
            int var4 = var3.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                LineData lineData = var3[var5];
                if (lineData != null) {
                    if (this.myLineMask != null) {
                        lineData.setHits(this.myLineMask[lineData.getLineNumber()]);
                    }

                    String sig = CoverageIOUtil.collapse(lineData.getMethodSignature(), dictionaryLookup);
                    List<LineData> lines = (List) sigLines.get(sig);
                    if (lines == null) {
                        lines = new ArrayList();
                        sigLines.put(sig, lines);
                    }

                    ((List) lines).add(lineData);
                }
            }

            return sigLines;
        }
    }

    public void merge(CoverageData data) {
        ClassData classData = (ClassData) data;
        this.mergeLines(classData.myLinesArray);
        Iterator var3 = this.getMethodSigs().iterator();

        while (var3.hasNext()) {
            String o = (String) var3.next();
            this.myStatus.put(o, (Integer) null);
        }

        if (this.mySource == null && classData.mySource != null) {
            this.mySource = classData.mySource;
        }

    }

    private void mergeLines(LineData[] dLines) {
        if (dLines != null) {
            if (this.myLinesArray == null || this.myLinesArray.length < dLines.length) {
                LineData[] lines = new LineData[dLines.length];
                if (this.myLinesArray != null) {
                    System.arraycopy(this.myLinesArray, 0, lines, 0, this.myLinesArray.length);
                }

                this.myLinesArray = lines;
            }

            for (int i = 0; i < dLines.length; ++i) {
                LineData mergedData = dLines[i];
                if (mergedData != null) {
                    LineData lineData = this.myLinesArray[i];
                    if (lineData == null) {
                        lineData = new LineData(mergedData.getLineNumber(), mergedData.getMethodSignature());
                        this.registerMethodSignature(lineData);
                        this.myLinesArray[i] = lineData;
                    }

                    lineData.merge(mergedData);
                }
            }

        }
    }

    public void touchLine(int line) {
        this.myLineMask[line]++;
    }

    public void breakPointHere(Map<String, Integer> hitsMap) {

    }

    //if the name of `breakPointHere` is changed, it needs to be changed in the plugin code as well

    public void checkFlagAtEnd(int currentFlag) {
        Redirector.checkFlagAtEnd(this, currentFlag);
    }

    public void checkFlagAtEnd2(int currentFlag) {
        //System.out.println("checkFlagAtEnd");
        if (currentFlag == ProjectData.FLAG) {//last method call ==> breakpoint
            //check if not too many breakpoints on the same method

            if (ProjectData.BREAKPOINTS_HIT >= ProjectData.MAX_BREAKPOINTS) {
                return;
            }

            Integer hits = breakpointHitsPerMethod.putIfAbsent(currentFlag, 1);
            if (hits != null && hits > ProjectData.MAX_BREAKPOINTS_PER_METHOD) {
                return;
            }

            if (ProjectData.IS_AT_BREAKPOINT) {
                return;
            }

            ProjectData.BREAKPOINTS_HIT++;
            breakpointHitsPerMethod.put(currentFlag, breakpointHitsPerMethod.get(currentFlag) + 1);

            StackTraceElement[] stackTrace = new Throwable().getStackTrace();//somewhat faster than Thread.currentThread().getStackTrace()
            Map<String, Integer> hitsMap = new HashMap<>();

            ProjectData.BreakPointEvent breakPointEvent = new ProjectData.BreakPointEvent();
            try {
                for (StackTraceElement stackTraceElement : stackTrace) {
                    String className = null;
                    String breakPointInfo = "";
                    try {
                        className = stackTraceElement.getClassName();
                        int lineNumber = stackTraceElement.getLineNumber();
                        ClassData classData = ProjectData.getProjectData().getClassDataForBreakpoint(className);

                        if (!"modified.com.intellij.rt.coverage.data.ClassData".equals(className) &&
                                !"modified.com.intellij.rt.coverage.data.Redirector".equals(className) &&
                                !"java.lang.Thread".equals(className)) {
                            if (classData != null) {
                                try {
                                    String methodSignature = null;

                                    for (LineData lineData : classData.myLinesArray) {
                                        if (lineData != null && lineData.getLineNumber() == lineNumber) {
                                            methodSignature = lineData.getMethodSignature();
                                        }
                                    }

                                    int minLineNumber = lineNumber;//start of method

                                    if (methodSignature != null) {
                                        for (LineData lineData : classData.myLinesArray) {
                                            if (lineData != null && methodSignature.equals(lineData.getMethodSignature())) {
                                                int lineNumberTemp = lineData.getLineNumber();
                                                if (lineNumberTemp < minLineNumber) {
                                                    minLineNumber = lineNumberTemp;
                                                }
                                            }
                                        }
                                    }

                                    int hitsLine = classData.myLineMask[minLineNumber];
                                    hitsMap.put(className + "#" + lineNumber, hitsLine);

                                    breakPointInfo = classData.myClassName + "#" + stackTraceElement.getMethodName() + "#" + lineNumber + "#" + hitsLine;
                                    breakPointEvent.breakPointInfo.add(breakPointInfo);
//                                System.out.println(breakPointInfo);
                                } catch (Throwable e) {
                                    //e.printStackTrace();
                                    int hitsLine = classData.myLineMask[lineNumber];

                                    breakPointInfo = className + "#" + stackTraceElement.getMethodName() + "#" + lineNumber + "#" + hitsLine;
                                    breakPointEvent.breakPointInfo.add(breakPointInfo);

                                    hitsMap.put(className + "#" + lineNumber, hitsLine);
                                }
                            } else {
                                //without correct line number information we cannot construct the 100% correct sequence diagram
                                //but the stack trace can be shown correctly
                                breakPointInfo = className + "#" + stackTraceElement.getMethodName() + "#" + lineNumber + "#0";
                                breakPointEvent.breakPointInfo.add(breakPointInfo);
                            }
                        }
                    } catch (Throwable e) {
                        //System.out.println("className=" + className);
                        //e.printStackTrace();
                    }
                }
            } catch (Throwable e) {
                //e.printStackTrace();
            }

            synchronized (ProjectData.class) {
                if (!breakPointEvent.breakPointInfo.isEmpty()) {
                    ProjectData.BREAKPOINTS.add(breakPointEvent);
                }

                if (ProjectData.IS_AT_BREAKPOINT) {
                    return;
                }

                //when set to true we can be sure that no other thread sets it to true => no one will enter the breakpoint
                ProjectData.IS_AT_BREAKPOINT = true;
            }

            breakPointHere(hitsMap);
            ProjectData.IS_AT_BREAKPOINT = false;
            //System.out.println("breakPointHere");
        }
    }

    public void touch(int line) {
        LineData lineData = this.getLineData(line);
        if (lineData != null) {
            lineData.touch();
        }
    }

    public void touch(int line, int jump, boolean hit) {
        LineData lineData = this.getLineData(line);
        if (lineData != null) {
            lineData.touchBranch(jump, hit);
        }

    }

    public void touch(int line, int switchNumber, int key) {
        LineData lineData = this.getLineData(line);
        if (lineData != null) {
            lineData.touchBranch(switchNumber, key);
        }

    }

    public void registerMethodSignature(LineData lineData) {
        this.initStatusMap();
        this.myStatus.put(lineData.getMethodSignature(), (Integer) null);
    }

    public LineData getLineData(int line) {
        return this.myLinesArray[line];
    }

    public Object[] getLines() {
        return this.myLinesArray;
    }

    public boolean containsLine(int line) {
        return this.myLinesArray[line] != null;
    }

    public Collection<String> getMethodSigs() {
        this.initStatusMap();
        return this.myStatus.keySet();
    }

    private void initStatusMap() {
        if (this.myStatus == null) {
            this.myStatus = new HashMap();
        }

    }

    public Integer getStatus(String methodSignature) {
        Integer methodStatus = (Integer) this.myStatus.get(methodSignature);
        if (methodStatus == null) {
            LineData[] var3 = this.myLinesArray;
            int var4 = var3.length;

            for (int var5 = 0; var5 < var4; ++var5) {
                LineData lineData = var3[var5];
                if (lineData != null && methodSignature.equals(lineData.getMethodSignature()) && lineData.getStatus() != 0) {
                    methodStatus = 1;
                    break;
                }
            }

            if (methodStatus == null) {
                methodStatus = 0;
            }

            this.myStatus.put(methodSignature, methodStatus);
        }

        return methodStatus;
    }

    public String toString() {
        return this.myClassName;
    }

    public void initLineMask(LineData[] lines) {
        int i;
        if (this.myLineMask == null) {
            this.myLineMask = new int[this.myLinesArray != null ? Math.max(lines.length, this.myLinesArray.length) : lines.length];
            if (this.myLinesArray != null) {
                for (i = 0; i < this.myLinesArray.length; ++i) {
                    LineData data = this.myLinesArray[i];
                    if (data != null) {
                        this.myLineMask[i] = data.getHits();
                    }
                }
            }
        } else {
            if (this.myLineMask.length < lines.length) {
                int[] lineMask = new int[lines.length];
                System.arraycopy(this.myLineMask, 0, lineMask, 0, this.myLineMask.length);
                this.myLineMask = lineMask;
            }

            for (i = 0; i < lines.length; ++i) {
                if (lines[i] != null) {
                    int[] var10000 = this.myLineMask;
                    var10000[i] += lines[i].getHits();
                }
            }
        }

    }

    public void setLines(LineData[] lines) {
        if (this.myLinesArray == null) {
            this.myLinesArray = lines;
        } else {
            this.mergeLines(lines);
        }

    }

    public void checkLineMappings(LineMapData[] linesMap, ClassData classData) {
        if (linesMap != null) {
            LineData[] result;
            try {
                result = new LineData[linesMap.length];
                LineMapData[] var4 = linesMap;
                int var5 = linesMap.length;

                for (int var6 = 0; var6 < var5; ++var6) {
                    LineMapData mapData = var4[var6];
                    if (mapData != null) {
                        result[mapData.getSourceLineNumber()] = classData.createSourceLineData(mapData);
                    }
                }
            } catch (Throwable var8) {
                ErrorReporter.reportError("Error creating line mappings for " + classData.getName(), var8);
                return;
            }

            this.myLinesArray = result;
            this.myLineMask = null;
        }

    }

    private LineData createSourceLineData(LineMapData lineMapData) {
        for (int i = lineMapData.getTargetMinLine(); i <= lineMapData.getTargetMaxLine() && i < this.myLinesArray.length; ++i) {
            LineData targetLineData = this.getLineData(i);
            if (targetLineData != null) {
                LineData lineData = new LineData(lineMapData.getSourceLineNumber(), targetLineData.getMethodSignature());
                lineData.merge(targetLineData);
                if (this.myLineMask != null) {
                    lineData.setHits(this.myLineMask[i]);
                }

                return lineData;
            }
        }

        return null;
    }

    public void setSource(String source) {
        this.mySource = source;
    }

    public String getSource() {
        return this.mySource;
    }

    public int[] touchLines(int[] lines) {
        this.myLineMask = lines;
        return lines;
    }

    public Map<Integer, Integer> getBreakpointHitsPerMethod() {
        return breakpointHitsPerMethod;
    }

    public void setBreakpointHitsPerMethod(Map<Integer, Integer> breakpointHitsPerMethod) {
        this.breakpointHitsPerMethod = breakpointHitsPerMethod;
    }
}
