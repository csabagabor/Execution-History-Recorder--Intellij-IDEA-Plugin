package reuse.sequence.generator;

import gabor.history.debug.type.var.HistoryVar;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class CallStack implements Serializable {
    private static final long serialVersionUID = -2126293797318848679L;
    private MethodDescription _method;
    private CallStack _parent;
    private List<CallStack> _calls = new ArrayList<>();
    private int hits;
    private List<HistoryVar> variables;
    private int index;
    private boolean canRemove;
    private CallStack realParent;
    private boolean highlighted;
    private boolean filtered;
    private int mode;
    private boolean isProjectClass = true;

    public CallStack() {
    }

    public CallStack(MethodDescription method) {
        _method = method;
    }

    public CallStack(MethodDescription method, CallStack parent) {
        _method = method;
        _parent = parent;
    }

    public CallStack stackFrame(MethodDescription method) {
        CallStack callStack = new CallStack(method, this);
        _calls.add(callStack);
        return callStack;
    }

    public boolean isRecursive(MethodDescription method) {
        CallStack current = this;
        while (current != null) {
            if (current._method.equals(method))
                return true;
            current = current._parent;
        }
        return false;
    }

    public String generateSequence() {
        StringBuffer buffer = new StringBuffer();
        generate(buffer);
        return buffer.toString();
    }

    public MethodDescription getMethod() {
        return _method;
    }

    private void generate(StringBuffer buffer) {

            _method.getClassDescription().getAttributes().clear();
            _method.getClassDescription().getAttributes().add("highlight:" + isHighlighted());
            if (!isFiltered()) {
                buffer.append('(').append(_method.toJson()).append(' ');
            }
            for (Iterator<CallStack> iterator = _calls.iterator(); iterator.hasNext(); ) {
                CallStack callStack = iterator.next();
                callStack.generate(buffer);
                if (iterator.hasNext()) {
                    buffer.append(' ');
                }
            }
            if (!isFiltered()) {
                buffer.append(')');
            }
    }

    public String generateText() {
        StringBuffer buffer = new StringBuffer();
        int deep = 0;
        generateFormatStr(buffer, deep);
        return buffer.toString();
    }

    private void generateFormatStr(StringBuffer buffer, int deep) {
        for (int i = 0; i < deep; i++) {
            buffer.append("    ");
        }
        buffer.append(_method.toJson()).append('\n');
        for (CallStack callStack : _calls) {
            callStack.generateFormatStr(buffer, deep + 1);
        }
    }

    public void setMethod(MethodDescription _method) {
        this._method = _method;
    }

    public void setParent(CallStack _parent) {
        this._parent = _parent;
    }

    public void setCalls(List<CallStack> _calls) {
        this._calls = _calls;
    }

    public List<CallStack> getCalls() {
        return _calls;
    }

    public int getHits() {
        return hits;
    }

    public CallStack getParent() {
        return _parent;
    }

    public void setHits(int hits) {
        this.hits = hits;
    }

    public List<HistoryVar> getVariables() {
        return variables;
    }

    public void setVariables(List<HistoryVar> variables) {
        this.variables = variables;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean isCanRemove() {
        return canRemove;
    }

    public void setCanRemove(boolean canRemove) {
        this.canRemove = canRemove;
    }

    public CallStack getRealParent() {
        return realParent;
    }

    public void setRealParent(CallStack realParent) {
        this.realParent = realParent;
    }

    public boolean isHighlighted() {
        return highlighted;
    }

    public void setHighlighted(boolean highlighted) {
        this.highlighted = highlighted;
    }

    public void setFiltered(boolean filtered) {
        this.filtered = filtered;
    }

    public boolean isFiltered() {
        return filtered;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public boolean isProjectClass() {
        return isProjectClass;
    }

    public void setProjectClass(boolean projectClass) {
        isProjectClass = projectClass;
    }
}
