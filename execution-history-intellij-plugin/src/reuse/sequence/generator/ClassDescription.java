package reuse.sequence.generator;

import reuse.sequence.Constants;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class ClassDescription  implements Serializable {
    private static final long serialVersionUID = 6663044955597028693L;
    private String _className;
    private List<String> _attributes;

    public ClassDescription() {
    }

    public ClassDescription(String className) {
        _className = className != null ? className : Constants.ANONYMOUS_CLASS_NAME;
        _attributes = new ArrayList<>();
    }

    public String getClassShortName() {
        return _className.substring(_className.lastIndexOf('.') + 1);
    }

    public String getClassName() {
        return _className;
    }

    public List<String> getAttributes() { return _attributes; }

    public String toString() {
        StringBuilder buffer = new StringBuilder();
        for (String attribute : _attributes) {
            buffer.append('|').append(attribute);
        }
        buffer.append("|@").append(_className);
        return buffer.toString();
    }

    public boolean equals(Object o) {
        if(this == o) return true;
        if(!(o instanceof ClassDescription)) return false;

        final ClassDescription classDescription = (ClassDescription)o;

        return _className.equals(classDescription._className);
    }

    public int hashCode() {
        return _className.hashCode();
    }

    public static ClassDescription ANONYMOUS_CLASS = new ClassDescription(Constants.ANONYMOUS_CLASS_NAME);
}
