
package gabor.history.debug.type;


public class ComplexType extends HistoryType {
    private static final long serialVersionUID = 2640299260375934033L;

    public ComplexType(String name, long hashCode) {
        super(name + "@" + hashCode);
    }

    public ComplexType(String name, int hashCode) {
        super(name + "@" + hashCode);
    }

    public ComplexType() {
        //need for Kryon
    }
}
