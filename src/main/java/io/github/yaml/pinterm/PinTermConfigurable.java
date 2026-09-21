package io.github.yaml.pinterm;

import com.intellij.openapi.actionSystem.ActionToolbarPosition;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SearchableConfigurable;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.components.JBList;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.components.JBTextField;
import com.intellij.util.ui.JBUI;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListSelectionModel;
import javax.swing.JPanel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.HashSet;
import java.util.Set;

@SuppressWarnings("deprecation")
public final class PinTermConfigurable implements SearchableConfigurable {
    private static final Dimension COMMAND_EDITOR_PREFERRED_SIZE = JBUI.size(0, 140);

    private JPanel panel;
    private DefaultListModel<TerminalTabState> tabListModel;
    private JBList<TerminalTabState> tabList;
    private JBTextField tabNameField;
    private JBTextArea commandTextArea;
    private boolean updatingEditor;

    @Override
    public @NotNull String getId() {
        return "pinterm.settings";
    }

    @Override
    public @Nls String getDisplayName() {
        return "PinTerm";
    }

    @Override
    public @Nullable JComponent createComponent() {
        if (panel != null) {
            return panel;
        }

        tabListModel = new DefaultListModel<>();
        tabList = new JBList<>(tabListModel);
        tabList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabList.setCellRenderer(new TabListRenderer());
        tabList.setVisibleRowCount(12);
        tabList.addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                loadSelectedTabIntoEditor();
            }
        });

        tabNameField = new JBTextField();
        commandTextArea = new JBTextArea();
        commandTextArea.setRows(5);
        commandTextArea.setLineWrap(true);
        commandTextArea.setWrapStyleWord(false);
        attachEditorListeners();

        panel = new JPanel(new BorderLayout(0, 0));
        panel.setBorder(JBUI.Borders.empty(12));
        panel.add(createTabsPanel(), BorderLayout.WEST);
        panel.add(createEditorPanel(), BorderLayout.CENTER);

        reset();
        return panel;
    }

    @Override
    public @Nullable JComponent getPreferredFocusedComponent() {
        return tabList;
    }

    @Override
    public boolean isModified() {
        if (tabListModel == null) {
            return false;
        }

        PinTermSettings settings = PinTermSettings.getInstance();
        try {
            return !buildStateFromUi(true).equals(settings.copyState());
        }
        catch (ConfigurationException exception) {
            return true;
        }
    }

    @Override
    public void apply() throws ConfigurationException {
        if (tabListModel == null) {
            return;
        }

        PinTermSettings.getInstance().loadState(buildStateFromUi(true));
    }

    @Override
    public void reset() {
        if (tabListModel == null) {
            return;
        }

        loadStateIntoUi(PinTermSettings.getInstance().copyState());
    }

    @Override
    public void disposeUIResources() {
        panel = null;
        tabListModel = null;
        tabList = null;
        tabNameField = null;
        commandTextArea = null;
        updatingEditor = false;
    }

    private @NotNull JComponent createTabsPanel() {
        ToolbarDecorator decorator = ToolbarDecorator.createDecorator(tabList)
            .setToolbarPosition(ActionToolbarPosition.RIGHT)
            .setPreferredSize(JBUI.size(220, 0))
            .setAddAction(button -> addTab())
            .setRemoveAction(button -> deleteSelectedTab())
            .setMoveUpAction(button -> moveSelectedTab(-1))
            .setMoveDownAction(button -> moveSelectedTab(1))
            .setRemoveActionUpdater(button -> tabList.getSelectedIndex() >= 0 && tabListModel.getSize() > 1)
            .setMoveUpActionUpdater(button -> tabList.getSelectedIndex() > 0)
            .setMoveDownActionUpdater(button -> {
                int selectedIndex = tabList.getSelectedIndex();
                return selectedIndex >= 0 && selectedIndex < tabListModel.getSize() - 1;
            })
            .setAddActionName("Add Tab")
            .setRemoveActionName("Delete Tab")
            .setMoveUpActionName("Move Up")
            .setMoveDownActionName("Move Down")
            .setPanelBorder(JBUI.Borders.empty())
            .setScrollPaneBorder(BorderFactory.createEmptyBorder());

        JPanel tabsPanel = new JPanel(new BorderLayout(0, 8));
        tabsPanel.setBorder(JBUI.Borders.emptyRight(16));
        tabsPanel.setMinimumSize(JBUI.size(180, 240));
        tabsPanel.add(new JLabel("Tabs"), BorderLayout.NORTH);
        tabsPanel.add(decorator.createPanel(), BorderLayout.CENTER);
        return tabsPanel;
    }

    private @NotNull JComponent createEditorPanel() {
        JPanel editorPanel = new JPanel(new BorderLayout());
        editorPanel.setBorder(JBUI.Borders.emptyLeft(8));

        JPanel formPanel = new JPanel(new GridBagLayout());
        GridBagConstraints constraints = baseConstraints();

        formPanel.add(new JLabel("Tab Name"), constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 12, 0);
        formPanel.add(tabNameField, constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 6, 0);
        formPanel.add(new JLabel("Shell Command"), constraints);

        constraints.gridy++;
        constraints.insets = new Insets(0, 0, 0, 0);
        constraints.fill = GridBagConstraints.BOTH;
        JBScrollPane commandScrollPane = new JBScrollPane(commandTextArea);
        commandScrollPane.setPreferredSize(COMMAND_EDITOR_PREFERRED_SIZE);
        formPanel.add(commandScrollPane, constraints);

        editorPanel.add(formPanel, BorderLayout.NORTH);
        return editorPanel;
    }

    private @NotNull GridBagConstraints baseConstraints() {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.anchor = GridBagConstraints.NORTHWEST;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        constraints.weighty = 0.0;
        constraints.insets = new Insets(0, 0, 6, 0);
        return constraints;
    }

    private void attachEditorListeners() {
        DocumentListener documentListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent event) {
                syncEditorToModel();
            }

            @Override
            public void removeUpdate(DocumentEvent event) {
                syncEditorToModel();
            }

            @Override
            public void changedUpdate(DocumentEvent event) {
                syncEditorToModel();
            }
        };

        tabNameField.getDocument().addDocumentListener(documentListener);
        commandTextArea.getDocument().addDocumentListener(documentListener);
    }

    private void loadStateIntoUi(@NotNull PinTermSettings.State state) {
        updatingEditor = true;
        try {
            tabListModel.clear();
            for (TerminalTabState tab : state.tabs) {
                tabListModel.addElement(tab.copy());
            }
            if (!tabListModel.isEmpty()) {
                tabList.setSelectedIndex(0);
            }
            else {
                clearTabEditor();
                setTabEditorEnabled(false);
            }
        }
        finally {
            updatingEditor = false;
        }

        loadSelectedTabIntoEditor();
    }

    private @NotNull PinTermSettings.State buildStateFromUi(boolean validate) throws ConfigurationException {
        syncEditorToModel();

        PinTermSettings.State state = new PinTermSettings.State();
        Set<String> seenNames = new HashSet<>();
        for (int index = 0; index < tabListModel.size(); index++) {
            TerminalTabState tab = tabListModel.get(index).copy();
            tab.name = trim(tab.name);
            tab.command = emptyIfNull(tab.command);
            tab.shellScript = null;

            if (validate && tab.name.isBlank()) {
                throw new ConfigurationException("Tab name cannot be blank.");
            }
            if (validate) {
                validateSingleLineCommand(tab.name, tab.command);
                validateUniqueTabName(tab.name, seenNames);
            }

            state.tabs.add(tab);
        }

        return PinTermSettings.normalizeState(state);
    }

    private void loadSelectedTabIntoEditor() {
        TerminalTabState tab = getSelectedTab();
        updatingEditor = true;
        try {
            if (tab == null) {
                clearTabEditor();
                setTabEditorEnabled(false);
                return;
            }

            setTabEditorEnabled(true);
            tabNameField.setText(tab.name);
            setCommandText(tab.command);
        }
        finally {
            updatingEditor = false;
        }
    }

    private void syncEditorToModel() {
        if (updatingEditor) {
            return;
        }

        TerminalTabState selectedTab = getSelectedTab();
        if (selectedTab != null) {
            selectedTab.name = tabNameField.getText();
            selectedTab.command = getCommandText();
            selectedTab.shellScript = null;
            tabList.repaint();
        }
    }

    private void addTab() {
        TerminalTabState tab = TerminalTabState.createNamed(createUniqueTabName("New Tab"));
        tabListModel.addElement(tab);
        tabList.setSelectedIndex(tabListModel.size() - 1);
    }

    private void deleteSelectedTab() {
        int selectedIndex = tabList.getSelectedIndex();
        if (selectedIndex < 0 || tabListModel.size() <= 1) {
            return;
        }

        tabListModel.remove(selectedIndex);
        int nextIndex = Math.min(selectedIndex, tabListModel.size() - 1);
        if (nextIndex >= 0) {
            tabList.setSelectedIndex(nextIndex);
        }
    }

    private void moveSelectedTab(int offset) {
        int selectedIndex = tabList.getSelectedIndex();
        int targetIndex = selectedIndex + offset;
        if (selectedIndex < 0 || targetIndex < 0 || targetIndex >= tabListModel.size()) {
            return;
        }

        TerminalTabState tab = tabListModel.remove(selectedIndex);
        tabListModel.add(targetIndex, tab);
        tabList.setSelectedIndex(targetIndex);
    }

    private void setTabEditorEnabled(boolean enabled) {
        tabNameField.setEnabled(enabled);
        commandTextArea.setEnabled(enabled);
    }

    private void clearTabEditor() {
        tabNameField.setText("");
        setCommandText("");
    }

    private void setCommandText(@Nullable String value) {
        commandTextArea.setText(emptyIfNull(value));
        commandTextArea.setCaretPosition(0);
    }

    private @NotNull String getCommandText() {
        return commandTextArea.getText();
    }

    private @Nullable TerminalTabState getSelectedTab() {
        return tabList == null ? null : tabList.getSelectedValue();
    }

    private @NotNull String createUniqueTabName(@NotNull String baseName) {
        String candidate = baseName;
        int suffix = 2;
        while (containsTabName(candidate)) {
            candidate = baseName + " " + suffix;
            suffix++;
        }
        return candidate;
    }

    private boolean containsTabName(@NotNull String candidate) {
        for (int index = 0; index < tabListModel.size(); index++) {
            if (candidate.equals(tabListModel.get(index).name)) {
                return true;
            }
        }
        return false;
    }

    private static @NotNull String trim(@Nullable String value) {
        return value == null ? "" : value.trim();
    }

    private static @NotNull String emptyIfNull(@Nullable String value) {
        return value == null ? "" : value;
    }

    static boolean containsLineBreak(@NotNull String value) {
        return PinTermCommands.containsUnsafeControlChars(value);
    }

    static void validateSingleLineCommand(@NotNull String tabName, @NotNull String command)
        throws ConfigurationException {
        if (PinTermCommands.containsUnsafeControlChars(command)) {
            throw new ConfigurationException(
                "Shell command for \"" + PinTermDefaults.displayTabName(tabName) + "\" must be a single line."
            );
        }
    }

    static void validateUniqueTabName(@NotNull String tabName, @NotNull Set<String> seenNames)
        throws ConfigurationException {
        if (!seenNames.add(tabName)) {
            throw new ConfigurationException("Tab name \"" + PinTermDefaults.displayTabName(tabName) + "\" is already used.");
        }
    }

    private static final class TabListRenderer extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(
            JList<?> list,
            Object value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
        ) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value instanceof TerminalTabState tab) {
                label.setText(PinTermDefaults.displayTabName(tab.name));
            }
            return label;
        }
    }
}
