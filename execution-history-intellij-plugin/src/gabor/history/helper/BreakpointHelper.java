package gabor.history.helper;

import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.InstanceFilter;
import com.intellij.debugger.engine.*;
import com.intellij.debugger.engine.events.SuspendContextCommandImpl;
import com.intellij.debugger.engine.managerThread.DebuggerCommand;
import com.intellij.debugger.engine.requests.RequestManagerImpl;
import com.intellij.debugger.jdi.StackFrameProxyImpl;
import com.intellij.debugger.jdi.VirtualMachineProxyImpl;
import com.intellij.debugger.ui.breakpoints.FilteredRequestor;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.ide.scratch.ScratchRootType;
import com.intellij.lang.Language;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.xdebugger.breakpoints.SuspendPolicy;
import com.intellij.xdebugger.impl.XSourcePositionImpl;
import com.intellij.xdebugger.impl.breakpoints.XBreakpointUtil;
import com.sun.jdi.*;
import com.sun.jdi.event.LocatableEvent;
import com.sun.jdi.request.BreakpointRequest;
import com.sun.jdi.request.EventRequest;
import com.sun.jdi.request.EventRequestManager;
import gabor.history.action.CoverageContext;
import gabor.history.debug.DebugExtractor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reuse.sequence.diagram.ObjectInfo;
import reuse.sequence.generator.CallStack;
import reuse.sequence.generator.ClassDescription;
import reuse.sequence.generator.MethodDescription;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class BreakpointHelper {
    public static final String HISTORY_RUNNER_BREAKPOINT_FILE = "historyRunnerBreakpointFile";
    public static final String BREAKPOINT_METHOD_NAME = "breakPointHere";
    public static final String BREAKPOINT_ENTRY_METHOD_NAME = "checkFlagAtEnd";
    public static final String CLASS_DATA_FULL_NAME = "modified.com.intellij.rt.coverage.data.ClassData";
    private CoverageContext coverageContext;
    private List<CallStack> callStacks = new ArrayList<>();
    private static final int BREAKPOINT_POSITION = 149;
    private boolean breakpointWasSet = false;
    private final Object lock = new Object();
    private VirtualFile scratchFile;
    private DebugListener debugProcessListener;
//    private boolean firstRun = true;

    public BreakpointHelper(@NotNull CoverageContext coverageContext) {
        this.coverageContext = coverageContext;
    }

    public void reset() {
        callStacks = new ArrayList<>();
    }

    public void fetchValues(@NotNull SuspendContextImpl suspendContext)
            throws Exception {

        StackFrameProxyImpl sfProxy = suspendContext.getFrameProxy();

        CallStack prevCallStack = null;

        callStacks.removeIf(CallStack::isCanRemove);

        if (callStacks.size() > 0) {
            prevCallStack = callStacks.get(callStacks.size() - 1);
        }

        CallStack callStack = new CallStack();
        callStacks.add(callStack);
        DebugExtractor extractor = new DebugExtractor(sfProxy, suspendContext, callStack, coverageContext.getPatterns(),
                prevCallStack, coverageContext.getMaxFieldRecursiveLimit(), coverageContext.isIncludeCollections(),
                coverageContext.getNrCollectionItems(), coverageContext.getNrFieldsToShow());
        DebuggerManagerThreadImpl managerThread = suspendContext.getDebugProcess()
                .getManagerThread();
        managerThread.invokeCommand(extractor);
    }

    public class DebugListener implements DebugProcessListener, FilteredRequestor {
        private final DebugProcessImpl debugProcess;

        public DebugListener(DebugProcessImpl debugProcess) {
            this.debugProcess = debugProcess;
        }

        public void setBreakpoint() {
            DebuggerManagerThreadImpl managerThread = debugProcess.getManagerThread();
            new Thread(() -> {

                //wait for max 2 second
                for (int i = 0; i < 20; i++) {
                    managerThread.invokeCommand(new DebuggerCommand() {
                                                    @Override
                                                    public void action() {
                                                        VirtualMachineProxyImpl vmp = debugProcess.getVirtualMachineProxy();
                                                        VirtualMachine virtualMachine = vmp.getVirtualMachine();

                                                        synchronized (lock) {
                                                            if (!breakpointWasSet) {
                                                                List<ReferenceType> referenceTypes = virtualMachine.allClasses();
                                                                List<ReferenceType> classData = referenceTypes.stream().filter(t ->
                                                                        CLASS_DATA_FULL_NAME.equals(t.name())).collect(Collectors.toList());
                                                                if (classData.size() == 1) {
                                                                    List<Location> executableLines;
                                                                    try {
                                                                        executableLines = classData.get(0).allLineLocations();
                                                                    } catch (AbsentInformationException exc) {
                                                                        LoggingHelper.error(exc);
                                                                        return;
                                                                    }
                                                                    RequestManagerImpl requestManager = debugProcess.getRequestsManager();

                                                                    //check if method BREAKPOINT_ENTRY_METHOD_NAME in ClassData is not empty(instrumentation takes time)
//                                                                    int nrLines = 0;

//                                                                    for (Location loc : executableLines) {
//                                                                        if (loc.method().name().equals(BREAKPOINT_ENTRY_METHOD_NAME)) {
//                                                                            nrLines++;
//                                                                        }
//                                                                    }

//                                                                    if (firstRun) {
//                                                                        firstRun = false;

                                                                    boolean isBreakpointSet = false;

                                                                    EventRequestManager manager = debugProcess.getVirtualMachineProxy().eventRequestManager();
                                                                    List<BreakpointRequest> breakpointRequests = manager.breakpointRequests();
                                                                    for (BreakpointRequest breakPoint : breakpointRequests) {
                                                                        Location location = breakPoint.location();

                                                                        if (CLASS_DATA_FULL_NAME.equals(location.declaringType().name()) && BREAKPOINT_METHOD_NAME.equals(location.method().name())) {
                                                                            isBreakpointSet = true;
                                                                            breakpointWasSet = true;
                                                                            breakPoint.enable();
                                                                        }
                                                                    }

                                                                    if (!isBreakpointSet) {
                                                                        for (Location loc : executableLines) {
                                                                            if (loc.method().name().equals(BREAKPOINT_METHOD_NAME)) {
                                                                                BreakpointRequest breakpointRequest = requestManager.createBreakpointRequest(DebugListener.this, loc);
                                                                                breakpointRequest.setSuspendPolicy(EventRequest.SUSPEND_EVENT_THREAD);
                                                                                breakpointRequest.enable();


                                                                                breakpointWasSet = true;

                                                                                break;
                                                                            }
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }


                                                    }


                                                    @Override
                                                    public void commandCancelled() {

                                                    }
                                                }
                    );
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        LoggingHelper.error(e);
                    }
                }
            }).start();

        }

        @Override
        public void paused(@NotNull SuspendContext suspendContext) {
            try {
                if (suspendContext instanceof SuspendContextImpl) {
                    SuspendContextImpl suspendContextImpl = (SuspendContextImpl) suspendContext;

                    //stop if own breakpoint
                    Location location = suspendContextImpl.getLocation();
                    if (location != null) {
                        Method method = location.method();

                        if (method != null) {
                            String name = method.name();
                            if ("breakPointHere".equals(name)) {
                                fetchValues(suspendContextImpl);
                            }
                        }
                    }
                }
            } catch (Throwable e) {
                LoggingHelper.error(e);
            }
        }

        @Override
        public boolean processLocatableEvent(@NotNull SuspendContextCommandImpl action, LocatableEvent event) {
            return true;
        }

        @Override
        public String getSuspendPolicy() {
            return "SUSPEND_EVENT_THREAD";
        }

        @Override
        public boolean isInstanceFiltersEnabled() {
            return false;
        }

        @Override
        public InstanceFilter[] getInstanceFilters() {
            return new InstanceFilter[0];
        }

        @Override
        public boolean isCountFilterEnabled() {
            return false;
        }

        @Override
        public int getCountFilter() {
            return 0;
        }

        @Override
        public boolean isClassFiltersEnabled() {
            return false;
        }

        @Override
        public ClassFilter[] getClassFilters() {
            return new ClassFilter[0];
        }

        @Override
        public ClassFilter[] getClassExclusionFilters() {
            return new ClassFilter[0];
        }
    }

    public void removeBreakpoint(@Nullable ProcessHandler processHandler) {
        callStacks.clear();

        try {
            if (scratchFile != null) {
                new File(scratchFile.getCanonicalPath()).delete();
                scratchFile = null;
            }
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }

        try {
            Project project = coverageContext.getMyProject();
            DebugProcessImpl process = (DebugProcessImpl) DebuggerManager.getInstance(project).getDebugProcess(processHandler);

            if (process == null) {
                return;
            }

            DebuggerManagerThreadImpl managerThread = process.getManagerThread();

            managerThread.invokeCommand(new DebuggerCommand() {
                @Override
                public void action() {
                    EventRequestManager manager = process.getVirtualMachineProxy().eventRequestManager();
                    List<BreakpointRequest> breakpointRequests = manager.breakpointRequests();
                    for (BreakpointRequest breakPoint : breakpointRequests) {
                        Location location = breakPoint.location();

                        if (CLASS_DATA_FULL_NAME.equals(location.declaringType().name()) && BREAKPOINT_METHOD_NAME.equals(location.method().name())) {
                            breakPoint.disable();
                        }
                    }
                }

                @Override
                public void commandCancelled() {

                }
            });
        } catch (Throwable e) {
            LoggingHelper.debug(e);
        }
    }

    public void setBreakpoint(@Nullable ProcessHandler processHandler) {
        if (processHandler != null) {

            synchronized (lock) {
                breakpointWasSet = false;
            }

            reset();

            Project project = coverageContext.getMyProject();
            DebugProcess process = DebuggerManager.getInstance(project).getDebugProcess(processHandler);

            if (debugProcessListener == null) {
                debugProcessListener = new DebugListener((DebugProcessImpl) process);
                process.addDebugProcessListener(debugProcessListener);

                processHandler.addProcessListener(new ProcessListener() {
                    @Override
                    public void startNotified(@NotNull ProcessEvent processEvent) {

                    }

                    @Override
                    public void processTerminated(@NotNull ProcessEvent processEvent) {
                        process.removeDebugProcessListener(debugProcessListener);
                        debugProcessListener = null;

                        ApplicationManager.getApplication().invokeLater(() -> {
                            //delete scratch file
                            try {
                                VirtualFile scratchFile = ScratchFileService.getInstance().findFile(ScratchRootType.getInstance(), BreakpointHelper.HISTORY_RUNNER_BREAKPOINT_FILE, ScratchFileService.Option.existing_only);
                                if (scratchFile != null) {
                                    new File(scratchFile.getCanonicalPath()).delete();
                                }
                            } catch (Throwable e) {
                                LoggingHelper.error(e);
                            }
                        });
                    }

                    @Override
                    public void processWillTerminate(@NotNull ProcessEvent processEvent, boolean b) {
                    }

                    @Override
                    public void onTextAvailable(@NotNull ProcessEvent processEvent, @NotNull Key key) {

                    }
                });
            }

            try {
                debugProcessListener.setBreakpoint();
            } catch (Throwable e) {
                LoggingHelper.error(e);
            }

            //if breakpoint was not set even after 2.2 second, try another approach
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    synchronized (lock) {
                        if (!breakpointWasSet) {
                            ApplicationManager.getApplication().invokeLater(() -> {
                                breakpointWasSet = true;

                                scratchFile = ScratchRootType.getInstance().createScratchFile(project, HISTORY_RUNNER_BREAKPOINT_FILE, Language.findLanguageByID("JAVA"),
                                        BreakpointHelper.this.CLASS_DATA_SOURCE_CODE, ScratchFileService.Option.create_if_missing);

                                EditorFactory editorFactory = EditorFactory.getInstance();
                                Document document = FileDocumentManager.getInstance().getDocument(scratchFile);
                                Editor editor = editorFactory.createEditor(document, project, scratchFile, false);

                                final VirtualFile file = FileDocumentManager.getInstance().getFile(document);

                                XBreakpointUtil.toggleLineBreakpoint(project,
                                        XSourcePositionImpl.create(file, BREAKPOINT_POSITION),
                                        editor,
                                        false,
                                        false,
                                        false)
                                        .onSuccess(breakpoint -> {
                                            if (breakpoint != null) {
                                                breakpoint.setSuspendPolicy(SuspendPolicy.ALL);
                                            }
                                        });
                            });
                        }
                    }
                }
            }, 2200);
        }
    }

    @NotNull
    public CallStack getCallStack() {
        CallStack mainCallStack = new CallStack();

        callStacks.removeIf(t -> t.getMethod() == null);
        callStacks.removeIf(CallStack::isCanRemove);

        mainCallStack.setCalls(new ArrayList<>(callStacks));

        for (CallStack callStack : callStacks) {
            callStack.setParent(mainCallStack);
        }

        ClassDescription classDescription = new ClassDescription(ObjectInfo.ACTOR_NAME);
        MethodDescription methodDescription = new MethodDescription(classDescription, new ArrayList<>(), ObjectInfo.ACTOR_METHOD,
                "",
                new ArrayList<>(), new ArrayList<>());
        mainCallStack.setMethod(methodDescription);

        return mainCallStack;
    }

    public boolean isBreakpointWasSet() {
        return breakpointWasSet;
    }

    private final String CLASS_DATA_SOURCE_CODE = "\n" +
            "package modified.com.intellij.rt.coverage.data;\n" +
            "\n" +
            "import original.com.intellij.rt.coverage.data.CoverageData;\n" +
            "import original.com.intellij.rt.coverage.data.LineData;\n" +
            "import original.com.intellij.rt.coverage.data.LineMapData;\n" +
            "import original.com.intellij.rt.coverage.util.CoverageIOUtil;\n" +
            "import original.com.intellij.rt.coverage.util.DictionaryLookup;\n" +
            "import original.com.intellij.rt.coverage.util.ErrorReporter;\n" +
            "\n" +
            "import java.io.DataOutputStream;\n" +
            "import java.io.IOException;\n" +
            "import java.util.*;\n" +
            "\n" +
            "public class ClassData implements CoverageData {\n" +
            "    private final String myClassName;\n" +
            "    private LineData[] myLinesArray;\n" +
            "    private Map<String, Integer> myStatus;\n" +
            "    private int[] myLineMask;\n" +
            "    private String mySource;\n" +
            "    private Map<Integer, Integer> breakpointHitsPerMethod = new HashMap<>();\n" +
            "\n" +
            "    public ClassData(String name) {\n" +
            "        this.myClassName = name;\n" +
            "    }\n" +
            "\n" +
            "    public String getName() {\n" +
            "        return this.myClassName;\n" +
            "    }\n" +
            "\n" +
            "    public void reset() {\n" +
            "        try {\n" +
            "            if (myLinesArray != null) {\n" +
            "                for (LineData lineData : myLinesArray) {\n" +
            "                    if (lineData != null) {\n" +
            "                        lineData.setHits(0);\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "\n" +
            "            if (myLineMask != null) {\n" +
            "                Arrays.fill(myLineMask, 0);\n" +
            "            }\n" +
            "        } catch (Throwable e) {\n" +
            "            com.github.csabagabor.helper.LoggingHelper.error(e);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public void save(DataOutputStream os, DictionaryLookup dictionaryLookup) throws IOException {\n" +
            "        CoverageIOUtil.writeINT(os, dictionaryLookup.getDictionaryIndex(this.myClassName));\n" +
            "        Map<String, List<LineData>> sigLines = this.prepareSignaturesMap(dictionaryLookup);\n" +
            "        Set<String> sigs = sigLines.keySet();\n" +
            "        CoverageIOUtil.writeINT(os, sigs.size());\n" +
            "        Iterator var5 = sigs.iterator();\n" +
            "\n" +
            "        while (var5.hasNext()) {\n" +
            "            Object sig1 = var5.next();\n" +
            "            String sig = (String) sig1;\n" +
            "            CoverageIOUtil.writeUTF(os, sig);\n" +
            "            List<LineData> lines = (List) sigLines.get(sig);\n" +
            "            CoverageIOUtil.writeINT(os, lines.size());\n" +
            "            Iterator var9 = lines.iterator();\n" +
            "\n" +
            "            while (var9.hasNext()) {\n" +
            "                Object line = var9.next();\n" +
            "                ((LineData) line).save(os);\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    private Map<String, List<LineData>> prepareSignaturesMap(DictionaryLookup dictionaryLookup) {\n" +
            "        Map<String, List<LineData>> sigLines = new HashMap();\n" +
            "        if (this.myLinesArray == null) {\n" +
            "            return sigLines;\n" +
            "        } else {\n" +
            "            LineData[] var3 = this.myLinesArray;\n" +
            "            int var4 = var3.length;\n" +
            "\n" +
            "            for (int var5 = 0; var5 < var4; ++var5) {\n" +
            "                LineData lineData = var3[var5];\n" +
            "                if (lineData != null) {\n" +
            "                    if (this.myLineMask != null) {\n" +
            "                        lineData.setHits(this.myLineMask[lineData.getLineNumber()]);\n" +
            "                    }\n" +
            "\n" +
            "                    String sig = CoverageIOUtil.collapse(lineData.getMethodSignature(), dictionaryLookup);\n" +
            "                    List<LineData> lines = (List) sigLines.get(sig);\n" +
            "                    if (lines == null) {\n" +
            "                        lines = new ArrayList();\n" +
            "                        sigLines.put(sig, lines);\n" +
            "                    }\n" +
            "\n" +
            "                    ((List) lines).add(lineData);\n" +
            "                }\n" +
            "            }\n" +
            "\n" +
            "            return sigLines;\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public void merge(CoverageData data) {\n" +
            "        ClassData classData = (ClassData) data;\n" +
            "        this.mergeLines(classData.myLinesArray);\n" +
            "        Iterator var3 = this.getMethodSigs().iterator();\n" +
            "\n" +
            "        while (var3.hasNext()) {\n" +
            "            String o = (String) var3.next();\n" +
            "            this.myStatus.put(o, (Integer) null);\n" +
            "        }\n" +
            "\n" +
            "        if (this.mySource == null && classData.mySource != null) {\n" +
            "            this.mySource = classData.mySource;\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    private void mergeLines(LineData[] dLines) {\n" +
            "        if (dLines != null) {\n" +
            "            if (this.myLinesArray == null || this.myLinesArray.length < dLines.length) {\n" +
            "                LineData[] lines = new LineData[dLines.length];\n" +
            "                if (this.myLinesArray != null) {\n" +
            "                    System.arraycopy(this.myLinesArray, 0, lines, 0, this.myLinesArray.length);\n" +
            "                }\n" +
            "\n" +
            "                this.myLinesArray = lines;\n" +
            "            }\n" +
            "\n" +
            "            for (int i = 0; i < dLines.length; ++i) {\n" +
            "                LineData mergedData = dLines[i];\n" +
            "                if (mergedData != null) {\n" +
            "                    LineData lineData = this.myLinesArray[i];\n" +
            "                    if (lineData == null) {\n" +
            "                        lineData = new LineData(mergedData.getLineNumber(), mergedData.getMethodSignature());\n" +
            "                        this.registerMethodSignature(lineData);\n" +
            "                        this.myLinesArray[i] = lineData;\n" +
            "                    }\n" +
            "\n" +
            "                    lineData.merge(mergedData);\n" +
            "                }\n" +
            "            }\n" +
            "\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public void touchLine(int line) {\n" +
            "        this.myLineMask[line]++;\n" +
            "    }\n" +
            "\n" +
            "    public void breakPointHere(Map<String, Integer> hitsMap) {\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    public void checkFlagAtEnd(int currentFlag) {\n" +
            "        System.out.println(\"checkFlagAtEnd\");\n" +
            "        if (currentFlag == ProjectData.FLAG) {//last method call ==> breakpoint\n" +
            "            //check if not too many breakpoints on the same method\n" +
            "\n" +
            "            if (ProjectData.BREAKPOINTS_HIT > ProjectData.MAX_BREAKPOINT_HIT_LIMIT_TOTAL) {\n" +
            "                return;\n" +
            "            }\n" +
            "\n" +
            "            Integer hits = breakpointHitsPerMethod.putIfAbsent(currentFlag, 1);\n" +
            "            if (hits != null && hits > ProjectData.MAX_BREAKPOINT_HIT_LIMIT_PER_METHOD) {\n" +
            "                return;\n" +
            "            }\n" +
            "\n" +
            "            StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();\n" +
            "            Map<String, Integer> hitsMap = new HashMap<>();\n" +
            "\n" +
            "\n" +
            "            try {\n" +
            "                for (StackTraceElement stackTraceElement : stackTrace) {\n" +
            "                    try {\n" +
            "                        String className = stackTraceElement.getClassName();\n" +
            "                        int lineNumber = stackTraceElement.getLineNumber();\n" +
            "                        ClassData classData = ProjectData.getProjectData().getClassDataForBreakpoint(className);\n" +
            "\n" +
            "                        if (classData != null) {\n" +
            "                            int hitsLine = classData.myLineMask[lineNumber];\n" +
            "                            hitsMap.put(className + \"#\" + stackTraceElement.getLineNumber(), hitsLine);\n" +
            "                        }\n" +
            "\n" +
            "                    } catch (Throwable e) {\n" +
            "                        com.github.csabagabor.helper.LoggingHelper.error(e);\n" +
            "                    }\n" +
            "                }\n" +
            "            } catch (Throwable e) {\n" +
            "                com.github.csabagabor.helper.LoggingHelper.error(e);\n" +
            "            }\n" +
            "            System.out.println(\"breakPointHere\");\n" +
            "            breakPointHere(hitsMap);\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public void touch(int line) {\n" +
            "        LineData lineData = this.getLineData(line);\n" +
            "        if (lineData != null) {\n" +
            "            lineData.touch();\n" +
            "        }\n" +
            "    }\n" +
            "\n" +
            "    public void touch(int line, int jump, boolean hit) {\n" +
            "        LineData lineData = this.getLineData(line);\n" +
            "        if (lineData != null) {\n" +
            "            lineData.touchBranch(jump, hit);\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    public void touch(int line, int switchNumber, int key) {\n" +
            "        LineData lineData = this.getLineData(line);\n" +
            "        if (lineData != null) {\n" +
            "            lineData.touchBranch(switchNumber, key);\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    public void registerMethodSignature(LineData lineData) {\n" +
            "        this.initStatusMap();\n" +
            "        this.myStatus.put(lineData.getMethodSignature(), (Integer) null);\n" +
            "    }\n" +
            "\n" +
            "    public LineData getLineData(int line) {\n" +
            "        return this.myLinesArray[line];\n" +
            "    }\n" +
            "\n" +
            "    public Object[] getLines() {\n" +
            "        return this.myLinesArray;\n" +
            "    }\n" +
            "\n" +
            "    public boolean containsLine(int line) {\n" +
            "        return this.myLinesArray[line] != null;\n" +
            "    }\n" +
            "\n" +
            "    public Collection<String> getMethodSigs() {\n" +
            "        this.initStatusMap();\n" +
            "        return this.myStatus.keySet();\n" +
            "    }\n" +
            "\n" +
            "    private void initStatusMap() {\n" +
            "        if (this.myStatus == null) {\n" +
            "            this.myStatus = new HashMap();\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    public Integer getStatus(String methodSignature) {\n" +
            "        Integer methodStatus = (Integer) this.myStatus.get(methodSignature);\n" +
            "        if (methodStatus == null) {\n" +
            "            LineData[] var3 = this.myLinesArray;\n" +
            "            int var4 = var3.length;\n" +
            "\n" +
            "            for (int var5 = 0; var5 < var4; ++var5) {\n" +
            "                LineData lineData = var3[var5];\n" +
            "                if (lineData != null && methodSignature.equals(lineData.getMethodSignature()) && lineData.getStatus() != 0) {\n" +
            "                    methodStatus = 1;\n" +
            "                    break;\n" +
            "                }\n" +
            "            }\n" +
            "\n" +
            "            if (methodStatus == null) {\n" +
            "                methodStatus = 0;\n" +
            "            }\n" +
            "\n" +
            "            this.myStatus.put(methodSignature, methodStatus);\n" +
            "        }\n" +
            "\n" +
            "        return methodStatus;\n" +
            "    }\n" +
            "\n" +
            "    public String toString() {\n" +
            "        return this.myClassName;\n" +
            "    }\n" +
            "\n" +
            "    public void initLineMask(LineData[] lines) {\n" +
            "        int i;\n" +
            "        if (this.myLineMask == null) {\n" +
            "            this.myLineMask = new int[this.myLinesArray != null ? Math.max(lines.length, this.myLinesArray.length) : lines.length];\n" +
            "            if (this.myLinesArray != null) {\n" +
            "                for (i = 0; i < this.myLinesArray.length; ++i) {\n" +
            "                    LineData data = this.myLinesArray[i];\n" +
            "                    if (data != null) {\n" +
            "                        this.myLineMask[i] = data.getHits();\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "        } else {\n" +
            "            if (this.myLineMask.length < lines.length) {\n" +
            "                int[] lineMask = new int[lines.length];\n" +
            "                System.arraycopy(this.myLineMask, 0, lineMask, 0, this.myLineMask.length);\n" +
            "                this.myLineMask = lineMask;\n" +
            "            }\n" +
            "\n" +
            "            for (i = 0; i < lines.length; ++i) {\n" +
            "                if (lines[i] != null) {\n" +
            "                    int[] var10000 = this.myLineMask;\n" +
            "                    var10000[i] += lines[i].getHits();\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    public void setLines(LineData[] lines) {\n" +
            "        if (this.myLinesArray == null) {\n" +
            "            this.myLinesArray = lines;\n" +
            "        } else {\n" +
            "            this.mergeLines(lines);\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    public void checkLineMappings(LineMapData[] linesMap, ClassData classData) {\n" +
            "        if (linesMap != null) {\n" +
            "            LineData[] result;\n" +
            "            try {\n" +
            "                result = new LineData[linesMap.length];\n" +
            "                LineMapData[] var4 = linesMap;\n" +
            "                int var5 = linesMap.length;\n" +
            "\n" +
            "                for (int var6 = 0; var6 < var5; ++var6) {\n" +
            "                    LineMapData mapData = var4[var6];\n" +
            "                    if (mapData != null) {\n" +
            "                        result[mapData.getSourceLineNumber()] = classData.createSourceLineData(mapData);\n" +
            "                    }\n" +
            "                }\n" +
            "            } catch (Throwable var8) {\n" +
            "                ErrorReporter.reportError(\"Error creating line mappings for \" + classData.getName(), var8);\n" +
            "                return;\n" +
            "            }\n" +
            "\n" +
            "            this.myLinesArray = result;\n" +
            "            this.myLineMask = null;\n" +
            "        }\n" +
            "\n" +
            "    }\n" +
            "\n" +
            "    private LineData createSourceLineData(LineMapData lineMapData) {\n" +
            "        for (int i = lineMapData.getTargetMinLine(); i <= lineMapData.getTargetMaxLine() && i < this.myLinesArray.length; ++i) {\n" +
            "            LineData targetLineData = this.getLineData(i);\n" +
            "            if (targetLineData != null) {\n" +
            "                LineData lineData = new LineData(lineMapData.getSourceLineNumber(), targetLineData.getMethodSignature());\n" +
            "                lineData.merge(targetLineData);\n" +
            "                if (this.myLineMask != null) {\n" +
            "                    lineData.setHits(this.myLineMask[i]);\n" +
            "                }\n" +
            "\n" +
            "                return lineData;\n" +
            "            }\n" +
            "        }\n" +
            "\n" +
            "        return null;\n" +
            "    }\n" +
            "\n" +
            "    public void setSource(String source) {\n" +
            "        this.mySource = source;\n" +
            "    }\n" +
            "\n" +
            "    public String getSource() {\n" +
            "        return this.mySource;\n" +
            "    }\n" +
            "\n" +
            "    public int[] touchLines(int[] lines) {\n" +
            "        this.myLineMask = lines;\n" +
            "        return lines;\n" +
            "    }\n" +
            "}\n";
}
