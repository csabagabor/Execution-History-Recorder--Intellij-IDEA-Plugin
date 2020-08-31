package reuse.sequence;

import gabor.history.helper.SequenceAdapter;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.psi.*;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBScrollBar;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import reuse.sequence.generator.filters.SingleClassFilter;
import gabor.history.helper.LoggingHelper;
import reuse.sequence.diagram.*;
import reuse.sequence.generator.*;
import icons.SequencePluginIcons;
import reuse.sequence.ui.MyButtonlessScrollBarUI;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.*;

public class SequencePanel extends JPanel {
    public static final String ALL_CLASSES = "ALL_CLASSES";
    public static final String FILTER_SEARCH_PANEL_TITLE = "Include only stack traces which contain selected classes";
    //private static final Logger LOGGER = Logger.getInstance(SequencePanel.class.getName());

    private final Display _display;
    private final Model _model;
    private final SequenceService _plugin;
    private final SequenceParams _sequenceParams;
    private final JavaPsiFacade psiFacade;
    private PsiMethod _psiMethod;
    private CallStack callStack;
    private Project project;
    private String _titleName;
    private final JScrollPane _jScrollPane;
    private final Map<Integer, CallStack> callStackMap = new HashMap<>();
    private int _callDepth = 50;
    private boolean _noGetterSetters = true;
    private boolean _noConstructors = true;
    private boolean _noPrivateMethods = false;
    private Set<String> filteredClasses = new HashSet<>();
    private long MAX_CALLS = 1000;//else sequence diagram becomes unresponisve

    private Set<String> allClasses = new HashSet<>();
    private boolean FILTER_BLOCKED;

    public SequencePanel(SequenceService plugin, CallStack callStack, SequenceParams sequenceParams, Project project) {
        super(new BorderLayout());
        this.psiFacade = JavaPsiFacade.getInstance(project);
        _plugin = plugin;
        this.callStack = callStack;

        long totalCalls = calcAllClasses(callStack, true);

        if (totalCalls > MAX_CALLS) {

            int resp = JOptionPane.showConfirmDialog(null, "Please provide some filtering options to lower the number of calls down to " + MAX_CALLS + ". Currently there are "
                    + totalCalls + " calls", "Show Sequence Diagram", JOptionPane.OK_CANCEL_OPTION);

            if (resp == JOptionPane.CANCEL_OPTION) {
                throw new RuntimeException("User cancelled after too large call stack");
            }

            new FilterAction().showDialog(false);

            totalCalls = calcAllClasses(callStack, true);
            if (totalCalls > MAX_CALLS) {
                throw new RuntimeException("User cancelled after too large call stack");
            }

        }

        this.project = project;
        createCallStackMap(callStack);

        _sequenceParams = sequenceParams;

        _model = new Model();
        _display = new Display(_model, new SequenceListenerImpl());

        DefaultActionGroup actionGroup = new DefaultActionGroup("SequencerActionGroup", false);
        actionGroup.add(new ReGenerateAction());
        actionGroup.add(new ExportAction());
        actionGroup.add(new ExportTextAction());
        actionGroup.add(new SearchAction());
        actionGroup.add(new FilterAction());

        ActionManager actionManager = ActionManager.getInstance();
        ActionToolbar actionToolbar = actionManager.createActionToolbar("SequencerToolbar", actionGroup, false);
        add(actionToolbar.getComponent(), BorderLayout.WEST);

        MyButton birdViewButton = new MyButton(SequencePluginIcons.PREVIEW_ICON_13);
        birdViewButton.setToolTipText("Bird view");
        birdViewButton.addActionListener(e -> showBirdView());

        _jScrollPane = new JBScrollPane(_display);
        _jScrollPane.setVerticalScrollBar(new MyScrollBar(Adjustable.VERTICAL));
        _jScrollPane.setHorizontalScrollBar(new MyScrollBar(Adjustable.HORIZONTAL));
        _jScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        _jScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        _jScrollPane.setCorner(JScrollPane.LOWER_RIGHT_CORNER, birdViewButton);
        add(_jScrollPane, BorderLayout.CENTER);
    }

