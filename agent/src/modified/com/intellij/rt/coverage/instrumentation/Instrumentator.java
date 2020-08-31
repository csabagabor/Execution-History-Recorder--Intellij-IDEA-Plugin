
package modified.com.intellij.rt.coverage.instrumentation;

import modified.com.intellij.rt.coverage.data.ProjectData;
import modified.com.intellij.rt.coverage.main.CoveragePremain;
import modified.com.intellij.rt.coverage.main.StartFileWatcher;

import java.io.File;
import java.lang.instrument.Instrumentation;

public class Instrumentator {

    public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
        try {
            //needed as soon as possible to set breakpoint
            Class.forName("modified.com.intellij.rt.coverage.data.ClassData");
            Class.forName("modified.com.intellij.rt.coverage.data.Redirector");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        (new Instrumentator()).performPremain(argsString, instrumentation);
    }

    public void performPremain(String argsString, Instrumentation instrumentation) throws Exception {
        if (argsString == null) {
            return;
        }

        String[] split = argsString.split("#");

        if (split.length != CoveragePremain.NR_ARGUMENTS) {
            return;
        }

        String commonDirectory = split[1];//is used to save some space, there is a limit on argsString

        String eventFile = commonDirectory + File.separator + "event";

        File finishedFile = new File(commonDirectory + File.separator + "finished" + File.separator + "e");

        if (finishedFile.exists()) {
            finishedFile.delete();
        }

        File callStackFile = new File(commonDirectory + File.separator + "call" +  File.separator + "e");

        if (callStackFile.exists()) {
            callStackFile.delete();
        }

        try {
            ProjectData.MODE = Integer.parseInt(split[3]);
        } catch (NumberFormatException e) {
        }


        StartFileWatcher fileWatcher = new StartFileWatcher(split[0], eventFile, finishedFile, callStackFile,
                instrumentation, split[2], commonDirectory + File.separator + "inst" + File.separator + "e");
        new Thread(fileWatcher).start();
    }
}
