package modified.com.intellij.rt.coverage.main;

import modified.com.intellij.rt.coverage.data.ProjectData;
import modified.com.intellij.rt.coverage.data.Redirector;
import modified.com.intellij.rt.coverage.instrumentation.AbstractIntellijClassfileTransformer;
import modified.com.intellij.rt.coverage.instrumentation.CoverageClassfileTransformer;
import original.com.intellij.rt.coverage.instrumentation.SaveHook;
import original.com.intellij.rt.coverage.util.ClassNameUtil;
import original.com.intellij.rt.coverage.util.classFinder.ClassFinder;

import java.io.*;
import java.lang.instrument.Instrumentation;
import java.nio.file.FileSystem;
import java.nio.file.*;
import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;

public class StartFileWatcher implements Runnable {
    private String coverageFile;
    private File dataFile;
    private File finishedFile;
    private String startFile;
    private File callStackFile;
    private Instrumentation instrumentation;
    private String isLoadTime;
    private String instrumentationReadyFile;
    private boolean firstTime = true;
    private ClassFinder cf;
    private final Object lock = new Object();

    public StartFileWatcher(String coverageFile, String startFile, File finishedFile, File callStackFile,
                            Instrumentation instrumentation, String isLoadTime, String instrumentationReadyFile) {
        this.coverageFile = coverageFile;
        this.startFile = startFile;
        this.finishedFile = finishedFile;
        this.callStackFile = callStackFile;
        this.instrumentation = instrumentation;
        this.isLoadTime = isLoadTime;
        this.instrumentationReadyFile = instrumentationReadyFile;

        //first step is to load some classes which can cause deadlocks when invoking methods in the Debugger thread
        initDebuggerMethodCalls();

        synchronized (lock) {
            if ("false".equalsIgnoreCase(isLoadTime)) {
                readFilters();
                firstTime = false;
            }
        }
    }

