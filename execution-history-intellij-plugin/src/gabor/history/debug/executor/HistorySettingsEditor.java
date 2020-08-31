package gabor.history.debug.executor;

import com.intellij.execution.configurations.RunConfigurationBase;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.ComponentValidator;
import com.intellij.openapi.ui.ValidationInfo;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.ui.IdeBorderFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.classFilter.ClassFilter;
import com.intellij.ui.classFilter.ClassFilterEditor;
import com.intellij.util.ui.JBUI;
import gabor.history.helper.LoggingHelper;
import gabor.history.helper.PluginHelper;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.util.function.Supplier;

public class HistorySettingsEditor extends SettingsEditor<RunConfigurationBase> {
    private final JPanel panel = new JPanel(new GridBagLayout());
    //    protected JLabel minMethodSizeLabel = new JLabel("<html>Min. number of lines in an included <br> method (larger => faster)</html>");
    protected JLabel maxBreakpointsLabel = new JLabel("Number of max stack traces (larger => slower)");
    protected JLabel maxBreakpointPerMethodLabel = new JLabel("<html>Max number of occurrences of a single method <br> in stack trace (larger => slower)</html>");
    protected JLabel maxDepthVariablesLabel = new JLabel("<html>Depth of variables shown (larger => slower)</html>");
    protected JLabel nrCollectionItemsLabel = new JLabel("<html>Max. number of Collection/Array/Map elements to store <br> (larger => more memory)</html>");
    protected JLabel nrFieldsToShowLabel = new JLabel("<html>Max. number of fields in a Class to show <br> (larger => more memory)</html>");
    protected ClassFilterEditor inclusionClassFilterEditor;
    protected ClassFilterEditor exclusionClassFilterEditor;
    //    protected JTextField minMethodSizeField = new JTextField();
    protected JTextField maxBreakpointsField = new JTextField();
    protected JTextField maxBreakpointPerMethodField = new JTextField();
    protected JTextField maxDepthVariablesField = new JTextField();
    protected JTextField nrCollectionItemsField = new JTextField();
    protected JTextField nrFieldsToShowField = new JTextField();
    protected JRadioButton includeConstructors = new JRadioButton("Include constructors");
    protected JRadioButton includeSettersGetters = new JRadioButton("Include getters/setters/toString/equals/hashcode");
    //protected HyperlinkLabel tutorialLabel = new HyperlinkLabel("Tutorial");
    protected JLabel helpLabel = new JLabel("If nothing is included, then all the classes in the current project will be included (==> slow)!");
    public final String[] modes = new String[]{"Only covered lines (fastest)", "Sequence Diagram + stack traces (slower)",
            "Variable information (Experimental) (slowest) - Only Debug Mode"};
    protected JLabel modesLabel = new JLabel("Select the mode in which the plugin will work");
    public JComboBox<String> modesList = new ComboBox<>(modes);
    protected JRadioButton includeCollections = new JRadioButton("Include Collections/Maps in Variables Tab (=> slower + more memory)");

