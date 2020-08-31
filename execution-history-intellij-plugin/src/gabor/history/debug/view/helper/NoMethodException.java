package gabor.history.debug.view.helper;

public class NoMethodException extends RuntimeException {
    private static final long serialVersionUID = 6746519696840721191L;

    public NoMethodException(String message) {
        super(message);
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }
}
