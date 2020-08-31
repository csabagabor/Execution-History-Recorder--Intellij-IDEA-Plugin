package gabor.history.debug.type;

import java.io.Serializable;

public abstract class HistoryType implements Serializable {
    private static final long serialVersionUID = -1506814550929058542L;
    protected String name;

    public HistoryType(String name) {
        this.name = name;
    }

    public HistoryType() {
        //need for Kryon
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