    private long calcAllClasses(CallStack callStack, boolean firstTime) {

        if (callStack == null) {
            return 0;
        }

        MethodDescription method = callStack.getMethod();
        if (method != null) {
            ClassDescription classDescription = method.getClassDescription();

            if (classDescription != null) {
                String className = classDescription.getClassName();

                if (className != null) {

                    if (firstTime) {
                        allClasses.add(className);
                    }
                }
            }
        }

        List<CallStack> calls = callStack.getCalls();

        long sum = 0;
        if (calls != null) {
            for (CallStack call : calls) {
                sum += calcAllClasses(call, firstTime);
            }
        }

        if (callStack.isFiltered()) {
            return sum;
        } else {
            return sum + 1;
        }
    }

    //ADDED CODE
    //recursively add callstacks to map
    private void createCallStackMap(CallStack callStack) {
        callStackMap.put(callStack.hashCode(), callStack);

        List<CallStack> calls = callStack.getCalls();
        if (calls != null) {
            for (CallStack call : calls) {
                createCallStackMap(call);
            }
        }
    }
    //ADDED CODE

    public Model getModel() {
        return _model;
    }

    private void generate(String query) {
//        LOGGER.debug("sequence = " + query);
        _model.setText(query, this);
        _display.invalidate();
    }

    public void generate() {
        SequenceGenerator generator = new SequenceGenerator(_sequenceParams);
//        final CallStack callStack = generator.generate(_psiMethod);

        _titleName = "History Debugger";
        generate(callStack.generateSequence());
    }

    public void generateTextFile(File selectedFile) throws IOException {
        if (_psiMethod == null || !_psiMethod.isValid()) { // || !_psiMethod.isPhysical()
            _psiMethod = null;
            return;
        }
        SequenceGenerator generator = new SequenceGenerator(_sequenceParams);
        final CallStack callStack = generator.generate(_psiMethod);

        Files.write(selectedFile.toPath(),
                callStack.generateText().getBytes(),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);

    }

    private void showBirdView() {
        PreviewFrame frame = new PreviewFrame(_jScrollPane, _display);
        frame.setVisible(true);
    }

    public String getTitleName() {
        return _titleName;
    }

    private void gotoSourceCode(ScreenObject screenObject) {
        if (screenObject instanceof DisplayObject) {
            DisplayObject displayObject = (DisplayObject) screenObject;
            gotoClass(displayObject.getObjectInfo());
        } else if (screenObject instanceof DisplayMethod) {
            DisplayMethod displayMethod = (DisplayMethod) screenObject;

            DisplayLink call = displayMethod.getCall();
            if (call != null) {
                int hashCodeOfCallstack = call.getLink().getHashCodeOfCallstack();

                if (hashCodeOfCallstack != 0) {
                    gotoMethodLast(hashCodeOfCallstack);
                } else {
                    gotoMethod(displayMethod.getMethodInfo());
                }
            } else {
                gotoMethod(displayMethod.getMethodInfo());
            }

        } else if (screenObject instanceof DisplayLink) {
            DisplayLink displayLink = (DisplayLink) screenObject;
            gotoCall(displayLink.getLink().getCallerMethodInfo(),
                    displayLink.getLink().getMethodInfo(), displayLink.getLink().getHashCodeOfCallstack());
        }
    }

    private void gotoClass(ObjectInfo objectInfo) {
        _plugin.openClassInEditor(objectInfo.getFullName());
    }

    private void gotoMethod(MethodInfo methodInfo) {
        String className = methodInfo.getObjectInfo().getFullName();
        String methodName = methodInfo.getRealName();
        List<String> argTypes = methodInfo.getArgTypes();

        _plugin.openMethodInEditor(className, methodName, argTypes);
    }

    private void gotoMethodLast(int hashCodeOfCallstack) {
        CallStack callStack = callStackMap.get(hashCodeOfCallstack);
        SequenceAdapter.showVariableView(project, callStack, callStack);
    }

