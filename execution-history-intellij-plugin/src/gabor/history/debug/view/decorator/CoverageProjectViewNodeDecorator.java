package gabor.history.debug.view.decorator;

import com.intellij.coverage.AbstractCoverageProjectViewNodeDecorator;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.packageDependencies.ui.PackageDependenciesNode;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.rt.coverage.data.ClassData;
import com.intellij.rt.coverage.data.LineData;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.ui.ColoredTreeCellRenderer;
import gabor.history.action.CoverageContext;
import gabor.history.helper.LoggingHelper;
import gabor.history.helper.OnlyProjectSearchScope;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class CoverageProjectViewNodeDecorator extends AbstractCoverageProjectViewNodeDecorator {

    public CoverageProjectViewNodeDecorator(@NotNull Project project) {
        super(project);
    }

    @Override
    public void decorate(PackageDependenciesNode node, ColoredTreeCellRenderer cellRenderer) {
    }

    @Override
    public void decorate(ProjectViewNode node, PresentationData data) {
        final Project project = node.getProject();
        if (project == null) {
            return;
        }

        CoverageContext context = CoverageContext.getContextByProject(project);

        if (context == null) {
            return;
        }

        ProjectData projectData = context.getData();
        if (projectData == null) {
            return;
        }


        try {
            final Object value = node.getValue();
            PsiElement element = null;
            if (value instanceof PsiElement) {
                element = (PsiElement) value;
            } else if (value instanceof SmartPsiElementPointer) {
                element = ((SmartPsiElementPointer) value).getElement();
            }

            if (element instanceof PsiClass) {
                final GlobalSearchScope searchScope = new OnlyProjectSearchScope(project);
                final VirtualFile vFile = PsiUtilCore.getVirtualFile(element);
                if (vFile != null && searchScope.contains(vFile)) {
                    final String qName = ((PsiClass) element).getQualifiedName();
                    if (qName != null) {
                        ClassData classData = projectData.getClassData(qName);

                        if (classData != null) {
                            try {
                                int nrLinesHit = 0;
                                int nrLines = 0;

                                String source = classData.getSource();
                                if (source == null) {//not calculated before, source contains total number of hits
                                    LineData[] lines = (LineData[]) classData.getLines();

                                    if (lines != null) {
                                        nrLines = lines.length;
                                        for (LineData line : lines) {
                                            if (line != null) {
                                                int hits = line.getHits();
                                                if (hits > 0) {
                                                    nrLinesHit++;
                                                }
                                            }
                                        }
                                    }

                                    classData.setSource(nrLinesHit + "#" + nrLines);
                                } else {
                                    String[] split = source.split("#");
                                    nrLinesHit = Integer.parseInt(split[0]);
                                    nrLines = Integer.parseInt(split[1]);
                                }

                                if (nrLinesHit > 0) {
                                    double percentage = 100.0 * nrLinesHit / nrLines;
                                    if (percentage <= 100) {
                                        data.setLocationString(String.format("%.02f", percentage) + " % lines covered");
                                    }
                                }

                            } catch (Exception e) {
                                LoggingHelper.error(e);
                            }
                        }
                    }
                }
            } else if (element instanceof PsiDirectory) {
                PsiDirectory directory = (PsiDirectory) element;
                final PsiPackage psiPackage = JavaDirectoryService.getInstance().getPackage(directory);
                final VirtualFile virtualFile = directory.getVirtualFile();

                final GlobalSearchScope searchScope = new OnlyProjectSearchScope(project);
                final VirtualFile vFile = PsiUtilCore.getVirtualFile(element);
                if (virtualFile != null && searchScope.contains(vFile)) {
                    if (psiPackage != null) {
                        String name = psiPackage.getQualifiedName();

                        if (name != null) {
                            Map<String, ClassData> classes = projectData.getClasses();
                            if (classes != null) {

                                int nrLinesHitTotal = 0;
                                int nrLinesTotal = 0;
                                for (Map.Entry<String, ClassData> classDataEntry : classes.entrySet()) {
                                    ClassData classData = classDataEntry.getValue();

                                    if (classData != null && classData.getName().contains(name)) {
                                        //calc hits number if not done before
                                        int nrLinesHit = 0;
                                        int nrLines = 0;
                                        String source = classData.getSource();
                                        if (source == null) {//not calculated before, source contains total number of hits
                                            LineData[] lines = (LineData[]) classData.getLines();

                                            if (lines != null) {
                                                nrLines = lines.length;
                                                for (LineData line : lines) {
                                                    if (line != null) {
                                                        int hits = line.getHits();
                                                        if (hits > 0) {
                                                            nrLinesHit++;
                                                        }
                                                    }
                                                }
                                            }

                                            classData.setSource(nrLinesHit + "#" + nrLines);
                                        } else {
                                            String[] split = source.split("#");
                                            nrLinesHit = Integer.parseInt(split[0]);
                                            nrLines = Integer.parseInt(split[1]);
                                        }

                                        nrLinesHitTotal += nrLinesHit;
                                        nrLinesTotal += nrLines;
                                    }
                                }

                                if (nrLinesHitTotal > 0) {
                                    double percentage = 100.0 * nrLinesHitTotal / nrLinesTotal;
                                    if (percentage <= 100) {
                                        data.setLocationString(String.format("%.02f", percentage) + " % lines covered");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            LoggingHelper.debug(e);
        }
    }
}
