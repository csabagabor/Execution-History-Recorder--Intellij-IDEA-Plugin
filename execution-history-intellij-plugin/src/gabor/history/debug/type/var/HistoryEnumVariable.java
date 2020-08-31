package gabor.history.debug.type.var;

public class HistoryEnumVariable extends HistoryVar {
    private static final long serialVersionUID = 8073721353603643680L;

    public HistoryEnumVariable() {
        //used for Kryo, also don't forget to register for Kryo
    }

    public HistoryEnumVariable(String name, Object value) {
        super(name, value);
    }
}
