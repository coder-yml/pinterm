package io.github.yaml.pinterm;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.actionSystem.ex.ComboBoxAction;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;

public final class PinTermToolbarDropdownAction extends ComboBoxAction {
    public PinTermToolbarDropdownAction() {
        setPopupTitle("PinTerm");
        setSmallVariant(true);
    }

    @Override
    public void update(@NotNull AnActionEvent event) {
        Presentation presentation = event.getPresentation();
        Project project = event.getProject();

        presentation.setText(toolbarButtonText());
        presentation.setDescription("Open a configured PinTerm tab");
        presentation.setEnabled(isEnabledFor(project != null, PinTermSettings.getInstance().hasTabs()));
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.EDT;
    }

    @Override
    protected @NotNull DefaultActionGroup createPopupActionGroup(@NotNull JComponent button, @NotNull DataContext dataContext) {
        DefaultActionGroup group = new DefaultActionGroup();
        Project project = CommonDataKeys.PROJECT.getData(dataContext);
        PinTermSettings settings = PinTermSettings.getInstance();

        for (TerminalTabState tab : settings.getTabs()) {
            group.add(new OpenTabAction(project, tab.name, tab.id));
        }

        group.add(Separator.create());
        group.add(new ManageTabsAction(project));
        return group;
    }

    static boolean isEnabledFor(boolean projectOpen, boolean hasTabs) {
        return projectOpen && hasTabs;
    }

    static @NotNull String toolbarButtonText() {
        return "";
    }

    private static final class OpenTabAction extends DumbAwareAction {
        private final Project project;
        private final String tabId;

        private OpenTabAction(Project project, @NotNull String tabName, @NotNull String tabId) {
            super(tabName);
            this.project = project;
            this.tabId = tabId;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            Project targetProject = project != null ? project : event.getProject();
            if (targetProject == null) {
                return;
            }

            TerminalTabState tab = PinTermSettings.getInstance().findTabById(tabId);
            if (tab == null) {
                return;
            }

            targetProject.getService(PinTermService.class)
                .openTerminalInEditor(tab);
        }
    }

    private static final class ManageTabsAction extends DumbAwareAction {
        private final Project project;

        private ManageTabsAction(Project project) {
            super("Manage Tabs...");
            this.project = project;
        }

        @Override
        public void actionPerformed(@NotNull AnActionEvent event) {
            Project targetProject = project != null ? project : event.getProject();
            ShowSettingsUtil.getInstance().showSettingsDialog(targetProject, PinTermConfigurable.class);
        }
    }
}
