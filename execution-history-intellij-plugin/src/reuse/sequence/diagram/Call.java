package reuse.sequence.diagram;

import reuse.sequence.config.Configuration;

public class Call extends Link {

    public Call(ObjectInfo from, ObjectInfo to) {
        super(from, to);
    }

    public String getName() {
        if(getMethodInfo() == null)
            return super.getName();
        if(Configuration.getInstance().SHOW_CALL_NUMBERS)
            return super.getName();
        else
            return super.getName();
    }

    public String toString() {
        return "Calling " + getName() + " on " + _to + " from " + _from;
    }
}
