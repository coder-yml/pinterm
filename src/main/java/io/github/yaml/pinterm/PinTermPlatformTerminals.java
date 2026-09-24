package io.github.yaml.pinterm;

import com.intellij.openapi.project.Project;
import com.intellij.terminal.frontend.editor.TerminalViewVirtualFile;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager;
import com.intellij.terminal.frontend.view.TerminalView;
import org.jetbrains.annotations.NotNull;

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
}
