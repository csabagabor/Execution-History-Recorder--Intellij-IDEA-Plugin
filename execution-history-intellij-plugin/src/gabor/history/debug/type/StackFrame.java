

package gabor.history.debug.type;

import gabor.history.debug.type.var.HistoryVar;

import java.util.List;
import java.util.Objects;


public class StackFrame {
    private String className;
    private String fullClassName;
    private String name;
    private int line;
    private final List<HistoryVar> variables;
    private boolean isProjectClass = true;

    public StackFrame(String className, String fullClassName, String name, int line, List<HistoryVar> variables, boolean projectClass) {
        this.className = className;
        this.fullClassName = fullClassName;
        this.name = name;
        this.line = line;
        this.variables = variables;
        this.isProjectClass = projectClass;
    }

    public List<HistoryVar> getVars() {
        return variables;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getFullClassName() {
        return fullClassName;
    }

    public void setFullClassName(String fullClassName) {
        this.fullClassName = fullClassName;
    }

    public boolean isProjectClass() {
        return isProjectClass;
    }

    public void setProjectClass(boolean projectClass) {
        isProjectClass = projectClass;
    }

    @Override
    public String toString() {
        return name + ":" + (line + 1) + ", " + className + " (" + fullClassName + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StackFrame that = (StackFrame) o;
        return line == that.line &&
                Objects.equals(className, that.className) &&
                Objects.equals(fullClassName, that.fullClassName) &&
                Objects.equals(name, that.name) &&
                Objects.equals(variables, that.variables);
    }

    @Override
    public int hashCode() {
        return Objects.hash(className, fullClassName, name, line, variables);
    }
}
