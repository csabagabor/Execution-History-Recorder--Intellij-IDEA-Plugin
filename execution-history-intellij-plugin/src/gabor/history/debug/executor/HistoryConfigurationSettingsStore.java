package gabor.history.debug.executor;

import com.intellij.debugger.impl.DebuggerUtilsEx;
import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.util.InvalidDataException;
import com.intellij.openapi.util.JDOMExternalizable;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.WriteExternalException;
import com.intellij.ui.classFilter.ClassFilter;
import gabor.history.debug.DebugResources;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;


public class HistoryConfigurationSettingsStore implements JDOMExternalizable {
    public static final Key<HistoryConfigurationSettingsStore> KEY = Key.create(DebugResources.SETTINGS_KEY);
    public static final String INCLUDE_FILTER = "includeHistoryFilter";
    public static final String EXCLUDE_FILTER = "excludeHistoryFilter";
    public ClassFilter[] includeFilters = ClassFilter.EMPTY_ARRAY;
    public ClassFilter[] excludeFilters = ClassFilter.EMPTY_ARRAY;
    public Boolean includeGettersSetters = false;
    public Boolean includeConstructors = false;
    public Integer minMethodSize = 2;
    public Integer maxBreakpointsPerMethod = 5;
    public Integer maxBreakpoints = 200;
    public Integer maxDepthVariables = 3;//3 means to show a Person/Pet object properties inside this.pets, where this.pets is a Collection
    public Integer mode = 1;
    public boolean includeCollections = true;
    public int nrCollectionItems = 5;//nr of collection/map/array items to store
    public int nrFieldsToShow = 10;

    @NotNull
    public static HistoryConfigurationSettingsStore getByConfiguration(RunConfigurationBase<HistoryConfigurationSettingsStore> configuration) {
        HistoryConfigurationSettingsStore store = configuration.getCopyableUserData(KEY);
        if (store == null) {
            store = new HistoryConfigurationSettingsStore();
            configuration.putCopyableUserData(KEY, store);
        }

        return store;
    }

    @Override
    public void readExternal(Element element) throws InvalidDataException {
        this.includeFilters = DebuggerUtilsEx.readFilters(element.getChildren(INCLUDE_FILTER));
        this.excludeFilters = DebuggerUtilsEx.readFilters(element.getChildren(EXCLUDE_FILTER));

        includeConstructors = Boolean.valueOf(element.getAttributeValue("includeConstructors"));
        includeGettersSetters = Boolean.valueOf(element.getAttributeValue("includeGettersSetters"));
        minMethodSize = Integer.valueOf(element.getAttributeValue("minMethodSize"));
        maxBreakpointsPerMethod = Integer.valueOf(element.getAttributeValue("maxBreakpointsPerMethod"));
        maxBreakpoints = Integer.valueOf(element.getAttributeValue("maxBreakpoints"));
        maxDepthVariables = Integer.valueOf(element.getAttributeValue("maxDepthVariables"));
        mode = Integer.valueOf(element.getAttributeValue("mode"));
        nrCollectionItems = Integer.parseInt(element.getAttributeValue("nrCollectionItems"));
        nrFieldsToShow = Integer.parseInt(element.getAttributeValue("nrFieldsToShow"));
        includeCollections = Boolean.parseBoolean(element.getAttributeValue("includeCollections"));
    }

    @Override
    public void writeExternal(Element element) throws WriteExternalException {
        DebuggerUtilsEx.writeFilters(element, INCLUDE_FILTER, this.includeFilters);
        DebuggerUtilsEx.writeFilters(element, EXCLUDE_FILTER, this.excludeFilters);

        element.setAttribute("includeConstructors", String.valueOf(includeConstructors));
        element.setAttribute("includeGettersSetters", String.valueOf(includeGettersSetters));
        element.setAttribute("minMethodSize", String.valueOf(minMethodSize));
        element.setAttribute("maxBreakpointsPerMethod", String.valueOf(maxBreakpointsPerMethod));
        element.setAttribute("maxBreakpoints", String.valueOf(maxBreakpoints));
        element.setAttribute("maxDepthVariables", String.valueOf(maxDepthVariables));
        element.setAttribute("mode", String.valueOf(mode));
        element.setAttribute("nrCollectionItems", String.valueOf(nrCollectionItems));
        element.setAttribute("includeCollections", String.valueOf(includeCollections));
        element.setAttribute("nrFieldsToShow", String.valueOf(nrFieldsToShow));
    }
}
