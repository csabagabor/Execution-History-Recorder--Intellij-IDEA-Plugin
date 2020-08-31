package gabor.history.helper;

import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public final class FileHelper {

    public static final int MAX_PROJECT_LENGTH = 50;

    private FileHelper() {
    }

    public static String createCoverageFileWithFolder(Project project, String folderName, String fileName, String extension) {
        String projectName = getProjectName(project);
        @NonNls final String folderPath = PathManager.getSystemPath() + File.separator + "coverage" + File.separator + projectName
                + File.separator + folderName;
        final String path = folderPath + File.separator
                + FileUtil.sanitizeFileName(fileName) + extension;

        new File(folderPath).mkdirs();

        return path;
    }

    public static String createCommonFolder(Project project) {
        String projectName = getProjectName(project);
        @NonNls final String folderPath = PathManager.getSystemPath() + File.separator + "coverage" + File.separator + projectName;
        new File(folderPath).mkdirs();

        return folderPath;
    }

    @NotNull
    private static String getProjectName(Project project) {
        String projectName = FileUtil.sanitizeFileName(project.getName());
        if (projectName.length() > MAX_PROJECT_LENGTH) {
            projectName = projectName.substring(0, MAX_PROJECT_LENGTH);
        }
        return projectName;
    }

    public static void writeClasses(@NotNull File tempFile, @NotNull String[] patterns) throws IOException {
        //too slow
//        for (String coveragePattern : patterns) {
//            write2file(tempFile, coveragePattern);
//        }

        BufferedWriter bw = new BufferedWriter(new FileWriter(tempFile, true));
        for (String coveragePattern : patterns) {
            bw.write(coveragePattern);
            bw.newLine();
        }
        bw.close();
    }


    public static void writePatterns(@NotNull File tempFile, @NotNull String[] patterns) throws IOException {
        for (String coveragePattern : patterns) {
            coveragePattern = coveragePattern.replace("$", "\\$").replace(".", "\\.").replaceAll("\\*", ".*");
            if (!coveragePattern.endsWith(".*")) { //include inner classes
                coveragePattern += "(\\$.*)*";
            }
            write2file(tempFile, coveragePattern);
        }
    }

    public static void write2file(@NotNull File tempFile, @NotNull String arg) throws IOException {
        FileUtil.writeToFile(tempFile, (arg + "\n").getBytes(StandardCharsets.UTF_8), true);
    }

    public static File createTempFile() throws IOException {
        File tempFile = FileUtil.createTempFile("coverage", "args");
        if (!SystemInfo.isWindows && tempFile.getAbsolutePath().contains(" ")) {
            tempFile = FileUtil.createTempFile(new File(PathManager.getSystemPath(), "coverage"), "coverage", "args", true);
            if (tempFile.getAbsolutePath().contains(" ")) {
                final String userDefined = System.getProperty("java.test.agent.lib.path");
                if (userDefined != null && new File(userDefined).isDirectory()) {
                    tempFile = FileUtil.createTempFile(new File(userDefined), "coverage", "args", true);
                }
            }
        }
        return tempFile;
    }

    public static void deleteFile(@NotNull String path) {
        try {
            new File(path).delete();
        } catch (Exception e) {
            LoggingHelper.error(e);
        }
    }
}
