package gabor.history.debug.type.var;

public class HistoryEntryVariable extends HistoryVar {
    private static final long serialVersionUID = -9195421990135649173L;

    public HistoryEntryVariable() {
        //used for Kryo, also don't forget to register for Kryo
    }

    public HistoryEntryVariable(String name, Object value) {
        super(name, value);
    }
}
