package modified.com.intellij.rt.coverage.main;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.jar.JarFile;

public class CoveragePremain {
    public static final int NR_ARGUMENTS = 5;
    public static void main(String[] args) throws IOException {
//        toString2();
//        new ClassReader(Redirector.class.getResourceAsStream("Redirector.class"))
//                .accept(new TraceClassVisitor(null, new ASMifier(), new PrintWriter(System.out)), 0);

    }

    public static void agentmain(String argsString, Instrumentation instrumentation) throws Exception {
        if (argsString == null) {
            return;
        }

        String[] split = argsString.split("#");

        if (split.length != NR_ARGUMENTS) {
            return;
        }

        //System.out.println("agentmain");
        //ClassData and ProjectData needs to be loaded in the client app's code, this prevents NoClassDefFoundError
        instrumentation.appendToBootstrapClassLoaderSearch(new JarFile(split[4]));

        premain(argsString, instrumentation, "modified.com.intellij.rt.coverage.instrumentation.Instrumentator");
    }

    public static void premain(String argsString, Instrumentation instrumentation) throws Exception {
        premain(argsString, instrumentation, "modified.com.intellij.rt.coverage.instrumentation.Instrumentator");
    }

    public static void premain(String argsString, Instrumentation instrumentation, String instrumenterName) throws Exception {
        //System.out.println("loaded history runner agent");
        Class<?> instrumentator = Class.forName(instrumenterName, true, CoveragePremain.class.getClassLoader());
        Method premainMethod = instrumentator.getDeclaredMethod("premain", String.class, Instrumentation.class);
        premainMethod.invoke(null, argsString, instrumentation);
    }
}