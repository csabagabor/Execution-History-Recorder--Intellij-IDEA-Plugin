

package modified.com.intellij.rt.coverage.data;

import original.com.intellij.rt.coverage.data.CoverageData;
import original.com.intellij.rt.coverage.data.FileMapData;
import original.com.intellij.rt.coverage.util.ErrorReporter;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ProjectData implements CoverageData, Serializable {
    private static final ProjectData.MethodCaller TOUCH_LINE_METHOD;
    private static final ProjectData.MethodCaller TOUCH_LINES_METHOD;
    private static final ProjectData.MethodCaller TOUCH_SWITCH_METHOD;
    private static final ProjectData.MethodCaller TOUCH_JUMP_METHOD;
    private static final ProjectData.MethodCaller TOUCH_METHOD;
    private static final ProjectData.MethodCaller GET_CLASS_DATA_METHOD;
    private static final ProjectData.MethodCaller TRACE_LINE_METHOD;
    private static boolean ourStopped;
    public static ProjectData ourProjectData;
    private File myDataFile;
    private String myCurrentTestName;
    private boolean myTraceLines;
    private boolean mySampling;
    private Map<modified.com.intellij.rt.coverage.data.ClassData, boolean[]> myTrace;
    private File myTracesDir;
    private final ProjectData.ClassesMap myClasses = new ProjectData.ClassesMap();
    private Map<String, FileMapData[]> myLinesMap;
    private static Object ourProjectDataObject;
    public static boolean DISABLED;
    public static int FLAG = 0;
    public static int METHOD_ID = 0;
    public static int BREAKPOINTS_HIT = 0;//incremented after each brekapoint hit
    public static List<BreakPointEvent> BREAKPOINTS = new ArrayList<>();

    public static final Set<String> CLASSES_PATTERNS = new HashSet<>();
    public static boolean INCLUDE_GETTERS_SETTERS = false;
    public static boolean INCLUDE_CONSTRUCTORS = false;
    public static int MIN_METHOD_SIZE = 2;
    public static int MAX_BREAKPOINTS_PER_METHOD = 20;
    public static int MAX_BREAKPOINTS = 200;
    public static int MODE = 2;
    public static boolean IS_AT_BREAKPOINT = false;

    public static class BreakPointEvent {
        public List<String> breakPointInfo = new ArrayList<>();
    }

    public ProjectData() {
    }

    public modified.com.intellij.rt.coverage.data.ClassData getClassData(String name) {
        return this.myClasses.get(name);
    }

    public modified.com.intellij.rt.coverage.data.ClassData getClassDataForBreakpoint(String name) {
        return this.myClasses.getForBreakpoint(name);
    }

    public modified.com.intellij.rt.coverage.data.ClassData getOrCreateClassData(String name) {
        modified.com.intellij.rt.coverage.data.ClassData classData = this.myClasses.get(name);
        if (classData == null) {
            classData = new modified.com.intellij.rt.coverage.data.ClassData(name);
            this.myClasses.put(name, classData);
        }

        return classData;
    }

    public static ProjectData getProjectData() {
        return ourProjectData;
    }

    public void stop() {
        ourStopped = true;
    }

    public boolean isStopped() {
        return ourStopped;
    }

    public boolean isSampling() {
        return this.mySampling;
    }

    public static ProjectData createProjectData(File dataFile, ProjectData initialData, boolean traceLines, boolean isSampling) throws IOException {
        ourProjectData = initialData == null ? new ProjectData() : initialData;

        if (dataFile != null && !dataFile.exists()) {
            File parentDir = dataFile.getParentFile();
            if (parentDir != null && !parentDir.exists()) {
                parentDir.mkdirs();
            }

            dataFile.createNewFile();
        }

        ourProjectData.mySampling = isSampling;
        ourProjectData.myTraceLines = traceLines;
        ourProjectData.myDataFile = dataFile;
        return ourProjectData;
    }

    public void merge(CoverageData data) {
        ProjectData projectData = (ProjectData) data;

        modified.com.intellij.rt.coverage.data.ClassData mergedData;
        modified.com.intellij.rt.coverage.data.ClassData classData;
        for (Iterator var3 = projectData.myClasses.names().iterator(); var3.hasNext(); classData.merge(mergedData)) {
            Object o = var3.next();
            String key = (String) o;
            mergedData = projectData.myClasses.get(key);
            classData = this.myClasses.get(key);
            if (classData == null) {
                classData = new modified.com.intellij.rt.coverage.data.ClassData(mergedData.getName());
                this.myClasses.put(key, classData);
            }
        }

    }

    public void checkLineMappings() {
        if (this.myLinesMap != null) {
            Iterator var1 = this.myLinesMap.keySet().iterator();

            while (var1.hasNext()) {
                Object o = var1.next();
                String className = (String) o;
                modified.com.intellij.rt.coverage.data.ClassData classData = this.getClassData(className);
                FileMapData[] fileData = (FileMapData[]) this.myLinesMap.get(className);
                FileMapData mainData = null;
                FileMapData[] var7 = fileData;
                int var8 = fileData.length;

                for (int var9 = 0; var9 < var8; ++var9) {
                    FileMapData aFileData = var7[var9];
                    String fileName = aFileData.getClassName();
                    if (fileName.equals(className)) {
                        mainData = aFileData;
                    } else {
                        modified.com.intellij.rt.coverage.data.ClassData classInfo = this.getOrCreateClassData(fileName);
                        classInfo.checkLineMappings(aFileData.getLines(), classData);
                    }
                }

                if (mainData != null) {
                    classData.checkLineMappings(mainData.getLines(), classData);
                } else {
                    ErrorReporter.reportError("Class data was not extracted: " + className, new Throwable());
                }
            }
        }

    }

    public void addLineMaps(String className, FileMapData[] fileDatas) {
        if (this.myLinesMap == null) {
            this.myLinesMap = new HashMap();
        }

        this.myLinesMap.put(className, fileDatas);
    }

    public void testEnded(String name) {
        if (this.myTrace != null) {
            File traceFile = new File(this.getTracesDir(), name + ".tr");

            try {
                if (!traceFile.exists()) {
                    traceFile.createNewFile();
                }

                DataOutputStream os = null;

                try {
                    os = new DataOutputStream(new FileOutputStream(traceFile));
                    os.writeInt(this.myTrace.size());
                    Iterator var4 = this.myTrace.keySet().iterator();

                    while (var4.hasNext()) {
                        modified.com.intellij.rt.coverage.data.ClassData classData = (modified.com.intellij.rt.coverage.data.ClassData) var4.next();
                        os.writeUTF(classData.toString());
                        boolean[] lines = (boolean[]) this.myTrace.get(classData);
                        int numberOfTraces = 0;
                        boolean[] var8 = lines;
                        int var9 = lines.length;

                        for (int var10 = 0; var10 < var9; ++var10) {
                            boolean line = var8[var10];
                            if (line) {
                                ++numberOfTraces;
                            }
                        }

                        os.writeInt(numberOfTraces);

                        for (int idx = 0; idx < lines.length; ++idx) {
                            boolean incl = lines[idx];
                            if (incl) {
                                os.writeInt(idx);
                            }
                        }
                    }
                } finally {
                    if (os != null) {
                        os.close();
                    }

                }
            } catch (IOException var21) {
                ErrorReporter.reportError("Error writing traces to file " + traceFile.getPath(), var21);
            } finally {
                this.myTrace = null;
            }

        }
    }

    public void testStarted(String name) {
        this.myCurrentTestName = name;
        if (this.myTraceLines) {
            this.myTrace = new ConcurrentHashMap();
        }

    }

    private File getTracesDir() {
        if (this.myTracesDir == null) {
            String fileName = this.myDataFile.getName();
            int i = fileName.lastIndexOf(46);
            String dirName = i != -1 ? fileName.substring(0, i) : fileName;
            this.myTracesDir = new File(this.myDataFile.getParent(), dirName);
            if (!this.myTracesDir.exists()) {
                this.myTracesDir.mkdirs();
            }
        }

        return this.myTracesDir;
    }

    public static String getCurrentTestName() {
        try {
            Object projectDataObject = getProjectDataObject();
            return (String) projectDataObject.getClass().getDeclaredField("myCurrentTestName").get(projectDataObject);
        } catch (Exception var1) {
            ErrorReporter.reportError("Current test name was not retrieved:", var1);
            return null;
        }
    }

    public static void reset(File dataFile) {
        try {
            BREAKPOINTS_HIT = 0;

            BREAKPOINTS = new ArrayList<>();
            ourProjectData.myClasses.asMap().forEach((name, classData) -> {
                classData.reset();
                classData.setBreakpointHitsPerMethod(new HashMap<>());
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Map<String, modified.com.intellij.rt.coverage.data.ClassData> getClasses() {
        return this.myClasses.asMap();
    }

    public static void touchLine(Object classData, int line) {
        if (DISABLED) {
            return;
        }

        if (ourProjectData != null) {
            ((modified.com.intellij.rt.coverage.data.ClassData) classData).touchLine(line);
        } else {
            touch(TOUCH_LINE_METHOD, classData, new Object[]{line});
        }
    }

    public static void touchSwitch(Object classData, int line, int switchNumber, int key) {
        if (ourProjectData != null) {
            ((modified.com.intellij.rt.coverage.data.ClassData) classData).touch(line, switchNumber, key);
        } else {
            touch(TOUCH_SWITCH_METHOD, classData, new Object[]{line, switchNumber, key});
        }
    }

    public static void touchJump(Object classData, int line, int jump, boolean hit) {
        if (ourProjectData != null) {
            ((modified.com.intellij.rt.coverage.data.ClassData) classData).touch(line, jump, hit);
        } else {
            touch(TOUCH_JUMP_METHOD, classData, new Object[]{line, jump, hit});
        }
    }

    public static void trace(Object classData, int line) {
        if (ourProjectData != null) {
            ((modified.com.intellij.rt.coverage.data.ClassData) classData).touch(line);
            ourProjectData.traceLine((modified.com.intellij.rt.coverage.data.ClassData) classData, line);
        } else {
            touch(TOUCH_METHOD, classData, new Object[]{line});

            try {
                Object projectData = getProjectDataObject();
                TRACE_LINE_METHOD.invoke(projectData, new Object[]{classData, line});
            } catch (Exception var3) {
                ErrorReporter.reportError("Error tracing class " + classData.toString(), var3);
            }

        }
    }

    private static Object touch(ProjectData.MethodCaller methodCaller, Object classData, Object[] paramValues) {
        try {
            return methodCaller.invoke(classData, paramValues);
        } catch (Exception var4) {
            ErrorReporter.reportError("Error in project data collection: " + methodCaller.myMethodName, var4);
            return null;
        }
    }

    public static int[] touchClassLines(String className, int[] lines) {
        if (ourProjectData != null) {
            return ourProjectData.getClassData(className).touchLines(lines);
        } else {
            try {
                Object projectDataObject = getProjectDataObject();
                Object classData = GET_CLASS_DATA_METHOD.invoke(projectDataObject, new Object[]{className});
                return (int[]) ((int[]) touch(TOUCH_LINES_METHOD, classData, new Object[]{lines}));
            } catch (Exception var4) {
                ErrorReporter.reportError("Error in class data loading: " + className, var4);
                return lines;
            }
        }
    }

    public static modified.com.intellij.rt.coverage.data.ClassData loadClassData(String className) {
        return ourProjectData.getClassData(className);
    }

    private static Object getProjectDataObject() throws ClassNotFoundException, IllegalAccessException, NoSuchFieldException {
        if (ourProjectDataObject == null) {
            Class projectDataClass = Class.forName(ProjectData.class.getName(), false, (ClassLoader) null);
            ourProjectDataObject = projectDataClass.getDeclaredField("ourProjectData").get((Object) null);
        }

        return ourProjectDataObject;
    }

    public void traceLine(modified.com.intellij.rt.coverage.data.ClassData classData, int line) {
        if (this.myTrace != null) {
            synchronized (this.myTrace) {
                boolean[] lines = (boolean[]) this.myTrace.get(classData);
                if (lines == null) {
                    lines = new boolean[line + 20];
                    this.myTrace.put(classData, lines);
                }

                if (lines.length <= line) {
                    boolean[] longLines = new boolean[line + 20];
                    System.arraycopy(lines, 0, longLines, 0, lines.length);
                    lines = longLines;
                    this.myTrace.put(classData, longLines);
                }

                lines[line] = true;
            }
        }

    }

    static {
        TOUCH_LINE_METHOD = new ProjectData.MethodCaller("touchLine", new Class[]{Integer.TYPE});
        TOUCH_LINES_METHOD = new ProjectData.MethodCaller("touchLines", new Class[]{int[].class});
        TOUCH_SWITCH_METHOD = new ProjectData.MethodCaller("touch", new Class[]{Integer.TYPE, Integer.TYPE, Integer.TYPE});
        TOUCH_JUMP_METHOD = new ProjectData.MethodCaller("touch", new Class[]{Integer.TYPE, Integer.TYPE, Boolean.TYPE});
        TOUCH_METHOD = new ProjectData.MethodCaller("touch", new Class[]{Integer.TYPE});
        GET_CLASS_DATA_METHOD = new ProjectData.MethodCaller("getClassData", new Class[]{String.class});
        TRACE_LINE_METHOD = new ProjectData.MethodCaller("traceLine", new Class[]{Object.class, Integer.TYPE});
        ourStopped = false;
    }

    private static class IdentityClassData {
        private final String myClassName;
        private final modified.com.intellij.rt.coverage.data.ClassData myClassData;

        private IdentityClassData(String className, modified.com.intellij.rt.coverage.data.ClassData classData) {
            this.myClassName = className;
            this.myClassData = classData;
        }

        public modified.com.intellij.rt.coverage.data.ClassData getClassData(String name) {
            return name == this.myClassName ? this.myClassData : null;
        }
    }

    private static class ClassesMap {
        private static final int POOL_SIZE = 1000;
        private final ProjectData.IdentityClassData[] myIdentityArray;
        private final Map<String, modified.com.intellij.rt.coverage.data.ClassData> myClasses;

        private ClassesMap() {
            this.myIdentityArray = new ProjectData.IdentityClassData[1000];
            this.myClasses = new HashMap(1000);
        }

        public modified.com.intellij.rt.coverage.data.ClassData getForBreakpoint(String name) {
            return myClasses.get(name);
        }

        public modified.com.intellij.rt.coverage.data.ClassData get(String name) {
            int idx = Math.abs(name.hashCode() % 1000);
            ProjectData.IdentityClassData lastClassData = this.myIdentityArray[idx];
            modified.com.intellij.rt.coverage.data.ClassData data;
            if (lastClassData != null) {
                data = lastClassData.getClassData(name);
                if (data != null) {
                    return data;
                }
            }

            data = (modified.com.intellij.rt.coverage.data.ClassData) this.myClasses.get(name);
            this.myIdentityArray[idx] = new ProjectData.IdentityClassData(name, data);
            return data;
        }

        public void put(String name, modified.com.intellij.rt.coverage.data.ClassData data) {
            this.myClasses.put(name, data);
        }

        public HashMap<String, ClassData> asMap() {
            return new HashMap(this.myClasses);
        }

        public Collection<String> names() {
            return this.myClasses.keySet();
        }
    }

    private static class MethodCaller {
        private Method myMethod;
        private final String myMethodName;
        private final Class[] myParamTypes;

        private MethodCaller(String methodName, Class[] paramTypes) {
            this.myMethodName = methodName;
            this.myParamTypes = paramTypes;
        }

        public Object invoke(Object thisObj, Object[] paramValues) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
            if (this.myMethod == null) {
                this.myMethod = findMethod(thisObj.getClass(), this.myMethodName, this.myParamTypes);
            }

            return this.myMethod.invoke(thisObj, paramValues);
        }

        private static Method findMethod(Class<?> clazz, String name, Class[] paramTypes) throws NoSuchMethodException {
            Method m = clazz.getDeclaredMethod(name, paramTypes);
            m.setAccessible(true);
            return m;
        }
    }
}
