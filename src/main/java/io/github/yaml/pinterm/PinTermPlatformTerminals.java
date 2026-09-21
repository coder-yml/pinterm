package io.github.yaml.pinterm;

import com.intellij.openapi.project.Project;
import com.intellij.terminal.frontend.editor.TerminalViewVirtualFile;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager;
import com.intellij.terminal.frontend.view.TerminalView;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.terminal.settings.impl.TerminalSessionPersistedTab;
import org.jetbrains.plugins.terminal.settings.impl.TerminalTabsStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

final class PinTermPlatformTerminals {
    private PinTermPlatformTerminals() {
    }

    static @NotNull TerminalToolWindowTab createTab(
        @NotNull Project project,
        @NotNull String workingDirectory,
        @NotNull String tabName
    ) {
        return TerminalToolWindowTabsManager.getInstance(project)
            .createTabBuilder()
            .workingDirectory(workingDirectory)
            .tabName(tabName)
            .requestFocus(false)
            .deferSessionStartUntilUiShown(false)
            .shouldAddToToolWindow(true)
            .createTab();
    }

    static void detachTab(
        @NotNull TerminalToolWindowTabsManager tabsManager,
        @NotNull TerminalToolWindowTab terminalTab
    ) {
        tabsManager.detachTab(terminalTab);
    }

    static void closeTab(
        @NotNull TerminalToolWindowTabsManager tabsManager,
        @NotNull TerminalToolWindowTab terminalTab
    ) {
        tabsManager.closeTab(terminalTab);
    }

    static @NotNull TerminalViewVirtualFile createEditorFile(@NotNull TerminalToolWindowTab terminalTab) {
        return new TerminalViewVirtualFile(terminalTab);
    }

    static @NotNull TerminalView viewOf(@NotNull TerminalToolWindowTab terminalTab) {
        return terminalTab.getView();
    }

    static void sendExecutedText(@NotNull TerminalView terminalView, @NotNull String command) {
        terminalView.createSendTextBuilder()
            .shouldExecute()
            .send(command);
    }

    static @NotNull List<TerminalSessionPersistedTab> storedTabs(@NotNull Project project) {
        return new ArrayList<>(TerminalTabsStorage.getInstance(project).getStoredTabs());
    }

    static void updateStoredTabs(
        @NotNull Project project,
        @NotNull List<TerminalSessionPersistedTab> remainingTabs
    ) {
        TerminalTabsStorage.getInstance(project).updateStoredTabs(remainingTabs);
    }

    static @NotNull List<TerminalSessionPersistedTab> withoutPluginTabs(
        @NotNull List<TerminalSessionPersistedTab> storedTabs,
        @NotNull Set<String> configuredTabNames
    ) {
        List<TerminalSessionPersistedTab> remainingTabs = new ArrayList<>();
        for (TerminalSessionPersistedTab storedTab : storedTabs) {
            if (shouldDropPersistedTab(storedTab == null ? null : storedTab.getName(), configuredTabNames)) {
                continue;
            }
            remainingTabs.add(storedTab);
        }
        return remainingTabs;
    }

    static boolean shouldDropPersistedTab(
        @Nullable String storedTabName,
        @NotNull Set<String> configuredTabNames
    ) {
        return storedTabName != null
            && PinTermTabNames.isPinTermTabName(storedTabName, configuredTabNames);
    }
}
