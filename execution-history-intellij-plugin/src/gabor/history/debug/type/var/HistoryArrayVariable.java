package gabor.history.debug.type.var;

public class HistoryArrayVariable extends HistoryVar {
    private static final long serialVersionUID = 4733863834842878066L;

    public HistoryArrayVariable() {
        //used for Kryo, also don't forget to register for Kryo
    }

    public HistoryArrayVariable(String name, Object value) {
        super(name, value);
    }
}
