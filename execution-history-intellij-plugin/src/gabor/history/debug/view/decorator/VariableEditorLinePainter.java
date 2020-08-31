package gabor.history.debug.view.decorator;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.EditorLinePainter;
import com.intellij.openapi.editor.LineExtensionInfo;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.Gray;
import com.intellij.ui.JBColor;
import com.intellij.xdebugger.ui.DebuggerColors;
import gabor.history.action.CoverageContext;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class VariableEditorLinePainter extends EditorLinePainter {
    @Override
    public Collection<LineExtensionInfo> getLineExtensions(@NotNull final Project project, @NotNull final VirtualFile file, final int lineNumber) {
        //Intellij 2020.2+
        //if (com.intellij.ide.lightEdit.LightEdit.owns(project)) return null;

        CoverageContext context = CoverageContext.getContextByProject(project);

        if (context == null || !context.isVarCacheCalculated()) {
            return null;
        }

        Document document = FileDocumentManager.getInstance().getDocument(file);

        if (document == null) {
            return null;
        }

        Set<CoverageContext.LineVarInfo> vars = context.getVarsForVirtualFile(file, lineNumber);
        TextAttributes attributes = getNormalAttributes();

        List<LineExtensionInfo> lineInfos = new ArrayList<>();
        if (vars != null) {
            for (CoverageContext.LineVarInfo var : vars) {
                lineInfos.add(new LineExtensionInfo("  " + var.name + " = " + var.value, attributes));
            }
        }

        return lineInfos;
    }

    private static TextAttributes getNormalAttributes() {
        TextAttributes attributes = EditorColorsManager.getInstance().getGlobalScheme().getAttributes(DebuggerColors.INLINED_VALUES);
        if (attributes == null || attributes.getForegroundColor() == null) {
            return new TextAttributes(new JBColor(() -> EditorColorsManager.getInstance().isDarkEditor() ? new Color(0x3d8065) : Gray._135), null, null, null, Font.ITALIC);
        }
        return attributes;
    }
}