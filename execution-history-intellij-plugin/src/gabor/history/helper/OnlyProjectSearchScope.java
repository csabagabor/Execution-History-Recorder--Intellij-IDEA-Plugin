package gabor.history.helper;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

public class OnlyProjectSearchScope extends GlobalSearchScope {
    private final ProjectFileIndex index;

    public OnlyProjectSearchScope(Project project) {
        super(project);
        this.index = ProjectRootManager.getInstance(project).getFileIndex();
    }

    @Override
    public boolean isSearchInModuleContent(@NotNull Module module) {
        return false;
    }

    @Override
    public boolean isSearchInLibraries() {
        return false;
    }

    @Override
    public boolean contains(@NotNull VirtualFile file) {
        return index.isInSourceContent(file) && !index.isInTestSourceContent(file);
    }
}