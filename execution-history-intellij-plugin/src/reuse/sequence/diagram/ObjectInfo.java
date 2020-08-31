package reuse.sequence.diagram;

import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

public class ObjectInfo extends Info {
    private static final Logger LOGGER = Logger.getLogger(ObjectInfo.class);
    public static final String ACTOR_NAME = "Actor";
    public static final String ACTOR_METHOD = "actor";

    private String _name;
    private String _fullName;
    private int _seq;
    private List<MethodInfo> _methods = new ArrayList<MethodInfo>();

    public ObjectInfo(String name, List<String> attributes, int seq) {
        super(attributes);
        _name = name.substring(name.lastIndexOf('.') + 1);
        _fullName = name;
        _seq = seq;
    }

    public void addMethod(MethodInfo mi) {
        if(LOGGER.isDebugEnabled())
            LOGGER.debug("addMethod(" + mi + ")");
        int possible = -1;
        for(int i = 0; i < _methods.size(); ++i) {
            MethodInfo otherMethod = _methods.get(i);
            if(otherMethod.getStartSeq() > mi.getStartSeq()) {
                possible = i;
                break;
            }
        }
        if(possible == -1)
            _methods.add(mi);
        else
            _methods.add(possible, mi);
    }

    public List<MethodInfo> getMethods() {
        return _methods;
    }

    public String getName() {
        return _name;
    }

    public String getFullName() {
        return _fullName;
    }

    public String getHtmlPackageName() {
        int index = _fullName.lastIndexOf('.');
        return index != -1 ? _fullName.substring(0, index) : "&lt;default&gt;";
    }

    public int getSeq() {
        return _seq;
    }

    public String toString() {
        return "Object " + _name + " seq " + _seq;
    }

    public int hashCode() {
        return _name.hashCode();
    }

    public boolean equals(Object o) {
        return _name.equals(((ObjectInfo)o)._name);
    }

    public boolean isActor() {
        return _name.equals(ACTOR_NAME);
    }
}
