package gabor.history.saver;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.rt.coverage.data.ProjectData;
import com.intellij.rt.coverage.util.ProjectDataLoader;
import gabor.history.action.CoverageContext;
import gabor.history.debug.type.ComplexType;
import gabor.history.debug.type.HistoryType;
import gabor.history.debug.type.PlainType;
import gabor.history.debug.type.var.*;
import gabor.history.helper.LoggingHelper;
import gabor.history.helper.PluginHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import reuse.sequence.ShowSequenceAction;
import reuse.sequence.generator.CallStack;
import reuse.sequence.generator.ClassDescription;
import reuse.sequence.generator.MethodDescription;

import javax.swing.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class RecordingSaver {

    public static final String LINES_COVERAGE_FILE = "lines.txt";
    private static final String VARIABLES_FILE = "variables.txt";
    private static final String VARIABLES_FILE_SERIALIZATION = "variables_ser.txt";
    private static final int MAX_FILE_SIZE_TOTAL = 500_000_000;//shouldn't save file > 500 mb
    private static final int MAX_FILE_SIZE_FOR_DUPLICATION = 40_000_000;//40 megabyte which will be 4mb when compressed => serialized file takes only 6-8mbs

    public static void loadRecordingFromFile(Project project, @NotNull File file) {
        try {
            ZipFile zipFile = new ZipFile(file.getAbsoluteFile());
            ZipEntry linesEntry = zipFile.getEntry(LINES_COVERAGE_FILE);
            ZipEntry variablesEntry = zipFile.getEntry(VARIABLES_FILE);
            ZipEntry variablesSerializationEntry = zipFile.getEntry(VARIABLES_FILE_SERIALIZATION);

            CoverageContext context = CoverageContext.createContextByProjectAndFilters(project, null);
            InputStream is = zipFile.getInputStream(linesEntry);
            try {
                Path unzipRecordingPath = Paths.get(file.getParent(), "unzipRecording");
                File unzipLinesFile = unzipRecordingPath.toFile();
                if (unzipLinesFile.exists()) {
                    unzipLinesFile.delete();
                }

                Files.copy(is, unzipRecordingPath);
                ProjectData data = ProjectDataLoader.load(unzipLinesFile);

                context.setProjectData(data);
            } catch (Throwable e) {
                LoggingHelper.error(e);
            }

            //show covered classes next to classes' name
            try {
                ProjectView.getInstance(project).refresh();
            } catch (Throwable e) {
                LoggingHelper.error(e);
            }

            context.highlightOpenFiles();


            //first try to read the kyro file, then if it fails, try to read the serialized file if it exists
            boolean isException = false;
            CallStack callStack = null;
            try {
                if (variablesEntry != null) {
                    Kryo kryo = getKryo();
                    Input input = new Input(zipFile.getInputStream(variablesEntry));
                    callStack = kryo.readObjectOrNull(input, CallStack.class);
                    input.close();
                }
            } catch (Throwable e) {
                isException = true;
                LoggingHelper.error(e);
            }

            if (isException || variablesEntry == null) {
                if (variablesSerializationEntry != null) {
                    ObjectInputStream oi = new ObjectInputStream(zipFile.getInputStream(variablesSerializationEntry));
                    callStack = (CallStack) oi.readObject();
                }
            }

            if (callStack == null) {
                return;
            }

            resetHashCodeValues(callStack, callStack.getMode());

            //order is important in these calls
            //first set callstack then calc Cache from it
            context.setMode(callStack.getMode());
            context.setMyCallStack(callStack);
            context.calcCallCache();
            context.refreshLineMarkersInCurrentFile();
            ShowSequenceAction showSequenceAction = new ShowSequenceAction();
            showSequenceAction.actionPerformed(project, callStack);
        } catch (Exception e) {
            LoggingHelper.error(e);
        }
    }

    public static void saveRecordingToFile(@Nullable Project project, @NotNull File file) {
        if (project == null) {
            return;
        }

        CoverageContext context = CoverageContext.getContextByProject(project);

        if (context == null) {
            return;
        }

        if (file.isDirectory()) {
            String fileName = JOptionPane.showInputDialog("Name of the file");
            file = new File(file, fileName + "." + PluginHelper.RECORDING_FILE_EXTENSION);
        }

        ZipOutputStream out = null;
        try {
            out = new ZipOutputStream(new FileOutputStream(file));

            String coverageFile = context.getCoverageFile();
            byte[] bytes = Files.readAllBytes(Paths.get(coverageFile));
            writeEntry(bytes, out, LINES_COVERAGE_FILE);

            try {
                bytes = null;
                CallStack callStack = context.getCallStack();

                if (callStack == null) {
                    return;
                }

                callStack.setMode(context.getMode());

                //first write it with kryo, if exception is thrown or file is small, write it with serialization as well
                boolean isException = false;
                try {
                    Kryo kryo = getKryo();
                    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                    Output output = new Output(byteArrayOutputStream);
                    kryo.writeObject(output, callStack);
                    output.close();
                    bytes = byteArrayOutputStream.toByteArray();

                    if (bytes.length > MAX_FILE_SIZE_TOTAL) {
                        JBPopup message = JBPopupFactory.getInstance().createMessage("Recording too large. Will omit stack frame information");
                        message.showInFocusCenter();
                        return;
                    }

                    writeEntry(bytes, out, VARIABLES_FILE);
                } catch (Throwable e) {
                    isException = true;
                    LoggingHelper.error(e);
                }

                if (isException || (bytes != null && bytes.length < MAX_FILE_SIZE_FOR_DUPLICATION)) {
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutputStream out2 = new ObjectOutputStream(bos);
                    out2.writeObject(callStack);
                    out2.flush();
                    writeEntry(bos.toByteArray(), out, VARIABLES_FILE_SERIALIZATION);
                }


            } catch (Throwable e) {
                LoggingHelper.error(e);
            }
        } catch (IOException ioException) {
            LoggingHelper.error(ioException);
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (Throwable e) {
            }
        }
    }

    private static void resetHashCodeValues(@Nullable CallStack callStack, int mode) {
        if (callStack == null) {
            return;
        }

        MethodDescription method = callStack.getMethod();
        if (method != null) {
            method.setHashOfCallStack(callStack.hashCode());
        }

        if (mode != 2) {
            callStack.setVariables(new ArrayList<>(Arrays.asList(new HistoryLocalVariable(CoverageContext.NO_VARIABLE_NAME_SAVED, ""))));
        }

        List<CallStack> calls = callStack.getCalls();
        if (calls != null) {
            for (CallStack call : calls) {
                resetHashCodeValues(call, mode);
            }
        }
    }

    private static void writeEntry(byte[] bytes, ZipOutputStream out, String name) throws IOException {
        ZipEntry e = new ZipEntry(name);
        out.putNextEntry(e);
        out.write(bytes, 0, bytes.length);
        out.closeEntry();
    }

    @NotNull
    private static Kryo getKryo() {
        Kryo kryo = new Kryo();
        kryo.register(CallStack.class);
        kryo.register(MethodDescription.class);
        kryo.register(HistoryVar.class);
        kryo.register(ClassDescription.class);
        kryo.register(HistoryLocalVariable.class);
        kryo.register(HistoryEntryVariable.class);
        kryo.register(HistoryPrimitiveVariable.class);
        kryo.register(HistoryArrayVariable.class);
        kryo.register(HistoryEnumVariable.class);
        kryo.register(ComplexType.class);
        kryo.register(PlainType.class);
        kryo.register(HistoryType.class);
        return kryo;
    }
}