    private void gotoMethod(MethodInfo methodInfo, int hashCodeOfCallstack) {
        CallStack callStack = callStackMap.get(hashCodeOfCallstack);

        SequenceAdapter.showVariableView(project, callStack, callStack);
    }

    private void gotoCall(MethodInfo fromMethodInfo, MethodInfo toMethodInfo, int hashCodeOfCallstack) {
        if (toMethodInfo == null) {
            return;
        }

        CallStack callStack = callStackMap.get(hashCodeOfCallstack);
        // Only first call from Actor, the fromMethodInfo is null
        if (fromMethodInfo == null) {
            gotoMethod(toMethodInfo, hashCodeOfCallstack);
            return;
        }

        if (isLambdaCall(toMethodInfo)) {
            _plugin.openLambdaExprInEditor(
                    fromMethodInfo.getObjectInfo().getFullName(),
                    fromMethodInfo.getRealName(),
                    fromMethodInfo.getArgTypes(),
                    toMethodInfo.getArgTypes(),
                    toMethodInfo.getReturnType()
            );
        } else if (isLambdaCall(fromMethodInfo)) {
            LambdaExprInfo lambdaExprInfo = (LambdaExprInfo) fromMethodInfo;
            _plugin.openStackFrameInsideLambdaExprInEditor(
                    _sequenceParams.getMethodFilter(),
                    lambdaExprInfo.getObjectInfo().getFullName(),
                    lambdaExprInfo.getEnclosedMethodName(),
                    lambdaExprInfo.getEnclosedMethodArgTypes(),
                    lambdaExprInfo.getArgTypes(),
                    lambdaExprInfo.getReturnType(),
                    toMethodInfo.getObjectInfo().getFullName(),
                    toMethodInfo.getRealName(),
                    toMethodInfo.getArgTypes(),
                    toMethodInfo.getNumbering().getTopLevel()
            );
        } else if (fromMethodInfo.getObjectInfo().hasAttribute(Info.INTERFACE_ATTRIBUTE) && fromMethodInfo.hasAttribute(Info.ABSTRACT_ATTRIBUTE)) {
            gotoMethod(toMethodInfo, hashCodeOfCallstack);
        } else {
//            _plugin.openStackFrameInEditor(
//                    _sequenceParams.getMethodFilter(),
//                    fromMethodInfo.getObjectInfo().getFullName(),
//                    fromMethodInfo.getRealName(),
//                    fromMethodInfo.getArgTypes(),
//                    toMethodInfo.getObjectInfo().getFullName(),
//                    toMethodInfo.getRealName(),
//                    toMethodInfo.getArgTypes(),
//                    toMethodInfo.getNumbering().getTopLevel(),
//                    callStack.getMethod().getLine());
        }

        SequenceAdapter.showVariableView(project, callStack.getParent(), callStack);
    }

    private boolean isLambdaCall(MethodInfo methodInfo) {
        return Objects.equals(methodInfo.getRealName(), Constants.Lambda_Invoke);
    }


