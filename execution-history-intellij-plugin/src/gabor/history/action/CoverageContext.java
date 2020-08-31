package gabor.history.action;


import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessListener;
import com.intellij.ide.ActivityTracker;
import com.intellij.ide.highlighter.JavaFileType;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.scratch.ScratchFileService;
import com.intellij.ide.scratch.ScratchRootType;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorFactory;
import com.intellij.openapi.editor.event.EditorFactoryEvent;
import com.intellij.openapi.editor.event.EditorFactoryListener;
import com.intellij.openapi.editor.markup.*;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindowId;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import com.intellij.ui.JBColor;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.messages.MessageBusConnection;
import gabor.history.debug.executor.HistoryConfigurationSettingsStore;
import gabor.history.debug.type.ComplexType;
import gabor.history.debug.type.PlainType;
import gabor.history.debug.type.var.HistoryLocalVariable;
import gabor.history.debug.type.var.HistoryVar;
import gabor.history.helper.*;
import gabor.history.marker.DefaultLineMarkerRenderer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reuse.sequence.ShowSequenceAction;
import reuse.sequence.diagram.ObjectInfo;
import reuse.sequence.generator.CallStack;
import reuse.sequence.generator.ClassDescription;
import reuse.sequence.generator.MethodDescription;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class CoverageContext implements Disposable {
    private static final Map<Project, CoverageContext> contextMap;
    public static final String NO_VARIABLE_NAME = "$$history$$";
    public static final String NO_VARIABLE_NAME_SAVED = "$$history_saved$$";
    public static final String IS_COLLECTION_STRING = "$2!size$%#";
    public static final int IS_COLLECTION_STRING_SIZE = IS_COLLECTION_STRING.length();
    private int maxFieldRecursiveLimit = 3;
    private Project myProject;
    private ClassFilter[] includeFilters;
    private ClassFilter[] excludeFilters;
    private boolean includeConstructors;
    private boolean includeSettersGetters;
    private int maxBreakpoints;
    private int maxBreakpointsPerMethod;
    private int minMethodSize;
    private boolean myIsProjectClosing = false;
    private final Object myLock = new Object();
    private String coverageFile;
    private String eventFile;
    private String eventDirectory;
    private String commonDirectory;
    private File tempFile;
    private String finishedFile;
    private String callStackFile;
    private String instrumentationIsReadyFile;
    private ProjectData data;
    private final Map<Editor, List<RangeHighlighter>> highlighters = new HashMap<>();
    private boolean paused = false;
    private final Set<ProcessHandler> attachedHandlers = new HashSet<>();
    private BreakpointHelper breakpointHelper;
    private Set<String> patterns;
    private boolean loadTimeAttach = false;
    private boolean patternsWrittenToFile = false;
    private int mode = 2;
    private CallStack myCallStack;
    private Map<String, HashMap<String, Set<Integer>>> callCache;
    private Map<VirtualFile, Map<Integer, Set<LineVarInfo>>> varCache;
    private boolean ready;
    private boolean includeCollections = true;
    private int nrCollectionItems = 5;
    private int nrFieldsToShow = 10;

    static {
        contextMap = new HashMap<>();
    }

    public CoverageContext(@NotNull Project project) {
        this.myProject = project;

        MessageBusConnection connection = project.getMessageBus().connect();
        CoverageEditorFactoryListener coverageEditorFactoryListener = new CoverageEditorFactoryListener();
        EditorFactory.getInstance().addEditorFactoryListener(coverageEditorFactoryListener, myProject);

        breakpointHelper = new BreakpointHelper(this);

        Disposer.register(project, this);
        connection.subscribe(ProjectManager.TOPIC, new ProjectManagerListener() {
            @Override
            public void projectClosing(@NotNull Project project) {
                try {
                    if (project != myProject) {
                        return;
                    }

                    dispose();
                } catch (Throwable e) {
                    LoggingHelper.error(e);
                }
            }
        });
    }

    @Nullable
    public synchronized static CoverageContext getContextByProject(Project project) {
        return contextMap.get(project);
    }

    public static CoverageContext createContextByProjectAndFilters(@NotNull Project project, @Nullable HistoryConfigurationSettingsStore settings) {
        removeScratchFile();

        CoverageContext context = contextMap.get(project);

        if (context == null) {
            context = new CoverageContext(project);
            contextMap.put(project, context);
        }

        if (settings != null) {
            context.setSettings(settings);
        }

        context.setLoadTimeAttach(false);
        return context;
    }

    private void setSettings(@NotNull HistoryConfigurationSettingsStore settings) {
        this.includeFilters = settings.includeFilters;
        this.excludeFilters = settings.excludeFilters;
        this.includeConstructors = settings.includeConstructors;
        this.includeSettersGetters = settings.includeGettersSetters;
        this.maxBreakpoints = settings.maxBreakpoints;
        this.maxBreakpointsPerMethod = settings.maxBreakpointsPerMethod;
        this.minMethodSize = settings.minMethodSize;
        this.maxFieldRecursiveLimit = settings.maxDepthVariables;
        this.includeCollections = settings.includeCollections;
        this.nrCollectionItems = settings.nrCollectionItems;
        this.nrFieldsToShow = settings.nrFieldsToShow;

        if (settings.mode >= 0 && settings.mode <= 2) {
            this.mode = settings.mode;
        }

        this.patternsWrittenToFile = false;
    }

    public void createAllCoverageFiles() {
        createCommonFolder();
        createCoverageFile();
        createEventFile();
        createFinishedFile();
        createCallStackFile();
        createOwnTempFile();
        createInstrumentationIsReadyFile();

        resetFiles();
    }

    private void createOwnTempFile() {
        File temp;
        try {
            temp = FileHelper.createTempFile();
            temp.deleteOnExit();
            tempFile = new File(temp.getCanonicalPath());
        } catch (IOException e) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e2) {
            }
            tempFile = new File(FileHelper.createCoverageFileWithFolder(myProject, "tmpp", "covinfo", ".dat"));
        }
    }

    /*
    This method is only called when the history recorder is started not to cause slowdown when it is not used.
     */
    public void writePatternsToTempFile() {
        if (!patternsWrittenToFile) {

            if (!loadTimeAttach) {//if load time attached => files were already created
                createAllCoverageFiles();
            }

            try {
                patternsWrittenToFile = true;

                FileHelper.write2file(tempFile, new File(coverageFile).getCanonicalPath());
                FileHelper.write2file(tempFile, Boolean.FALSE.toString());
                FileHelper.write2file(tempFile, Boolean.FALSE.toString()); //append unloaded
                FileHelper.write2file(tempFile, Boolean.FALSE.toString());//merge with existing
                FileHelper.write2file(tempFile, Boolean.TRUE.toString());

                patterns = new HashSet<>();

                Collection<VirtualFile> containingFiles = FileBasedIndex.getInstance()
                        .getContainingFiles(
                                FileTypeIndex.NAME,
                                JavaFileType.INSTANCE,
                                new OnlyProjectSearchScope(myProject));

                for (VirtualFile virtualFile : containingFiles) {
                    PsiFile psiFile = PsiManager.getInstance(myProject).findFile(virtualFile);
                    if (psiFile instanceof PsiJavaFile) {
                        PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
                        PsiClass[] javaFileClasses = psiJavaFile.getClasses();

                        for (PsiClass javaFileClass : javaFileClasses) {
                            patterns.add(javaFileClass.getQualifiedName());
                        }
                    }
                }

                if (includeFilters.length > 0) {
                    FileHelper.write2file(tempFile, "-include");
                    FileHelper.writePatterns(tempFile, Arrays.stream(includeFilters).map(ClassFilter::getPattern).toArray(String[]::new));
                } else {
                    FileHelper.writeClasses(tempFile, patterns.toArray(new String[0]));
                }
                FileHelper.write2file(tempFile, "-exclude");
                FileHelper.writePatterns(tempFile, Arrays.stream(excludeFilters).map(ClassFilter::getPattern).toArray(String[]::new));

                FileHelper.write2file(tempFile, "-config");
                FileHelper.write2file(tempFile, String.valueOf(includeSettersGetters));
                FileHelper.write2file(tempFile, String.valueOf(includeConstructors));
                FileHelper.write2file(tempFile, String.valueOf(minMethodSize));
                FileHelper.write2file(tempFile, String.valueOf(maxBreakpointsPerMethod));
                FileHelper.write2file(tempFile, String.valueOf(maxBreakpoints));
            } catch (Throwable e) {
                LoggingHelper.error(e);
            }
        }
    }

    public void resetVarCache() {
        varCache = null;
    }

    public void reset() {
        myCallStack = null;
        callCache = null;
        resetVarCache();
    }

    private static void removeScratchFile() {
        try {
            VirtualFile scratchFile = ScratchFileService.getInstance().findFile(ScratchRootType.getInstance(), BreakpointHelper.HISTORY_RUNNER_BREAKPOINT_FILE, ScratchFileService.Option.existing_only);
            if (scratchFile != null) {
                new File(scratchFile.getCanonicalPath()).delete();
            }
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }

    public void refreshData() throws InterruptedException, IOException {
        //wait for ~ 100 seconds max
        File ffile = new File(finishedFile);
        for (int i = 0; i < 1000; i++) {
            if (ffile.exists()) {
                break;
            }

            //sleep until file appears
            Thread.sleep(100);
        }

        String sessionDataFile = new File(coverageFile).getCanonicalPath();
        File file = new File(sessionDataFile);

        //only application thread can read a file
        ApplicationManager.getApplication().invokeLater(() -> {
            try {
                data = ProjectDataLoader.load(file);
                highlightOpenFiles();
            } catch (Throwable e) {
                LoggingHelper.error(e);
            }

            //show covered classes next to classes' name
            try {
                ProjectView.getInstance(myProject).refresh();
            } catch (Throwable e) {
                LoggingHelper.error(e);
            }
        });

    }

    public void highlightOpenFiles() {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        final VirtualFile[] openFiles = fileEditorManager.getOpenFiles();
        for (VirtualFile openFile : openFiles) {
            PsiFile psiFile = PsiManager.getInstance(myProject).findFile(openFile);

            if (!(psiFile instanceof PsiJavaFile)) {
                continue;
            }

            PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
            PsiClass[] javaFileClasses = psiJavaFile.getClasses();

            final TreeMap<Integer, LineData> executableLines = getExecutableLines(javaFileClasses);

            if (psiJavaFile.isPhysical()) {
                final FileEditor[] allEditors = fileEditorManager.getAllEditors(openFile);

                for (FileEditor editor : allEditors) {
                    if (editor instanceof TextEditor) {
                        try {
                            final Editor textEditor = ((TextEditor) editor).getEditor();

                            //clear highlighters(consecutive runs shouldn't affect each other)
                            MarkupModel markupModel = textEditor.getMarkupModel();
                            List<RangeHighlighter> rangeHighlighters = highlighters.computeIfAbsent(textEditor, k -> new ArrayList<>());

                            for (RangeHighlighter highlighter : rangeHighlighters) {
                                markupModel.removeHighlighter(highlighter);
                            }
                            rangeHighlighters.clear();

                            executableLines.forEach((lineNumberInCurrent, lineData) -> {
                                highlightLine(lineNumberInCurrent, textEditor, lineData);
                            });
                        } catch (Throwable e) {
                            LoggingHelper.debug(e);
                        }
                    }
                }
            }
        }
    }

    public void removeHighlighters() {
        if (highlighters == null) {
            return;
        }

        highlighters.forEach((editor, editorHighlighters) -> {
            MarkupModel markupModel = editor.getMarkupModel();
            for (RangeHighlighter highlighter : editorHighlighters) {
                markupModel.removeHighlighter(highlighter);
            }
        });

        data = null;
    }

    public void showSequenceDiagram() {
        myCallStack = breakpointHelper.getCallStack();

        ShowSequenceAction showSequenceAction = new ShowSequenceAction();

        if (myCallStack.getCalls().size() > 0) {
            try {
                //order is important, first calculate cache then refresh line maekers
                calcCallCache();
                refreshLineMarkersInCurrentFile();
            } catch (Throwable e) {
                LoggingHelper.error(e);
            }
            //check callstack
            //RemoveHistoryRunnerConsoleAction.checkStack(callStack.getCalls().get(0).getCalls());

            showSequenceAction.actionPerformed(myProject, myCallStack);
        } else {
            new Thread(() -> {
                try {
                    File file = new File(finishedFile);
                    for (int i = 0; i < 1000; i++) {
                        if (file.exists()) {
                            break;
                        }

                        //sleep until file appears
                        Thread.sleep(100);
                    }

                    //only application thread can read a file
                    ApplicationManager.getApplication().invokeLater(() -> {
                        try {
                            myCallStack = readCallStackFromFile();
                            //RemoveHistoryRunnerConsoleAction.checkStack(callStackRun.getCalls().get(0).getCalls());

                            try {
                                //order is important, first calculate cache then refresh line maekers
                                calcCallCache();
                                refreshLineMarkersInCurrentFile();
                            } catch (Throwable e) {
                                LoggingHelper.error(e);
                            }

                            showSequenceAction.actionPerformed(myProject, myCallStack);
                        } catch (Throwable e) {
                            LoggingHelper.error(e);
                        }
                    });
                } catch (Throwable e) {
                    LoggingHelper.error(e);
                }
            }).start();
        }
    }

    public void calcCallCache() {
        callCache = new HashMap<>();
        calcCallCache(myCallStack);
    }

    private void calcCallCache(@Nullable CallStack callStack) {
        if (callStack == null) {
            return;
        }

        try {
            MethodDescription method = callStack.getMethod();
            if (method != null) {
                ClassDescription classDescription = method.getClassDescription();
                String methodName = method.getMethodName();
                if (methodName != null && classDescription != null && classDescription.getClassName() != null) {
                    String className = classDescription.getClassName();
                    int indexDollarSign = className.indexOf("$");
                    if (indexDollarSign > 0) {//inner or anonymous
                        className = className.substring(0, indexDollarSign + 1);
                    }
                    int line = method.getLine();
                    callCache.computeIfAbsent(className, k -> new HashMap<>()).computeIfAbsent(methodName, k -> new HashSet<>()).add(line);
                }
            }
        } catch (Throwable e) {
        }

        List<CallStack> calls = callStack.getCalls();
        if (calls != null) {
            for (CallStack call : calls) {
                calcCallCache(call);
            }
        }
    }

    public void refreshLineMarkersInCurrentFile() {
        final FileEditorManager fileEditorManager = FileEditorManager.getInstance(myProject);
        VirtualFile[] selectedFiles = fileEditorManager.getSelectedFiles();
        for (VirtualFile selectedFile : selectedFiles) {
            PsiFile file = PsiManager.getInstance(myProject).findFile(selectedFile);
            if (file != null) {
                DaemonCodeAnalyzer.getInstance(myProject).restart(file);
            }
        }
    }

    @Nullable
    private CallStack readCallStackFromFile() {
        List<CallStack> callStacks = new ArrayList<>();
        try {
            try (BufferedReader br = new BufferedReader(new FileReader(new File(callStackFile)))) {
                String line;

                CallStack curCallStack = null;
                boolean isFirstProjectClassTaken = false;
                boolean patternsAreEmpty = patterns == null || patterns.isEmpty();
                while ((line = br.readLine()) != null) {

                    if (line.contains("##")) {
                        curCallStack = SequenceAdapter.createCallStack(ObjectInfo.ACTOR_NAME, ObjectInfo.ACTOR_METHOD, 0, 0);
                        callStacks.add(curCallStack);
                        isFirstProjectClassTaken = false;
                    } else {
                        try {
                            String[] split = line.split("#");
                            String myClassName = split[0];
                            String methodSignature = split[1];
                            int lineNumber = Integer.parseInt(split[2]);
                            int hits = Integer.parseInt(split[3]);

                            //do not ignore inner classes
                            int indexOfDollarSign = myClassName.indexOf("$");
                            String containingClass = myClassName;
                            if (indexOfDollarSign >= 0) {
                                containingClass = myClassName.substring(0, indexOfDollarSign);
                            }

                            //remove non-project frames from begininning of stack trace
                            if (isFirstProjectClassTaken || patternsAreEmpty || patterns.contains(containingClass)) {
                                isFirstProjectClassTaken = true;

                                CallStack callStack = SequenceAdapter.createCallStack(myClassName, methodSignature, hits, lineNumber);

                                if (!patternsAreEmpty && !patterns.contains(containingClass)) {//todo remove duplicate check
                                    callStack.setProjectClass(false);
                                }

                                callStack.setVariables(new ArrayList<>(Arrays.asList(new HistoryLocalVariable(NO_VARIABLE_NAME, ""))));
                                curCallStack.setCalls(new ArrayList<>(Collections.singletonList(callStack)));
                                callStack.setParent(curCallStack);

                                curCallStack = callStack;
                            }
                        } catch (Throwable e) {
                            LoggingHelper.error(e);
                        }
                    }
                }
            }


            //merge them
            for (int i = 0; i < callStacks.size() - 1; i++) {
                CallStack currentCall = callStacks.get(i + 1);

                //search prev callstack which won't be removed
                int indexPrev;
                for (indexPrev = i; indexPrev >= 0 && callStacks.get(indexPrev).isCanRemove(); indexPrev--) {

                }
                CallStack prevCall = callStacks.get(indexPrev);

                //find last callstack which is common between the two
                boolean foundCommon = false;
                try {
                    while (prevCall != null && prevCall.getMethod() != null &&
                            currentCall != null && currentCall.getMethod() != null) {
                        if (prevCall.getMethod().equals(currentCall.getMethod())
                                && prevCall.getHits() == currentCall.getHits()) {


                            CallStack parent = currentCall.getParent();
                            CallStack prevBreakpointParent = prevCall.getParent();

                            if (parent == null ||
                                    prevBreakpointParent == null ||
                                    parent.getMethod().getLine() == prevBreakpointParent.getMethod().getLine()) {//method cannot be null, checked before

                                List<CallStack> calls = prevCall.getCalls();

                                if (calls.size() > 0 && calls.get(calls.size() - 1) != null) {
                                    List<CallStack> currentCalls = currentCall.getCalls();

                                    if (currentCalls.size() > 0 && currentCalls.get(currentCalls.size() - 1) != null) {
                                        foundCommon = true;
                                        //only advance the two callstacks if both are not null
                                        //prevCall
                                        calls.get(calls.size() - 1).setRealParent(prevCall);//else we'll lose it
                                        List<HistoryVar> variables = prevCall.getVariables();
                                        prevCall = calls.get(calls.size() - 1);
                                        currentCall.setVariables(variables);//variables are not extracted multiple times for the same method if the stack frames have common elements

                                        //currentCall
                                        currentCall = currentCalls.get(currentCalls.size() - 1);
                                    } else {
                                        break;
                                    }

                                } else {
                                    break;
                                }
                            } else {
                                break;
                            }
                        } else {
                            break;
                        }
                    }


                    //prevCall and currentCall shouldn't be null but they are still checked to be sure
                    if (foundCommon && prevCall != null && currentCall != null) {
                        CallStack parent = prevCall.getRealParent();
                        List<CallStack> calls = parent.getCalls();
                        calls.add(currentCall);

                        callStacks.get(i + 1).setCanRemove(true);
                    }
                } catch (Throwable e) {
                    LoggingHelper.error(e);
                }
            }

            callStacks.removeIf(CallStack::isCanRemove);
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }

        if (callStacks.size() > 0) {
            return callStacks.get(0);
        }
        return null;
    }

    public void setBreakpoint(@Nullable ProcessHandler processHandler) {
        if (mode == 2) {
            try {
                breakpointHelper.setBreakpoint(processHandler);
            } catch (Throwable e) {
                LoggingHelper.error(e);
            }
        } else {
            removeBreakpoint(processHandler);
        }
    }

    public void removeBreakpoint(@Nullable ProcessHandler processHandler) {
        try {
            breakpointHelper.removeBreakpoint(processHandler);
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }

    public void waitForInstrumentation(@NotNull ProcessHandler processHandler) {
        DebugProcess process = DebuggerManager.getInstance(myProject).getDebugProcess(processHandler);
        String currentToolWindow = process != null ? ToolWindowId.DEBUG : ToolWindowId.RUN;
        Notification notification = NotificationGroup.toolWindowGroup("demo.notifications.toolWindow", currentToolWindow)
                .createNotification(
                        "Waiting", "", "Please wait until the recorder icon turns RED",
                        NotificationType.INFORMATION);
        notification.notify(myProject);

        LoggingHelper.debug("waiting for instrumentation file#file=" + instrumentationIsReadyFile + "#mode=" + mode);

        new Thread(() -> {
            try {
                //wait for max 100 seconds
                File instrumentReadyFile = new File(instrumentationIsReadyFile);
                for (int i = 0; i < 1000; i++) {
                    try {
                        //if breakpoint hasn't been set yet, do not show Ready to the user, it won't record execution (applies only to mode = 2)
                        if (mode < 2 || breakpointHelper.isBreakpointWasSet()) {//no need to syncronize here
                            if (instrumentReadyFile.exists()) {
                                ready = true;
                                LoggingHelper.debug("instrumentation file ready#mode=" + mode + "#isBreakpointWasSet=" + breakpointHelper.isBreakpointWasSet());

                                ApplicationManager.getApplication().invokeLater(() -> {
                                    ActivityTracker.getInstance().inc();//update StartHistoryRunnerConsoleAction icon to READY

                                    Notification msg = NotificationGroup.toolWindowGroup("demo.notifications.toolWindow", currentToolWindow)
                                            .createNotification(
                                                    "Recording ON", "", "History Debugger is recording.",
                                                    NotificationType.INFORMATION);
                                    msg.notify(myProject);

                                });

                                break;
                            }
                        }

                        Thread.sleep(100);
                    } catch (Exception e) {
                        LoggingHelper.error(e);
                    }
                }
            } catch (Throwable e) {
                LoggingHelper.error(e);
            }
        }).start();
    }

    public static class LineVarInfo {
        public LineVarInfo(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String name;
        public String value;
    }

    public void calcVarCache(VirtualFile virtualFile, List<HistoryVar> variables, int maxOffset, int minOffset, PsiClass containingClass, String methodName) {
        if (mode == 2) {
            PsiFile psiFile = PsiManager.getInstance(myProject).findFile(virtualFile);

            if (psiFile == null) {
                return;
            }

            Document document = FileDocumentManager.getInstance().getDocument(virtualFile);

            if (document == null) {
                return;
            }

            PsiMethod method = DebuggerUtilsEx.findPsiMethod(psiFile, maxOffset);
            if (method == null || !method.getName().equals(methodName)) {
                return;
            }

            if (varCache == null) {
                varCache = new HashMap<>();
            }

            Map<String, String> currentFrameVars = new HashMap<>();
            for (HistoryVar variable : variables) {
                String name = variable.getName();
                Object value = variable.getValue();
                if (value instanceof PlainType) {
                    currentFrameVars.put(name, ((PlainType) value).getName());
                } else if (value instanceof ComplexType) {
                    String typeName = ((ComplexType) value).getName();
                    if (typeName.startsWith("java.lang.Boolean") || typeName.startsWith("java.lang.Integer") || typeName.startsWith("java.lang.Long")
                            || typeName.startsWith("java.lang.Float") || typeName.startsWith("java.lang.Double")
                            || typeName.startsWith("java.lang.Short") || typeName.startsWith("java.lang.Character")
                            || typeName.startsWith("java.lang.Byte")) {
                        List<HistoryVar> fieldVariables = variable.getFieldVariables();
                        if (fieldVariables != null && !fieldVariables.isEmpty()) {
                            Object plainType = fieldVariables.get(0).getValue();
                            if (plainType instanceof PlainType) {
                                currentFrameVars.put(name, ((PlainType) plainType).getName());
                            }
                        }
                    } else if (variable.getSize() >= 0) {
                        currentFrameVars.put(name, IS_COLLECTION_STRING + " " + variable.getSize());
                    }
                } else if (value instanceof String) {
                    currentFrameVars.put(name, (String) value);
                }
            }

            Map<Integer, Set<LineVarInfo>> varSet = new HashMap<>();

            method.accept(new PsiRecursiveElementWalkingVisitor() {
                @Override
                public void visitElement(@NotNull PsiElement element) {
                    if (element instanceof PsiVariable && !(element instanceof PsiField)) {
                        int textOffset = element.getTextOffset();
                        if (textOffset < maxOffset && textOffset >= minOffset) {//else can become slow
                            String name = ((PsiVariable) element).getName();
                            String value = currentFrameVars.get(name);
                            if (value != null) {
                                int lineNumber = document.getLineNumber(textOffset);
                                varSet.computeIfAbsent(lineNumber, k -> new HashSet<>()).add(new LineVarInfo(name, value));
                            }
                        }
                    }
                    super.visitElement(element);
                }
            });

            varSet.forEach((k, v) -> {
                List<LineVarInfo> collectionInstances = v.stream().filter(t -> t.value.startsWith(IS_COLLECTION_STRING)).collect(Collectors.toList());
                for (LineVarInfo collectionInstance : collectionInstances) {
                    collectionInstance.name += ": size";
                    collectionInstance.value = collectionInstance.value.substring(CoverageContext.IS_COLLECTION_STRING_SIZE);
                }
            });

            varCache.put(virtualFile, varSet);
        }
    }

    public class CoverageEditorFactoryListener implements EditorFactoryListener {

        @Override
        public void editorCreated(@NotNull EditorFactoryEvent event) {
            try {
                synchronized (myLock) {
                    if (myIsProjectClosing) return;
                }

                final Editor editor = event.getEditor();
                if (editor.getProject() != myProject) return;
                final PsiFile psiFile = ReadAction.compute(() -> {
                    if (myProject.isDisposed()) return null;
                    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(myProject);
                    final Document document = editor.getDocument();
                    return documentManager.getPsiFile(document);
                });

                if (!(psiFile instanceof PsiJavaFile)) {
                    return;
                }

                PsiJavaFile psiJavaFile = (PsiJavaFile) psiFile;
                PsiClass[] javaFileClasses = psiJavaFile.getClasses();

                final TreeMap<Integer, LineData> executableLines = getExecutableLines(javaFileClasses);

                if (psiFile.isPhysical()) {
                    executableLines.forEach((lineNumberInCurrent, lineData) -> {
                        highlightLine(lineNumberInCurrent, editor, lineData);
                    });
                }
            } catch (Throwable e) {
                LoggingHelper.debug(e);
            }
        }
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public void setProjectData(ProjectData data) {
        this.data = data;
    }

    public boolean isReady() {
        return ready;
    }

    public void setReady(boolean ready) {
        this.ready = ready;
    }

    @Nullable
    public Map<String, HashMap<String, Set<Integer>>> getCallCache() {
        return callCache;
    }

    @Nullable
    public CallStack getCallStack() {
        return myCallStack;
    }

    @Nullable
    public ProjectData getData() {
        return data;
    }

    public boolean isVarCacheCalculated() {
        return varCache != null;
    }

    @Nullable
    public Set<LineVarInfo> getVarsForVirtualFile(VirtualFile virtualFile, int line) {
        if (varCache == null) {
            return null;
        }

        Map<Integer, Set<LineVarInfo>> lineMapping = varCache.get(virtualFile);
        if (lineMapping == null) {
            return null;
        }

        return lineMapping.get(line);
    }

    public void setMyCallStack(@Nullable CallStack myCallStack) {
        this.myCallStack = myCallStack;
    }

    public String getFinishedFile() {
        return finishedFile;
    }

    public String getCallStackFile() {
        return callStackFile;
    }

    public String getEventFile() {
        return eventFile;
    }

    public String getEventDirectory() {
        return eventDirectory;
    }

    public String getTempFile() {
        return tempFile.getAbsolutePath();
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public Set<ProcessHandler> getAttachedHandlers() {
        return attachedHandlers;
    }

    public Project getMyProject() {
        return myProject;
    }

    public boolean isLoadTimeAttach() {
        return loadTimeAttach;
    }

    public void setLoadTimeAttach(boolean loadTimeAttach) {
        this.loadTimeAttach = loadTimeAttach;
    }

    public boolean isAttached(ProcessHandler handler) {
        return attachedHandlers.contains(handler);
    }


    public void setAttached(@NotNull ProcessHandler handler) {
        attachedHandlers.add(handler);

        handler.addProcessListener(new ProcessListener() {
            @Override
            public void startNotified(@NotNull ProcessEvent processEvent) {

            }

            @Override
            public void processTerminated(@NotNull ProcessEvent processEvent) {
                attachedHandlers.remove(processEvent.getProcessHandler());
                paused = false;
            }

            @Override
            public void processWillTerminate(@NotNull ProcessEvent processEvent, boolean b) {
                attachedHandlers.remove(processEvent.getProcessHandler());
                paused = false;
            }

            @Override
            public void onTextAvailable(@NotNull ProcessEvent processEvent, @NotNull Key key) {

            }
        });
    }

    public Set<String> getPatterns() {
        return patterns;
    }

    public String getCoverageFile() {
        return coverageFile;
    }

    public int getMaxFieldRecursiveLimit() {
        return maxFieldRecursiveLimit;
    }

    public boolean isIncludeCollections() {
        return includeCollections;
    }

    public int getNrCollectionItems() {
        return nrCollectionItems;
    }

    public int getNrFieldsToShow() {
        return nrFieldsToShow;
    }

    @Override
    public void dispose() {
        resetFiles();

        synchronized (myLock) {
            myIsProjectClosing = true;

            for (ProcessHandler attachedHandler : attachedHandlers) {
                if (!attachedHandler.isProcessTerminated()) {
                    breakpointHelper.removeBreakpoint(attachedHandler);
                }
            }

            //remove to prevent memory leaks
            contextMap.remove(myProject);

            myProject = null;
            patterns = null;
            breakpointHelper = null;
            myCallStack = null;
        }
    }

    private void highlightLine(Integer lineNumberInCurrent, @NotNull Editor textEditor, @NotNull LineData lineData) {
        Document document = textEditor.getDocument();
        final int startOffset = document.getLineStartOffset(lineNumberInCurrent);
        final int endOffset = document.getLineEndOffset(lineNumberInCurrent);
        MarkupModel markupModel = textEditor.getMarkupModel();

        int hits = lineData.getHits();

        final DefaultLineMarkerRenderer markerRenderer;
        if (hits == 0) {
            markerRenderer = new DefaultLineMarkerRenderer(2, JBColor.RED, LineMarkerRendererEx.Position.LEFT);
        } else {
            markerRenderer = new DefaultLineMarkerRenderer(2, JBColor.GREEN, LineMarkerRendererEx.Position.LEFT);
        }

        final RangeHighlighter highlighter =
                markupModel.addRangeHighlighter(startOffset, endOffset, HighlighterLayer.SELECTION - 1, null,
                        HighlighterTargetArea.LINES_IN_RANGE);
        highlighter.setLineMarkerRenderer(markerRenderer);


        List<RangeHighlighter> rangeHighlighters = highlighters.computeIfAbsent(textEditor, k -> new ArrayList<>());
        rangeHighlighters.add(highlighter);
    }

    @NotNull
    private TreeMap<Integer, LineData> getExecutableLines(@NotNull PsiClass[] javaFileClasses) {
        final TreeMap<Integer, LineData> executableLines = new TreeMap<>();

        if (data == null) {
            return executableLines;
        }

        for (PsiClass javaFileClass : javaFileClasses) {
            String qualifiedName = javaFileClass.getQualifiedName();

            if (qualifiedName != null) {

                ClassData classData = data.getClassData(qualifiedName);

                if (classData != null) {
                    final Object[] lines = classData.getLines();
                    if (lines != null) {
                        for (Object lineData : lines) {
                            if (lineData instanceof LineData) {
                                final int line = ((LineData) lineData).getLineNumber() - 1;
                                executableLines.put(line, (LineData) lineData);
                            }
                        }
                    }
                }
            }
        }
        return executableLines;
    }

    private void createCommonFolder() {
        commonDirectory = FileHelper.createCommonFolder(myProject);
    }

    private void createCoverageFile() {//these values are hardcoded in the agent for now
        coverageFile = FileHelper.createCoverageFileWithFolder(myProject, "history", "hist", "");
    }

    private void createEventFile() {//these values are hardcoded in the agent for now
        eventDirectory = FileHelper.createCommonFolder(myProject) + File.separator + "event";
        eventFile = FileHelper.createCoverageFileWithFolder(myProject, "event", "e", "");
    }

    private void createFinishedFile() {//these values are hardcoded in the agent for now
        finishedFile = FileHelper.createCoverageFileWithFolder(myProject, "finished", "e", "");
    }

    private void createCallStackFile() {//these values are hardcoded in the agent for now
        callStackFile = FileHelper.createCoverageFileWithFolder(myProject, "call", "e", "");
    }

    private void createInstrumentationIsReadyFile() {//these values are hardcoded in the agent for now
        instrumentationIsReadyFile = FileHelper.createCoverageFileWithFolder(myProject, "inst", "e", "");
    }

    @NotNull
    public String getAgentArgString(@NotNull String pluginPath) {
        //there is a ~900 character length limit on the String
        return this.getTempFile()
                + "#" + this.commonDirectory
                + "#" + this.loadTimeAttach
                + "#" + this.mode
                + "#" + pluginPath;
    }

    private void resetFiles() {
        try {
            FileHelper.deleteFile(instrumentationIsReadyFile);
            FileHelper.deleteFile(callStackFile);
            FileHelper.deleteFile(coverageFile);
            FileHelper.deleteFile(finishedFile);
            FileHelper.deleteFile(eventFile);//delete if it exists because it will be recreated at agent start, also if the process is terminated, this won't be deleted by the plugin beforehand
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }
}

