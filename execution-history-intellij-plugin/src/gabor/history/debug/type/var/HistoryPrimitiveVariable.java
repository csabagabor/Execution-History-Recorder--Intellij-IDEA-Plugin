package gabor.history.debug.type.var;

public class HistoryPrimitiveVariable extends HistoryVar {
    private static final long serialVersionUID = -975780430183153668L;

    public HistoryPrimitiveVariable() {
        //used for Kryo, also don't forget to register for Kryo
    }

    public HistoryPrimitiveVariable(String name, Object value) {
        super(name, value);
    }
}
