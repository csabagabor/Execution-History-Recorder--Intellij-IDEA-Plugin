package modified.com.intellij.rt.coverage.data;

public class Redirector {
    //instrumenting ClassData again and again causes deoptimization and slowdon + errors when using boostrap classloader
    public static void checkFlagAtEnd(ClassData classData, int currentFlag) {
        classData.checkFlagAtEnd2(currentFlag);
    }
}