    private class ReGenerateAction extends AnAction {
        public ReGenerateAction() {
            super("ReGenerate", "Regenerate diagram", SequencePluginIcons.REFRESH_ICON);
        }

        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            generate();


        }
    }

    private class ExportAction extends AnAction {
        public ExportAction() {
            super("Export", "Export image to file", SequencePluginIcons.EXPORT_ICON);
        }

        public void actionPerformed(@NotNull AnActionEvent event) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.setFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().endsWith("png");
                }

                public String getDescription() {
                    return "PNG Images";
                }
            });
            try {
                if (fileChooser.showSaveDialog(SequencePanel.this) == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (!selectedFile.getName().endsWith("png"))
                        selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".png");
                    _display.saveImageToFile(selectedFile);
                }
            } catch (Throwable e) {
                LoggingHelper.error(e);
                JOptionPane.showMessageDialog(SequencePanel.this, e.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class ExportTextAction extends AnAction {

        public ExportTextAction() {
            super("ExportTextFile", "Export call stack as text file", SequencePluginIcons.EXPORT_TEXT_ICON);
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogType(JFileChooser.SAVE_DIALOG);
            fileChooser.setFileFilter(new FileFilter() {
                public boolean accept(File f) {
                    return f.isDirectory() || f.getName().endsWith("txt");
                }

                public String getDescription() {
                    return "Text File";
                }
            });
            try {
                if (fileChooser.showSaveDialog(SequencePanel.this) == JFileChooser.APPROVE_OPTION) {
                    File selectedFile = fileChooser.getSelectedFile();
                    if (!selectedFile.getName().endsWith("txt"))
                        selectedFile = new File(selectedFile.getParentFile(), selectedFile.getName() + ".txt");

                    generateTextFile(selectedFile);
                }
            } catch (Throwable e) {
                LoggingHelper.error(e);
                JOptionPane.showMessageDialog(SequencePanel.this, e.getMessage(), "Exception", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private class GotoSourceAction extends AnAction {
        private final ScreenObject _screenObject;

        public GotoSourceAction(ScreenObject screenObject) {
            super("Go to Source");
            _screenObject = screenObject;
        }

        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            gotoSourceCode(_screenObject);
        }
    }

    private class RemoveClassAction extends AnAction {
        private final ObjectInfo _objectInfo;

        public RemoveClassAction(ObjectInfo objectInfo) {
            super("Remove Class '" + objectInfo.getName() + "'");
            _objectInfo = objectInfo;
        }

        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            _sequenceParams.getMethodFilter().addFilter(new SingleClassFilter(_objectInfo.getFullName()));
            generate();
        }
    }

//    private class RemoveMethodAction extends AnAction {
//        private final MethodInfo _methodInfo;
//
//        public RemoveMethodAction(MethodInfo methodInfo) {
//            super("Remove Method '" + methodInfo.getRealName() + "()'");
//            _methodInfo = methodInfo;
//        }
//
//        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
//            _sequenceParams.getMethodFilter().addFilter(new SingleMethodFilter(
//                    _methodInfo.getObjectInfo().getFullName(),
//                    _methodInfo.getRealName(),
//                    _methodInfo.getArgTypes()
//            ));
//            generate();
//
//        }
//    }
//
//    private class ExpendInterfaceAction extends AnAction {
//        private final String face;
//        private final String impl;
//
//        public ExpendInterfaceAction(String face, String impl) {
//            super(impl);
//            this.face = face;
//            this.impl = impl;
//        }
//
//        @Override
//        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
//            _sequenceParams.getInterfaceImplFilter().put(
//                    face,
//                    new ImplementClassFilter(impl)
//            );
//            generate();
//        }
//    }

    private class SequenceListenerImpl implements SequenceListener {

        public void selectedScreenObject(ScreenObject screenObject) {
            gotoSourceCode(screenObject);
        }

        public void displayMenuForScreenObject(ScreenObject screenObject, int x, int y) {
            DefaultActionGroup actionGroup = new DefaultActionGroup("SequencePopup", true);
            actionGroup.add(new GotoSourceAction(screenObject));
//            if (screenObject instanceof DisplayObject) {
//                DisplayObject displayObject = (DisplayObject) screenObject;
//                if (displayObject.getObjectInfo().hasAttribute(Info.INTERFACE_ATTRIBUTE) && !_sequenceParams.isSmartInterface()) {
//                    String className = displayObject.getObjectInfo().getFullName();
//                    List<String> impls = _plugin.findImplementations(className);
//                    actionGroup.addSeparator();
//                    for (String impl : impls) {
//                        actionGroup.add(new ExpendInterfaceAction(className, impl));
//                    }
//                    actionGroup.addSeparator();
//                }
//                actionGroup.add(new RemoveClassAction(displayObject.getObjectInfo()));
//            } else if (screenObject instanceof DisplayMethod) {
//                DisplayMethod displayMethod = (DisplayMethod) screenObject;
//                if (displayMethod.getObjectInfo().hasAttribute(Info.INTERFACE_ATTRIBUTE) && !_sequenceParams.isSmartInterface()) {
//
//                    String className = displayMethod.getObjectInfo().getFullName();
//                    String methodName = displayMethod.getMethodInfo().getRealName();
//                    List<String> argTypes = displayMethod.getMethodInfo().getArgTypes();
//                    List<String> impls = _plugin.findImplementations(className, methodName, argTypes);
//
//                    actionGroup.addSeparator();
//                    for (String impl : impls) {
//                        actionGroup.add(new ExpendInterfaceAction(className, impl));
//                    }
//                    actionGroup.addSeparator();
//
//                }
//                actionGroup.add(new RemoveMethodAction(displayMethod.getMethodInfo()));
//            } else if (screenObject instanceof DisplayLink) {
//                DisplayLink displayLink = (DisplayLink) screenObject;
//                if (!displayLink.isReturnLink())
//                    actionGroup.add(new RemoveMethodAction(displayLink.getLink().getMethodInfo()));
//            }
            ActionPopupMenu actionPopupMenu = ActionManager.getInstance().
                    createActionPopupMenu("SequenceDiagram.Popup", actionGroup);
            Component invoker = screenObject instanceof DisplayObject ? _display.getHeader() : _display;
            actionPopupMenu.getComponent().show(invoker, x, y);
        }
    }

    private static class MyScrollBar extends JBScrollBar {
        public MyScrollBar(int orientation) {
            super(orientation);
        }

        @Override
        public void updateUI() {
            setUI(MyButtonlessScrollBarUI.createNormal());
        }


    }

    private static class MyButton extends JButton {

        public MyButton(Icon icon) {
            super(icon);
            init();
        }

        private void init() {
            setUI(new BasicButtonUI());
            setBackground(UIUtil.getLabelBackground());
//            setContentAreaFilled(false);
            setBorder(BorderFactory.createEmptyBorder());
            setBorderPainted(false);
            setFocusable(false);
            setRequestFocusEnabled(false);
        }

        @Override
        public void updateUI() {
            init();
        }
    }


    private class DialogPanel extends JPanel {
        private JSpinner jSpinner;
        private JCheckBox jCheckBoxNGS;
        private JCheckBox jCheckBoxNC;
        private JCheckBox jCheckBoxNPM;

        public DialogPanel() {
            super(new GridBagLayout());
            setBorder(BorderFactory.createTitledBorder("Filter"));
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridx = 0;
            gc.gridy = 0;
            gc.insets = JBUI.insets(5);
            gc.anchor = GridBagConstraints.WEST;
            gc.gridwidth = 2;
            JLabel jLabel = new JLabel("Max Call depth:");
            add(jLabel, gc);

            gc.gridx = 2;
            gc.gridy = 0;
            gc.anchor = GridBagConstraints.CENTER;
            jSpinner = new JSpinner(new SpinnerNumberModel(_callDepth, 1, 1000, 1));
            jLabel.setLabelFor(jSpinner);
            add(jSpinner, gc);

//            gc.gridx = 0;
//            gc.gridy = 2;
//            gc.anchor = GridBagConstraints.WEST;
//            gc.gridwidth = 2;
//            gc.insets = JBUI.emptyInsets();
//            jCheckBoxNGS = new JCheckBox("Skip getters/setters", _noGetterSetters);
//            add(jCheckBoxNGS, gc);
//
//            gc.gridx = 2;
//            gc.gridy = 2;
//            gc.anchor = GridBagConstraints.WEST;
//            gc.gridwidth = 2;
//            gc.insets = JBUI.emptyInsets();
//            jCheckBoxNC = new JCheckBox("Skip constructors", _noConstructors);
//            add(jCheckBoxNC, gc);

            gc.gridx = 0;
            gc.gridy = 2;
            gc.anchor = GridBagConstraints.WEST;
            gc.gridwidth = 2;
            gc.insets = JBUI.emptyInsets();
            jCheckBoxNPM = new JCheckBox("Skip private methods", _noPrivateMethods);
            add(jCheckBoxNPM, gc);
        }
    }

    private class OptionsDialogWrapper extends DialogWrapper {
        private JPanel southPanel = new JPanel();
        private final DialogPanel dialogPanel = new DialogPanel();
        private final SearchPanel searchPanel = new SearchPanel(new JPanel(), allClasses, FILTER_SEARCH_PANEL_TITLE);
        private final SearchButtonPanel searchButtonPanel = new SearchButtonPanel(searchPanel);

        public OptionsDialogWrapper(Project project) {
            super(project, false);

            southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
            southPanel.add(searchButtonPanel);
            southPanel.add(searchPanel);
            setResizable(false);
            setTitle("Sequence Diagram Options");
            init();
        }

        @Override
        protected JComponent createCenterPanel() {
            return southPanel;
        }

        @Nullable
        @Override
        protected JComponent createNorthPanel() {
            return dialogPanel;
        }

        public JComponent getPreferredFocusedComponent() {
            return dialogPanel.jSpinner;
        }

        public int getCallStackDepth() {
            return (Integer) dialogPanel.jSpinner.getValue();
        }

        public boolean isNoGetterSetters() {
            return dialogPanel.jCheckBoxNGS.isSelected();
        }

        public boolean isNoConstructors() {
            return dialogPanel.jCheckBoxNC.isSelected();
        }

        public boolean isNoPrivateMethods() {
            return dialogPanel.jCheckBoxNPM.isSelected();
        }

        public SearchPanel getSearchPanel() {
            return searchPanel;
        }
    }

    private class SearchPanel extends JScrollPane {
        private JPanel panel;
        private Set<String> classes;
        private final int maxSize = 100;//else becomes slow
        private Set<String> addedClasses = new HashSet<>();

        public SearchPanel(JPanel panel, Set<String> classes, String title) {
            super(panel);
            this.panel = panel;
            this.classes = classes;

            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            TitledBorder titledBorder = BorderFactory.createTitledBorder("");
            setBorder(BorderFactory.createTitledBorder(titledBorder, title, TitledBorder.TOP, TitledBorder.TOP, new Font("Serif", Font.BOLD, 12), JBColor.RED));

            JRadioButton allClassesRadioButton = null;
            if (FILTER_SEARCH_PANEL_TITLE.equals(title)) {
                allClassesRadioButton = new JRadioButton("All the Classes(default)");
                allClassesRadioButton.setForeground(JBColor.GREEN);
                allClassesRadioButton.setSelected(true);
                panel.add(allClassesRadioButton);

                JRadioButton finalAllClassesRadioButton = allClassesRadioButton;
                allClassesRadioButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        boolean selected = finalAllClassesRadioButton.isSelected();

                        if (selected) {
                            addedClasses.add(ALL_CLASSES);
                        } else {
                            addedClasses.remove(ALL_CLASSES);
                        }
                    }
                });
            }


            int pc = 0;
            for (String className : classes) {
                pc++;
                if (pc > maxSize) {
                    panel.add(new JLabel("..."));
                    break;
                }
                JRadioButton radioButton = new JRadioButton(className);
                panel.add(radioButton);

                JRadioButton finalAllClassesRadioButton1 = allClassesRadioButton;
                radioButton.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        boolean selected = radioButton.isSelected();

                        if (selected) {
                            addedClasses.add(className);
                        } else {
                            addedClasses.remove(className);
                        }

                        if (finalAllClassesRadioButton1 != null && finalAllClassesRadioButton1.isSelected()) {
                            finalAllClassesRadioButton1.setSelected(false);
                            addedClasses.remove(ALL_CLASSES);
                        }
                    }
                });
            }

            this.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

            this.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
            getVerticalScrollBar().setUnitIncrement(20);
            setPreferredSize(new Dimension(300, 400));
        }

        public void updateSearch(String text) {
            panel.removeAll();

            int pc = 0;
            for (String className : classes) {
                if (className.toLowerCase().contains(text.toLowerCase())) {
                    pc++;
                    if (pc > maxSize) {
                        panel.add(new JLabel("..."));
                        break;
                    }

                    JRadioButton radioButton = new JRadioButton(className);
                    panel.add(radioButton);

                    radioButton.addActionListener(new ActionListener() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            boolean selected = radioButton.isSelected();

                            if (selected) {
                                addedClasses.add(className);
                            } else {
                                addedClasses.remove(className);
                            }
                        }
                    });
                }
            }

            panel.revalidate();
            panel.repaint();
        }

        public Set<String> getAddedClasses() {
            return addedClasses;
        }
    }

    private class SearchButtonPanel extends JPanel {

        public SearchButtonPanel(SearchPanel searchPanel) {
            super();

            setLayout(new GridLayout(1, 2));
            JLabel label = new JLabel("Search for:");
            JTextField searchBox = new JTextField();

            searchBox.getDocument().addDocumentListener(new DocumentListener() {
                @Override
                public void insertUpdate(DocumentEvent e) {
                    updateSearch();
                }

                @Override
                public void removeUpdate(DocumentEvent e) {
                    updateSearch();
                }

                @Override
                public void changedUpdate(DocumentEvent e) {
                    updateSearch();
                }

                public void updateSearch() {
                    String text = searchBox.getText();

                    if (searchPanel != null) {
                        searchPanel.updateSearch(text);
                    }

                }
            });

            add(label);
            add(searchBox);
            setPreferredSize(new Dimension(300, 30));
        }
    }

    private class SearchDialogWrapper extends DialogWrapper {
        private final SearchPanel searchPanel = new SearchPanel(new JPanel(), allClasses, "Highlight selected classes with RED color");
        private final SearchButtonPanel searchButtonPanel = new SearchButtonPanel(searchPanel);

        public SearchDialogWrapper(Project project) {
            super(project, false);
            setResizable(false);
            setTitle("Search Classes");
            init();
        }

        @Override
        protected JComponent createCenterPanel() {
            return searchPanel;
        }

        @Nullable
        @Override
        protected JComponent createNorthPanel() {
            return searchButtonPanel;
        }

        public SearchPanel getSearchPanel() {
            return searchPanel;
        }
    }

    private class SearchAction extends AnAction {
        public SearchAction() {
            super("Search Classes", "Search for classes", AllIcons.Actions.Search);
        }

        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            SearchDialogWrapper dialogWrapper = new SearchDialogWrapper(project);
            dialogWrapper.show();
            if (dialogWrapper.isOK()) {

                SearchPanel searchPanel = dialogWrapper.getSearchPanel();
                Set<String> highlightClasses = searchPanel.getAddedClasses();

                if (highlightClasses.contains(ALL_CLASSES) && highlightClasses.size() > 1) {
                    highlightClasses.clear();
                    highlightClasses.add(ALL_CLASSES);
                }

                highlightCallStack(callStack, highlightClasses);

                if (FILTER_BLOCKED) {
                    //it is blocked if user exited from filter menu, so generation will yield a too big sequence diagram
                    //either show a message to the user or silently fail
                } else {
                    generate();
                }
            }
        }

        private void highlightCallStack(CallStack callStack, Set<String> highlightClasses) {
            if (callStack == null) {
                return;
            }

            callStack.setHighlighted(false);//reset

            try {
                String className = callStack.getMethod().getClassDescription().getClassName();
                if (highlightClasses.contains(className)) {
                    callStack.setHighlighted(true);
                }
            } catch (Exception e) {
            }

            List<CallStack> calls = callStack.getCalls();

            if (calls != null) {
                for (CallStack call : calls) {
                    highlightCallStack(call, highlightClasses);
                }
            }
        }
    }

    private class FilterAction extends AnAction {
        public FilterAction() {
            super("Filter Classes", "Filter classes", AllIcons.General.Filter);
        }

        public void actionPerformed(@NotNull AnActionEvent anActionEvent) {
            this.showDialog(true);
        }

        public void showDialog(boolean isActionEvent) {
            int preCallDepth = _callDepth;
            boolean prevPrivateMethods = _noPrivateMethods;
            boolean prevFilteredClasses = _noPrivateMethods;

            OptionsDialogWrapper dialogWrapper = new OptionsDialogWrapper(project);
            dialogWrapper.show();
            if (dialogWrapper.isOK()) {
                _callDepth = dialogWrapper.getCallStackDepth();
//                _noGetterSetters = dialogWrapper.isNoGetterSetters();
//                _noConstructors = dialogWrapper.isNoConstructors();
                _noPrivateMethods = dialogWrapper.isNoPrivateMethods();

                SearchPanel searchPanel = dialogWrapper.getSearchPanel();
                Set<String> addedClasses = searchPanel.getAddedClasses();

                filterCallStack(callStack, addedClasses);
                filterCallStackBasedOnDepth(callStack);

                long totalCalls = calcAllClasses(callStack, false);

                if (totalCalls > MAX_CALLS) {
                    int resp = JOptionPane.showConfirmDialog(null, "Please provide some filtering options (include only a small number of classes" +
                            " and/or lower call stack depth) to lower the number of calls down to " + MAX_CALLS + ". Currently there are "
                            + totalCalls + " calls", "Show Sequence Diagram", JOptionPane.OK_CANCEL_OPTION);

                    if (resp == JOptionPane.CANCEL_OPTION) {
                        _callDepth = preCallDepth;
                        _noPrivateMethods = prevPrivateMethods;
                        resetFilteredClasses(callStack);
                        filterCallStack(callStack, filteredClasses);//filter by original classes
                        filterCallStackBasedOnDepth(callStack);
                        return;
                    }

                    new FilterAction().showDialog(isActionEvent);
                } else {
                    filteredClasses = addedClasses;
                    if (isActionEvent) {
                        //assign only when done
                        generate();
                    }
                }
            }
        }

        private void resetFilteredClasses(CallStack callStack) {
            if (callStack == null) {
                return;
            }

            callStack.setFiltered(false);

            List<CallStack> calls = callStack.getCalls();

            if (calls != null) {
                for (CallStack call : calls) {
                    resetFilteredClasses(call);
                }
            }
        }

        private int filterCallStackBasedOnDepth(CallStack callStack) {
            if (callStack == null) {
                return 0;
            }

            int maxDepth = 0;

            List<CallStack> calls = callStack.getCalls();

            if (calls != null) {
                for (CallStack call : calls) {
                    int depth = filterCallStackBasedOnDepth(call);

                    if (depth > maxDepth) {
                        maxDepth = depth;
                    }

                }
            }

            if (maxDepth > _callDepth) {
                callStack.setFiltered(true);
            }

            return maxDepth + 1;
        }

        private boolean filterCallStack(CallStack callStack, Set<String> classes) {
            if (callStack == null) {
                return true;
            }

            if (classes.size() == 1 && classes.iterator().next().equals(ALL_CLASSES)) {
                callStack.setFiltered(false);//reset
            } else if (classes.isEmpty()) {
                callStack.setFiltered(false);//reset
            } else {
                callStack.setFiltered(true);//reset
            }
            boolean isIgnorable = false;
            String className;
            try {
                className = callStack.getMethod().getClassDescription().getClassName();

                if (classes.contains(className)) {
                    callStack.setFiltered(false);
                }

                if (_noPrivateMethods) {
                    PsiClass psiClass = psiFacade.findClass(className, GlobalSearchScope.projectScope(project));

                    if (psiClass != null) {
                        PsiMethod[] methods = psiClass.getMethods();
                        String methodName = callStack.getMethod().getMethodName();
                        for (PsiMethod method : methods) {
                            if (method.getName().equals(methodName)) {
                                PsiModifierList modifierList = method.getModifierList();
                                if (modifierList.hasModifierProperty(PsiModifier.PRIVATE)) {
                                    isIgnorable = true;
                                }

                                break;
                            }
                        }
                    }
                }

            } catch (Exception e) {
            }

            if (!isIgnorable) {
                List<CallStack> calls = callStack.getCalls();

                if (calls != null) {
                    for (CallStack call : calls) {
                        boolean filtered = filterCallStack(call, classes);

                        if (!filtered) {
                            callStack.setFiltered(false);
                        }
                    }
                }

                return callStack.isFiltered();
            } else {
                callStack.setFiltered(true);
                return true;
            }
        }
    }
}
