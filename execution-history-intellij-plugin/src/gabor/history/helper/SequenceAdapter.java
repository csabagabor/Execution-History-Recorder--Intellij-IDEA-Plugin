package gabor.history.helper;

import com.intellij.openapi.project.Project;
import gabor.history.debug.type.StackFrame;
import gabor.history.debug.type.var.HistoryVar;
import gabor.history.debug.view.HistoryToolWindowService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reuse.sequence.diagram.ObjectInfo;
import reuse.sequence.generator.CallStack;
import reuse.sequence.generator.ClassDescription;
import reuse.sequence.generator.MethodDescription;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static reuse.sequence.diagram.ObjectInfo.ACTOR_NAME;

public class SequenceAdapter {

    public static void showVariableView(Project project, @Nullable CallStack callstack, @Nullable CallStack callStackCurrent) {
        if (callstack == null) {
            return;
        }

        if (isActorCallStack(callstack)) {
            return;
        }

        //construct calls from callstack
        List<StackFrame> stackFrames = new ArrayList<>();

        addStackFramesChildren(stackFrames, callStackCurrent, 0);
        Collections.reverse(stackFrames);//to align the stack frame items in the correct order

        StackFrame currentFrame = convertCallStackToStackFrame(callstack);
        stackFrames.add(currentFrame);

        addStackFramesParent(stackFrames, callstack, 0);

        if (stackFrames.size() > 0) {
            StackFrame stackFrame = stackFrames.get(stackFrames.size() - 1);
            if (ObjectInfo.ACTOR_METHOD.equals(stackFrame.getName()) && ObjectInfo.ACTOR_NAME.equals(stackFrame.getClassName())) {
                stackFrames.remove(stackFrame);
            }
        }

        //remove duplicates(if any)
        stackFrames = stackFrames.stream().distinct().filter(Objects::nonNull).collect(Collectors.toList());

        if (stackFrames.isEmpty()) {
            return;
        }

        if (currentFrame == null) {
            currentFrame = stackFrames.get(0);
        }

        HistoryToolWindowService.getInstance(project).showToolWindow(
                stackFrames, currentFrame);
    }

    private static void addStackFramesParent(@NotNull List<StackFrame> stackFrames, @Nullable CallStack callstack, int index) {
        if (callstack == null) {
            return;
        }

        if (index > 0) {
            try {
                stackFrames.add(convertCallStackToStackFrame(callstack));
            } catch (Throwable e) {
                LoggingHelper.error(e);
            }
        }

        addStackFramesParent(stackFrames, callstack.getParent(), ++index);
    }

    private static void addStackFramesChildren(@NotNull List<StackFrame> stackFrames, @Nullable CallStack callstack, int index) {
        if (callstack == null) {
            return;
        }

//        if (index > 0) {
        try {
            stackFrames.add(convertCallStackToStackFrame(callstack));
        } catch (Throwable e) {
            LoggingHelper.error(e);
        }
//        }

        List<CallStack> calls = callstack.getCalls();
        if (calls != null && calls.size() == 1) {//if there are multiple calls, cannot know which is the next call
            addStackFramesChildren(stackFrames, calls.get(0), ++index);
        } else {
            CallStack parent = callstack.getParent();
            if (parent != null) {
                MethodDescription method = parent.getMethod();
                if (method != null) {
                    String methodName = method.getMethodName();
                    ClassDescription classDescription = method.getClassDescription();

                    if (classDescription != null) {
                        String className = classDescription.getClassName();
                        if (ACTOR_NAME.equals(className) && ObjectInfo.ACTOR_METHOD.equals(methodName) && calls != null && calls.size() > 0) {
                            addStackFramesChildren(stackFrames, calls.get(0), ++index);
                        }
                    }
                }
            }
        }
    }

    @NotNull
    public static CallStack createCallStack(String className, String method, int hits, int line) {
        CallStack callStack = new CallStack();
        ClassDescription classDescription = new ClassDescription(className);
        MethodDescription methodDescription = new MethodDescription(classDescription, new ArrayList<>(), method,
                "",
                new ArrayList<>(), new ArrayList<>());
        callStack.setHits(hits);
        methodDescription.setLine(line);
        callStack.setMethod(methodDescription);

        methodDescription.setHashOfCallStack(callStack.hashCode());
        return callStack;
    }

    public static boolean isActorCallStack(CallStack callStack) {
        try {
            if (callStack.getParent() == null) {//fail fast
                MethodDescription method = callStack.getMethod();
                if (method != null) {
                    String methodName = method.getMethodName();
                    ClassDescription classDescription = method.getClassDescription();
                    if (classDescription != null) {
                        String className = classDescription.getClassName();
                        return ObjectInfo.ACTOR_METHOD.equals(methodName) && ACTOR_NAME.equals(className);
                    }
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    @Nullable
    private static StackFrame convertCallStackToStackFrame(@Nullable CallStack callstack) {
        if (callstack == null) {
            return null;
        }

        if (callstack.getMethod() == null && callstack.getCalls() != null &&
                callstack.getCalls().size() > 0) {
            callstack = callstack.getCalls().get(0);
        }

        String className = callstack.getMethod().getClassDescription().getClassName();
        String classShortName = callstack.getMethod().getClassDescription().getClassShortName();
        String methodName = callstack.getMethod().getMethodName();
        int line = callstack.getMethod().getLine();
        return new StackFrame(classShortName, className, methodName, line - 1, getLocalVariables(callstack), callstack.isProjectClass());
    }

    @NotNull
    private static List<HistoryVar> getLocalVariables(@NotNull CallStack callstack) {
        List<HistoryVar> variables = callstack.getVariables();
        if (variables == null) {
            variables = new ArrayList<>();
        }
        return variables;
    }
}
