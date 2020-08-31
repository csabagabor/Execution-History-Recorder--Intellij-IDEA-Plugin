

package gabor.history.debug.type.var;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;


public abstract class HistoryVar implements Serializable {
    private static final long serialVersionUID = -4336572190658649452L;
    private String name;
    private Object value;
    private List<HistoryVar> fieldVariables = new ArrayList<>();
    private int size = -1;//applicable only to collections, but for simpler coding, it is implemented here

    public HistoryVar() {
        //used for Kryo, also don't forget to register for Kryo
    }

    public HistoryVar(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return this.name;
    }

    public Object getValue() {
        return value;
    }

    public List<HistoryVar> getFieldVariables() {
        return fieldVariables;
    }

    public void setFieldVariables(List<HistoryVar> fieldVariables) {
        this.fieldVariables = fieldVariables;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public int getSize() {
        return size;
    }
}
