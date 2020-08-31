package gabor.history.extension;

import com.intellij.execution.actions.ConsoleActionsPostProcessor;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.AnAction;
import gabor.history.action.StartHistoryRunnerConsoleAction;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;

public class HistoryRunnerConsoleActionsPostProcessor extends ConsoleActionsPostProcessor {

    @NotNull
    @Override
    public AnAction[] postProcess(@NotNull ConsoleView console, @NotNull AnAction[] actions) {
        ArrayList<AnAction> anActions = new ArrayList<>();
        anActions.add(new StartHistoryRunnerConsoleAction());
        anActions.addAll(Arrays.asList(actions));
        return anActions.toArray(new AnAction[anActions.size()]);
    }
}