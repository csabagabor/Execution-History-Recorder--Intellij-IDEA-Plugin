package reuse.sequence.diagram;

import reuse.sequence.config.Configuration;

public class Link {

    protected ObjectInfo _from;
    protected ObjectInfo _to;
    protected MethodInfo _methodInfo;
    protected MethodInfo _callerMethodInfo;
    protected int _seq;
    protected int hashCodeOfCallstack;

    public Link(ObjectInfo from, ObjectInfo to) {
        _from = from;
        _to = to;
    }

    public String getName() {
        if (_methodInfo == null)
            return "";
        if (Configuration.getInstance().SHOW_SIMPLIFY_CALL_NAME)
            return _methodInfo.getName();
        else
            return _methodInfo.getFullName();
    }

    public MethodInfo getMethodInfo() {
        return _methodInfo;
    }

    public void setMethodInfo(MethodInfo methodInfo) {
        _methodInfo = methodInfo;
    }

    public MethodInfo getCallerMethodInfo() {
        return _callerMethodInfo;
    }

    public void setCallerMethodInfo(MethodInfo callerMethodInfo) {
        _callerMethodInfo = callerMethodInfo;
    }

    public void setVerticalSeq(int y) {
        _seq = y;
    }

    public int getVerticalSeq() {
        return _seq;
    }

    public ObjectInfo getFrom() {
        return _from;
    }

    public ObjectInfo getTo() {
        return _to;
    }

    public boolean isBootstrap() {
        return _from.isActor() || _to.isActor();
    }

    public String toString() {
        return "Link from " + _from + " to " + _to;
    }

    public int getHashCodeOfCallstack() {
        return hashCodeOfCallstack;
    }

    public void setHashCodeOfCallstack(int hashCodeOfCallstack) {
        this.hashCodeOfCallstack = hashCodeOfCallstack;
    }
}
