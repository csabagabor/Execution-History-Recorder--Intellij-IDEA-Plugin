package reuse.sequence;

import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reuse.sequence.generator.CallStack;
import reuse.sequence.generator.SequenceParams;

public class ShowSequenceAction {
    public void actionPerformed(@NotNull Project project, @Nullable CallStack callStack) {
        SequenceService plugin = getPlugin(project);
        SequenceParams params = new SequenceParams();
        if (plugin != null) {
            plugin.showSequence(params, callStack, project);
        }
    }

    private SequenceService getPlugin(Project project) {
        return project.getService(SequenceService.class);
    }
}
