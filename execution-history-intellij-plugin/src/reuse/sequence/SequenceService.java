package reuse.sequence;

import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.project.Project;
import reuse.sequence.generator.CallStack;
import reuse.sequence.generator.SequenceParams;
import reuse.sequence.generator.filters.CompositeMethodFilter;
import reuse.sequence.generator.filters.MethodFilter;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * &copy; fanhuagang@gmail.com
 * Created by van on 2020/2/23.
 */
public interface SequenceService {
    static SequenceService getInstance(@NotNull Project project) {
        return ServiceManager.getService(project, SequenceService.class);
    }

    void showSequence(SequenceParams params, CallStack callStack, Project project);

    void openClassInEditor(String className);

    void openMethodInEditor(String className, String methodName, List<String> argTypes);

    boolean isInsideAMethod();

    void openStackFrameInEditor(MethodFilter filter, String fromClass, String fromMethod, List<String> fromArgTypes,
                                String toClass, String toMethod, List<String> toArgType, int callNo, int line);

    List<String> findImplementations(String className);

    List<String> findImplementations(String className, String methodName, List<String> argTypes);

    void openLambdaExprInEditor(String fromClass, String fromMethod, List<String> fromArgTypes, List<String> argTypes, String returnType);

    void openStackFrameInsideLambdaExprInEditor(CompositeMethodFilter methodFilter, String fromClass, String enclosedMethodName, List<String> enclosedMethodArgTypes,
                                                List<String> argTypes, String returnType,
                                                String toClass, String toMethod, List<String> toArgTypes, int callNo);
}