    public HistorySettingsEditor(Project project) {
        helpLabel.setForeground(JBColor.RED);
        modesLabel.setForeground(JBColor.RED);

        int y = 1;
        this.panel.add(helpLabel, new GridBagConstraints(0, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        //y++;
        //this.panel.add(tutorialLabel, new GridBagConstraints(0, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        y++;
        this.panel.add(includeConstructors, new GridBagConstraints(0, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        y++;
        this.panel.add(includeSettersGetters, new GridBagConstraints(0, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        y++;
        this.panel.add(modesLabel, new GridBagConstraints(0, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        this.panel.add(modesList, new GridBagConstraints(1, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
//        y++;
//        this.myMainPanel.add(minMethodSizeLabel, new GridBagConstraints(0, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
//        this.myMainPanel.add(minMethodSizeField, new GridBagConstraints(1, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        y++;
        this.panel.add(maxBreakpointsLabel, new GridBagConstraints(0, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        this.panel.add(maxBreakpointsField, new GridBagConstraints(1, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        y++;
        this.panel.add(maxBreakpointPerMethodLabel, new GridBagConstraints(0, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        this.panel.add(maxBreakpointPerMethodField, new GridBagConstraints(1, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        y++;
        this.panel.add(maxDepthVariablesLabel, new GridBagConstraints(0, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        this.panel.add(maxDepthVariablesField, new GridBagConstraints(1, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        y++;
        this.panel.add(includeCollections, new GridBagConstraints(0, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        y++;
        this.panel.add(nrCollectionItemsLabel, new GridBagConstraints(0, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        this.panel.add(nrCollectionItemsField, new GridBagConstraints(1, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));

        y++;
        this.panel.add(nrFieldsToShowLabel, new GridBagConstraints(0, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        this.panel.add(nrFieldsToShowField, new GridBagConstraints(1, y, 2, 1, 0.0D, 0.0D, 10, 1, JBUI.emptyInsets(), 0, 0));


        //this.tutorialLabel.setHyperlinkTarget(PluginHelper.TUTORIAL_URL);
        this.inclusionClassFilterEditor = new ClassFilterEditor(project);
        this.inclusionClassFilterEditor.setClassDelimiter(".");
        this.inclusionClassFilterEditor.setBorder(IdeBorderFactory.createTitledBorder("Include Patterns", false));
        this.panel.add(this.inclusionClassFilterEditor, new GridBagConstraints(0, 0, 1, 1, 1.0D, 1.0D, 10, 1, JBUI.emptyInsets(), 0, 0));
        this.exclusionClassFilterEditor = new ClassFilterEditor(project);
        this.exclusionClassFilterEditor.setClassDelimiter(".");
        this.exclusionClassFilterEditor.setBorder(IdeBorderFactory.createTitledBorder("Exclude Patterns", false));
        this.panel.add(this.exclusionClassFilterEditor, new GridBagConstraints(1, 0, 1, 1, 1.0D, 1.0D, 10, 1, JBUI.emptyInsets(), 0, 0));


//        new ComponentValidator(project).withValidator(new NumberValidator(minMethodSizeField)).installOn(minMethodSizeField);
        new ComponentValidator(project).withValidator(new NumberValidator(maxBreakpointPerMethodField)).installOn(maxBreakpointPerMethodField);
        new ComponentValidator(project).withValidator(new NumberValidator(maxBreakpointsField)).installOn(maxBreakpointsField);
        new ComponentValidator(project).withValidator(new NumberValidator(maxDepthVariablesField)).installOn(maxDepthVariablesField);
        new ComponentValidator(project).withValidator(new NumberValidator(nrCollectionItemsField)).installOn(nrCollectionItemsField);
        new ComponentValidator(project).withValidator(new NumberValidator(nrFieldsToShowField)).installOn(nrFieldsToShowField);
    }

    private static class NumberValidator implements Supplier<ValidationInfo> {
        private final JTextField textField;

        public NumberValidator(JTextField textField) {
            this.textField = textField;
        }

        @Override
        public ValidationInfo get() {
            String text = textField.getText();
            try {
                int i = Integer.parseInt(text);
                if (i < 1) {
                    return new ValidationInfo("Must be >= 1", textField);
                }
            } catch (NumberFormatException e) {
                return new ValidationInfo("Not a Number", textField);
            }
            return null;
        }
    }

    @Override
    protected void resetEditorFrom(@NotNull RunConfigurationBase runConfigurationBase) {
        HistoryConfigurationSettingsStore store = HistoryConfigurationSettingsStore.getByConfiguration(runConfigurationBase);
        this.inclusionClassFilterEditor.setFilters(store.includeFilters);
        this.exclusionClassFilterEditor.setFilters(store.excludeFilters);

        this.includeSettersGetters.setSelected(store.includeGettersSetters);
        this.includeConstructors.setSelected(store.includeConstructors);
//        this.minMethodSizeField.setText(String.valueOf(store.minMethodSize));
        this.maxBreakpointPerMethodField.setText(String.valueOf(store.maxBreakpointsPerMethod));
        this.maxBreakpointsField.setText(String.valueOf(store.maxBreakpoints));
        this.maxDepthVariablesField.setText(String.valueOf(store.maxDepthVariables));
        this.nrCollectionItemsField.setText(String.valueOf(store.nrCollectionItems));
        this.nrFieldsToShowField.setText(String.valueOf(store.nrFieldsToShow));
        this.includeCollections.setSelected(store.includeCollections);

        //hide options not related
        showOrShowExtraVariableOptions(store.mode == 2);

        //hide options not related
        showOrShowExtraBreakpointOptions(store.mode != 0);

        if (store.mode < modes.length) {
            this.modesList.setSelectedItem(modes[store.mode]);
        } else {
            this.modesList.setSelectedItem(modes[1]);
        }
    }


    @Override
    protected void applyEditorTo(@NotNull RunConfigurationBase runConfigurationBase) {
        HistoryConfigurationSettingsStore store = HistoryConfigurationSettingsStore.getByConfiguration(runConfigurationBase);
        store.includeFilters = this.inclusionClassFilterEditor.getFilters();
        store.excludeFilters = this.exclusionClassFilterEditor.getFilters();

        applyDebuggingHack(this.exclusionClassFilterEditor.getFilters());

        store.includeGettersSetters = this.includeSettersGetters.isSelected();
        store.includeConstructors = this.includeConstructors.isSelected();
        store.includeCollections = this.includeCollections.isSelected();
//        try {
//            ComponentValidator.getInstance(minMethodSizeField).ifPresent(ComponentValidator::revalidate);
//            store.minMethodSize = Integer.parseInt(this.minMethodSizeField.getText());
//        } catch (NumberFormatException e) {
//        }

        try {
            ComponentValidator.getInstance(maxBreakpointPerMethodField).ifPresent(ComponentValidator::revalidate);
            int nr = Integer.parseInt(this.maxBreakpointPerMethodField.getText());
            if (nr > 0) {
                store.maxBreakpointsPerMethod = nr;
            }
        } catch (NumberFormatException e) {
        }

        try {
            ComponentValidator.getInstance(maxBreakpointsField).ifPresent(ComponentValidator::revalidate);
            int nr = Integer.parseInt(this.maxBreakpointsField.getText());
            if (nr > 0) {
                store.maxBreakpoints = nr;
            }
        } catch (NumberFormatException e) {
        }

        try {
            ComponentValidator.getInstance(maxDepthVariablesField).ifPresent(ComponentValidator::revalidate);
            int nr = Integer.parseInt(this.maxDepthVariablesField.getText());
            if (nr > 0) {
                store.maxDepthVariables = nr;
            }
        } catch (NumberFormatException e) {
        }

        try {
            ComponentValidator.getInstance(nrCollectionItemsField).ifPresent(ComponentValidator::revalidate);
            int nr = Integer.parseInt(this.nrCollectionItemsField.getText());
            if (nr > 0) {
                store.nrCollectionItems = nr;
            }
        } catch (NumberFormatException e) {
        }

        try {
            ComponentValidator.getInstance(nrFieldsToShowField).ifPresent(ComponentValidator::revalidate);
            int nr = Integer.parseInt(this.nrFieldsToShowField.getText());
            if (nr > 0) {
                store.nrFieldsToShow = nr;
            }
        } catch (NumberFormatException e) {
        }

        String selectedItem = (String) this.modesList.getSelectedItem();

        for (int i = 0; i < modes.length; i++) {
            if (modes[i].equals(selectedItem)) {
                store.mode = i;

                //hide options not related
                showOrShowExtraVariableOptions(i == 2);

                //hide options not related
                showOrShowExtraBreakpointOptions(i != 0);
            }
        }
    }

    private void applyDebuggingHack(ClassFilter[] filters) {
        try {
            boolean enableLogging = false;
            if (filters != null) {
                for (ClassFilter filter : filters) {
                    String pattern = filter.PATTERN;
                    if ("HISTORY_MAGIC".equals(pattern)) {
                        enableLogging = true;
                        break;
                    }
                }
            }

            if (enableLogging) {
                LoggingHelper.enable();
            } else {
                LoggingHelper.disable();
            }
        } catch (Throwable e) {
            LoggingHelper.disable();
        }
    }

    private void showOrShowExtraBreakpointOptions(boolean show) {
//        minMethodSizeField.setVisible(show);
        maxBreakpointPerMethodField.setVisible(show);
        maxBreakpointsField.setVisible(show);

//        minMethodSizeLabel.setVisible(show);
        maxBreakpointPerMethodLabel.setVisible(show);
        maxBreakpointsLabel.setVisible(show);
    }

    private void showOrShowExtraVariableOptions(boolean show) {
        maxDepthVariablesField.setVisible(show);
        maxDepthVariablesLabel.setVisible(show);
        includeCollections.setVisible(show);
        nrCollectionItemsField.setVisible(show);
        nrCollectionItemsLabel.setVisible(show);
        nrFieldsToShowField.setVisible(show);
        nrFieldsToShowLabel.setVisible(show);
    }

    @Override
    @NotNull
    protected JComponent createEditor() {
        return this.panel;
    }
}
