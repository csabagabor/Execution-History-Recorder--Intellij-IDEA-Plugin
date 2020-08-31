package gabor.history.debug.type.var;

public class HistoryLocalVariable extends HistoryVar {
    private static final long serialVersionUID = 9031604289296837650L;

    public HistoryLocalVariable() {
        //used for Kryo, also don't forget to register for Kryo
    }

    public HistoryLocalVariable(String name, Object value) {
        super(name, value);
    }
}