    private void initDebuggerMethodCalls() {
        try {
            Float aFloat = 1.2f;
            Double aDouble = 1.2;
            Boolean aBoolean = false;
            Integer integer = 1;
            Long aLong = 1L;
            Short aShort = 142;
            Byte aByte = 12;
            Character character = 'r';

            aFloat.toString();
            aDouble.toString();
            aBoolean.toString();
            integer.toString();
            aLong.toString();
            aShort.toString();
            aByte.toString();
            character.toString();

            DayOfWeek.FRIDAY.ordinal();
            DayOfWeek.FRIDAY.toString();

            List<String> list1 = new LinkedList<>();
            List<String> list2 = new ArrayList<>();
            List<String> list3 = new Vector<>();
            List<String> list4 = new CopyOnWriteArrayList<>();
            list1.add("aa");
            list2.add("aa");
            list3.add("aa");
            list4.add("aa");

            list1.toArray();
            list2.toArray();
            list3.toArray();
            list4.toArray();

            Set<String> set1 = new TreeSet<>();
            Set<String> set2 = new HashSet<>();
            Set<String> set3 = new LinkedHashSet<>();
            Set<String> set4 = new CopyOnWriteArraySet<>();
            Set<DayOfWeek> set5 = EnumSet.allOf(DayOfWeek.class);
            set1.add("a");
            set2.add("a");
            set3.add("a");
            set4.add("a");
            set5.add(DayOfWeek.FRIDAY);

            set1.toArray();
            set2.toArray();
            set3.toArray();
            set4.toArray();
            set5.toArray();

            Map<String, String> map1 = new HashMap<>();
            map1.put("a", "a");
            Map<String, String> map2 = new LinkedHashMap<>();
            map2.put("a", "a");
            Map<String, String> map3 = new WeakHashMap<>();
            map3.put("a", "a");
            Map<String, String> map4 = new IdentityHashMap<>();
            map4.put("a", "a");
            EnumMap<DayOfWeek, String> activityMap = new EnumMap<>(DayOfWeek.class);
            activityMap.put(DayOfWeek.FRIDAY, "a");
            Map<String, String> map5 = new Hashtable<>();
            map5.put("a", "a");
            Map<String, String> map6 = new ConcurrentHashMap<>();
            map6.put("a", "a");
            Map<String, String> map7 = new TreeMap<>();
            map7.put("a", "a");

            map1.toString();//only called for hashmap

            map1.entrySet().toArray();
            map2.entrySet().toArray();
            map3.entrySet().toArray();
            map4.entrySet().toArray();
            map5.entrySet().toArray();
            map6.entrySet().toArray();
            map7.entrySet().toArray();
            activityMap.entrySet().toArray();
        } catch (Throwable e) {
            //e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            FileSystem fs = FileSystems.getDefault();
            WatchService ws = null;

            ws = fs.newWatchService();

            Path pTemp = Paths.get(startFile);
            pTemp.register(ws, ENTRY_CREATE, ENTRY_DELETE);
            while (true) {
                WatchKey k = ws.take();
                for (WatchEvent<?> e : k.pollEvents()) {
                    if (e.kind().equals(ENTRY_CREATE)) {

                        synchronized (lock) {
                            if (firstTime) {
                                readFilters();
                                firstTime = false;
                            } else {
                                finishedFile.delete();
                                modified.com.intellij.rt.coverage.data.ProjectData.DISABLED = false;
                                instrumentation.retransformClasses(Redirector.class);
                            }
                        }
                    } else if (e.kind().equals(ENTRY_DELETE)) {
                        modified.com.intellij.rt.coverage.data.ProjectData.DISABLED = true;
                        final SaveHook hook = new SaveHook(dataFile, false, cf);
                        hook.save(ProjectData.getProjectData());
                        hook.saveStackFrames(ProjectData.BREAKPOINTS, callStackFile);
                        ProjectData.reset(dataFile);

                        //create file when data has been written to dataFile so IDE can know when to start reading the file
                        finishedFile.createNewFile();
                        instrumentation.retransformClasses(Redirector.class);
                    }
                }

                k.reset();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void readFilters() {
        try {
            List<Pattern> includePatterns = new ArrayList<>();
            System.out.println("---- History Recorder Agent loaded ---- ");
            System.out.println("-Please wait until Recorder Icon becomes RED-");

            String[] args;
            try {
                args = this.readArgsFromFile(coverageFile);
            } catch (IOException e) {
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException ex) {
                    ex.printStackTrace();
                }

                args = this.readArgsFromFile(coverageFile);
            }

            dataFile = new File(args[0]);


            int i = 5;

            if ("-include".equals(args[i])) {//only patterns
                i++;
                for (; i < args.length && !"-exclude".equals(args[i]); ++i) {
                    try {
                        includePatterns.add(Pattern.compile(args[i]));
                    } catch (PatternSyntaxException var18) {
                        System.err.println("Problem occurred with include pattern " + args[i]);
                        System.err.println(var18.getDescription());
                        System.err.println("This may cause no tests run and no coverage collected");
                        System.exit(1);
                    }
                }
            } else {//all classes
                for (; i < args.length && !"-exclude".equals(args[i]); ++i) {
                    try {
                        ProjectData.CLASSES_PATTERNS.add(args[i]);
                    } catch (PatternSyntaxException var18) {
                        System.err.println("Problem occurred with include pattern " + args[i]);
                        System.err.println(var18.getDescription());
                        System.err.println("This may cause no tests run and no coverage collected");
                        System.exit(1);
                    }
                }
            }


            ++i;

            ArrayList<Pattern> excludePatterns;
            for (excludePatterns = new ArrayList<>(); i < args.length && !"-config".equals(args[i]); ++i) {
                try {
                    Pattern pattern = Pattern.compile(args[i]);
                    excludePatterns.add(pattern);
                } catch (PatternSyntaxException var17) {
                    System.err.println("Problem occurred with exclude pattern " + args[i]);
                    System.err.println(var17.getDescription());
                    System.err.println("This may cause no tests run and no coverage collected");
                    System.exit(1);
                }
            }

            if ("-config".equals(args[i])) {
                ProjectData.INCLUDE_GETTERS_SETTERS = Boolean.parseBoolean(args[++i]);
                ProjectData.INCLUDE_CONSTRUCTORS = Boolean.parseBoolean(args[++i]);
                ProjectData.MIN_METHOD_SIZE = Integer.parseInt(args[++i]);//future TODO
                ProjectData.MAX_BREAKPOINTS_PER_METHOD = Integer.parseInt(args[++i]);
                ProjectData.MAX_BREAKPOINTS = Integer.parseInt(args[++i]);
            }

            cf = new ClassFinder(includePatterns, excludePatterns);
            instrumentClasses(includePatterns, excludePatterns);
        } catch (Throwable e) {
        }
    }

    private void instrumentClasses(List<Pattern> includePatterns, List<Pattern> excludePatterns) {
        AbstractIntellijClassfileTransformer.InclusionPattern inclusionPattern = new AbstractIntellijClassfileTransformer.InclusionPattern() {
            public boolean accept(String className) {
                Iterator<Pattern> var2 = includePatterns.iterator();

                Pattern includePattern;
                do {
                    if (!var2.hasNext()) {
                        return false;
                    }

                    includePattern = var2.next();
                } while (!includePattern.matcher(className).matches());

                return true;
            }
        };


        Class<?>[] allLoadedClasses = instrumentation.getAllLoadedClasses();

        List<Class<?>> retransformClasses = new ArrayList<>();
        for (Class<?> loadedClass : allLoadedClasses) {
            if (loadedClass != null) {
                String canonicalName = null;
                try {
                    canonicalName = loadedClass.getName();//getName() for now to work with inner/anonymous classes
                } catch (Throwable e) {
                    //can throw  NoClassDefFoundError
                }
                if (canonicalName != null) {
                    //if inner class, check if parent is instrumentable
                    int indexOfDollarSign = canonicalName.indexOf("$");
                    if (indexOfDollarSign >= 0) {
                        canonicalName = canonicalName.substring(0, indexOfDollarSign);
                    }

                    if ((ProjectData.CLASSES_PATTERNS.contains(canonicalName) || inclusionPattern.accept(canonicalName)) &&
                            !ClassNameUtil.shouldExclude(canonicalName, excludePatterns)
                            && instrumentation.isModifiableClass(loadedClass)) {
                        retransformClasses.add(loadedClass);
                    }
                }
            }
        }

        //System.out.println("size transofrm: " + retransformClasses.size());


        //System.out.println("added classes to instrument");

        ProjectData data = null;
        try {
            data = ProjectData.createProjectData(dataFile, null, false, true);

            instrumentation.addTransformer(new CoverageClassfileTransformer(data, false, excludePatterns, includePatterns, cf),
                    true);

            if (retransformClasses.isEmpty()) {
                //create file when nothing is to be instrumented
                try {
                    new File(instrumentationReadyFile).createNewFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return;
            }

            //new Thread(()-> {
            transformClasses(retransformClasses, 0);
            try {
                new File(instrumentationReadyFile).createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            //   }).start();


//            System.out.println("***************");
//            System.out.println("----  DONE ---- ");
//            System.out.println("***************");
        } catch (IOException e) {
            try {
                new File(instrumentationReadyFile).createNewFile();
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            e.printStackTrace();
        }
    }

    private void transformClasses(List<Class<?>> retransformClasses, int index) {
        if (index > 100) {//if more than 100 failed attempts, stop
            return;
        }

        try {
            //one by one transformation is slow, so it is done in bulk
            instrumentation.retransformClasses(retransformClasses.toArray(new Class[0]));
        } catch (Throwable e) {
//            System.out.println("*******************************************************");
//            e.printStackTrace();
//            System.out.println("*******************************************************");

            synchronized (AbstractIntellijClassfileTransformer.TRANSFORMED_CLASSES) {
                retransformClasses.removeIf(cls -> AbstractIntellijClassfileTransformer.TRANSFORMED_CLASSES.contains(cls.getCanonicalName()));
            }

            if (index % 5 == 0) {
                //shuffle list to prevent same error again
                Collections.shuffle(retransformClasses);
            }
            transformClasses(retransformClasses, index + 1);
        }
    }

    private String[] readArgsFromFile(String arg) throws IOException {
        List<String> result = new ArrayList<>();
        File file = new File(arg);
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));

        try {
            while (reader.ready()) {
                result.add(reader.readLine());
            }
        } finally {
            reader.close();
        }

        return result.toArray(new String[0]);
    }
}
