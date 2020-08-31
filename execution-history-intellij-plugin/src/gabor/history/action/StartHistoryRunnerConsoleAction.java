package gabor.history.action;

import com.intellij.debugger.DebuggerManager;
import com.intellij.debugger.engine.DebugProcess;
import com.intellij.execution.ExecutionManager;
import com.intellij.execution.process.BaseProcessHandler;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.execution.ui.RunContentManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.project.Project;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.Kernel32;
import com.sun.jna.platform.win32.WinNT;
import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.spi.AttachProvider;
import gabor.helper.plugin.AttachHelper;
import gabor.history.ResourcesPlugin;
import gabor.history.helper.LoggingHelper;
import gabor.history.helper.PluginHelper;
import org.jetbrains.annotations.NotNull;
import sun.tools.attach.WindowsAttachProvider;

import java.io.File;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class StartHistoryRunnerConsoleAction extends AnAction {
    private static final String START_MSG = "Start History Recorder";
    private static final String STOP_MSG = "Stop History Recorder";

    static {
        try {
            Field providers = AttachProvider.class.getDeclaredField("providers");
            providers.setAccessible(true);
            List<AttachProvider> list = new ArrayList<>();
            String os = System.getProperty("os.name").toLowerCase();
            LoggingHelper.info("History Runner, type of OS:" + os);
            if (os.contains("win")) {
                LoggingHelper.info("Win attacher not found, attaching it");
                list.add(AttachHelper.createWindowsProvider());
            } else if (os.contains("mac")) {
                LoggingHelper.info("Mac attacher not found, attaching it");
                list.add(AttachHelper.createMacProvider());
            } else if (os.contains("nux")) {//linux on Linux
                LoggingHelper.info("Linux attacher not found, attaching it");
                list.add(AttachHelper.createLinuxProvider());
            }
            providers.set(null, list);
        } catch (Throwable e) {
            LoggingHelper.debug(e);
        }
    }

    public StartHistoryRunnerConsoleAction() {
        super(START_MSG, null, ResourcesPlugin.NO_RECORD_CONSOLE);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Project project = null;
        Presentation presentation = event.getPresentation();

        try {
            project = PlatformDataKeys.PROJECT.getData(event.getDataContext());
            RunContentManager contentManager = RunContentManager.getInstance(project);
            RunContentDescriptor selectedContent = contentManager.getSelectedContent();

            if (selectedContent != null) {
                ProcessHandler processHandler = selectedContent.getProcessHandler();

                if (processHandler != null) {
                    if (processHandler.isProcessTerminated()) {
                        presentation.setEnabled(false);
                        presentation.setIcon(ResourcesPlugin.NO_RECORD_CONSOLE);
                        presentation.setText(START_MSG);
                    } else {
                        presentation.setEnabled(true);

                        final CoverageContext coverageContext = CoverageContext.getContextByProject(project);

                        if (coverageContext != null && coverageContext.isPaused()) {
                            if (coverageContext.isReady()) {
                                presentation.setIcon(ResourcesPlugin.RECORD_CONSOLE);
                            } else {
                                presentation.setIcon(ResourcesPlugin.WAITING_CONSOLE);
                            }
                            presentation.setText(STOP_MSG);
                        } else {
                            presentation.setIcon(ResourcesPlugin.NO_RECORD_CONSOLE);
                            presentation.setText(START_MSG);
                        }
                    }
                }
            }
        } catch (Throwable e) {
            LoggingHelper.debug(e);
            try {
                ProcessHandler[] runningProcesses = ExecutionManager.getInstance(project).getRunningProcesses();

                if (runningProcesses.length == 0) {
                    presentation.setEnabled(false);
                    presentation.setIcon(ResourcesPlugin.NO_RECORD_CONSOLE);
                    presentation.setText(START_MSG);
                }

            } catch (Throwable e2) {
                LoggingHelper.debug(e2);
            }
        }

        super.update(event);
    }

    @Override
    public void actionPerformed(@NotNull final AnActionEvent event) {
        try {
            LoggingHelper.debug("startHistoryRunner");
            final Project project = PlatformDataKeys.PROJECT.getData(event.getDataContext());

            if (project == null) {
                return;
            }

            final CoverageContext coverageContext = CoverageContext.getContextByProject(project);

            if (coverageContext == null) {
                return;
            }

            coverageContext.setPaused(!coverageContext.isPaused());

            ProcessHandler processHandler = null;
            try {
                RunContentManager contentManager = RunContentManager.getInstance(project);
                RunContentDescriptor selectedContent = contentManager.getSelectedContent();
                processHandler = selectedContent.getProcessHandler();


            } catch (Throwable e) {
                LoggingHelper.error(e);

                ProcessHandler[] runningProcesses = ExecutionManager.getInstance(project).getRunningProcesses();
                for (ProcessHandler runningProcessHandler : runningProcesses) {
                    if (!runningProcessHandler.isProcessTerminated()) {
                        processHandler = runningProcessHandler;
                    }
                }
            }

            if (processHandler == null) {
                return;
            }

            LoggingHelper.debug("startHistoryRunner#readyToAttach");
            //***************************************************************************************************
            //has to happen before creating the event file, also before attaching the agent
            coverageContext.writePatternsToTempFile();
            //***************************************************************************************************
            if (!coverageContext.isAttached(processHandler)) {
                //if loadtime, still attach, to have event listener, when process terminates
                coverageContext.setAttached(processHandler);
                if(!coverageContext.isLoadTimeAttach() && coverageContext.isPaused()) {
                    attachAgentToVirtualMachine(processHandler, coverageContext);
                }
            }

            Presentation presentation = event.getPresentation();
            String eventFile = coverageContext.getEventFile();

            if (coverageContext.isPaused()) {
                presentation.setIcon(ResourcesPlugin.WAITING_CONSOLE);
                coverageContext.setReady(false);
                coverageContext.waitForInstrumentation(processHandler);
                presentation.setText(STOP_MSG);
                new File(eventFile).createNewFile();


                try {
                    DebugProcess process = DebuggerManager.getInstance(project).getDebugProcess(processHandler);

                    if (process != null) {//debugging mode
                        coverageContext.setBreakpoint(processHandler);
                    } else if (coverageContext.getMode() == 2) {//if RUN mode but still left on mode = 2, waitForInstrumentation won't work
                        coverageContext.setMode(1);
                    }

                } catch (Throwable e) {
                    LoggingHelper.error(e);
                }


            } else {

                new File(eventFile).delete();

                new Thread(() -> {
                    try {
                        coverageContext.refreshData();
                    } catch (Throwable e) {
                        LoggingHelper.error(e);
                    }
                }).start();

                try {
                    coverageContext.showSequenceDiagram();
                } catch (Throwable e) {
                    LoggingHelper.error(e);
                }

                coverageContext.removeBreakpoint(processHandler);
                presentation.setIcon(ResourcesPlugin.NO_RECORD_CONSOLE);
                presentation.setText(START_MSG);
            }

        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }

    private void attachAgentToVirtualMachine(ProcessHandler runningProcess, @NotNull CoverageContext coverageContext) throws Exception {
        try {
            if (runningProcess instanceof BaseProcessHandler) {
                Field field = BaseProcessHandler.class.getDeclaredField("myProcess");
                field.setAccessible(true);
                Object process = field.get(runningProcess);

                if (process instanceof Process) {
                    long processID = getProcessID((Process) process);
                    LoggingHelper.debug("processID_VirtualMachine=" + processID);

                    if (processID >= 0) {
                        new Thread(() -> {
                            try {
                                final VirtualMachine attachedVm = VirtualMachine.attach(String.valueOf(processID));
                                Optional<String> pluginPath = PluginHelper.getAgentPath();

                                LoggingHelper.debug("attachAgentToVirtualMachine");
                                if (pluginPath.isPresent()) {
                                    LoggingHelper.debug("attachAgentToVirtualMachine#hit");
                                    attachedVm.loadAgent(pluginPath.get(),
                                            coverageContext.getAgentArgString(pluginPath.get()));
                                }

                                attachedVm.detach();
                                LoggingHelper.debug("detached");
                            } catch (Throwable e) {
                                LoggingHelper.error(e);
                            }
                        }).start();
                    }
                }
            }
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
    }

    private long getProcessID(@NotNull Process p) {
        long result = -1;
        try {
            String name = p.getClass().getName();
            LoggingHelper.debug("getProcessID#className=" + name);
            //for unix based operating systems this should work
            //if field is not found, exception is thrown
            Field f = p.getClass().getDeclaredField("pid");
            f.setAccessible(true);
            result = f.getLong(p);
            f.setAccessible(false);
        } catch (Throwable ex) {
            LoggingHelper.debug("getProcessID#second_try#className");
            try {
                //"java.lang.ProcessImpl" on some systems contains 'pid', on others it contains 'handle'
                Field f = p.getClass().getDeclaredField("handle");
                f.setAccessible(true);
                long handl = f.getLong(p);
                Kernel32 kernel = Kernel32.INSTANCE;
                WinNT.HANDLE hand = new WinNT.HANDLE();
                hand.setPointer(Pointer.createConstant(handl));
                result = kernel.GetProcessId(hand);
                f.setAccessible(false);
            } catch (Throwable e) {
                LoggingHelper.error(e);
                result = -1;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "History Recorder";
    }
}
