package reuse.sequence.generator;

import reuse.sequence.generator.filters.PackageFilter;
import reuse.sequence.generator.filters.SingleClassFilter;
import reuse.sequence.config.Configuration;
import reuse.sequence.config.ExcludeEntry;
import reuse.sequence.generator.filters.CompositeMethodFilter;
import reuse.sequence.generator.filters.InterfaceImplFilter;

import java.util.List;

public class SequenceParams {
    private static final String PACKAGE_INDICATOR = ".*";
    private static final String RECURSIVE_PACKAGE_INDICATOR = ".**";

    private int _maxDepth = 3;
    private boolean _allowRecursion = true;
    private boolean smartInterface = true;
    private CompositeMethodFilter _methodFilter = new CompositeMethodFilter();
    private InterfaceImplFilter _implFilter = new InterfaceImplFilter();

    public SequenceParams() {
        List<ExcludeEntry> excludeList = Configuration.getInstance().getExcludeList();
        for (ExcludeEntry excludeEntry : excludeList) {
            if (!excludeEntry.isEnabled())
                continue;
            String excludeName = excludeEntry.getExcludeName();
            if (excludeName.endsWith(PACKAGE_INDICATOR)) {
                int index = excludeName.lastIndexOf(PACKAGE_INDICATOR);
                _methodFilter.addFilter(new PackageFilter(excludeName.substring(0, index)));
            } else if (excludeName.endsWith(RECURSIVE_PACKAGE_INDICATOR)) {
                int index = excludeName.lastIndexOf(RECURSIVE_PACKAGE_INDICATOR);
                _methodFilter.addFilter(new PackageFilter(excludeName.substring(0, index), true));
            } else
                _methodFilter.addFilter(new SingleClassFilter(excludeName));
        }
    }

    public int getMaxDepth() {
        return _maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this._maxDepth = maxDepth;
    }

    public boolean isNotAllowRecursion() {
        return !_allowRecursion;
    }

    public void setAllowRecursion(boolean allowRecursion) {
        this._allowRecursion = allowRecursion;
    }

    public boolean isSmartInterface() {
        return smartInterface;
    }

    public void setSmartInterface(boolean smartInterface) {
        this.smartInterface = smartInterface;
    }

    public CompositeMethodFilter getMethodFilter() {
        return _methodFilter;
    }

    public InterfaceImplFilter getInterfaceImplFilter() {
        return _implFilter;
    }
}

