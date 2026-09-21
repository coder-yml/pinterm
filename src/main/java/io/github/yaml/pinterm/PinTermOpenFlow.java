package io.github.yaml.pinterm;

import org.jetbrains.annotations.NotNull;

final class PinTermOpenFlow {
    private PinTermOpenFlow() {
    }

    interface Platform {
        @NotNull Object createTab(@NotNull String workingDirectory, @NotNull String tabName);

        void detachTab(@NotNull Object terminalTab);

        void closeTab(@NotNull Object terminalTab);

        @NotNull Object createEditorFile(@NotNull Object terminalTab);

        void openAndPin(@NotNull Object editorFile);

        @NotNull Object terminalView(@NotNull Object terminalTab);
    }

    record OpenedSession(@NotNull Object terminalTab, @NotNull Object editorFile, @NotNull Object terminalView) {
    }

    static @NotNull OpenedSession openPinned(
        @NotNull Platform platform,
        @NotNull TerminalTabState tab,
        @NotNull String workingDirectory
    ) {
        Object terminalTab = platform.createTab(workingDirectory, tab.name);
        try {
            platform.detachTab(terminalTab);
            Object editorFile = platform.createEditorFile(terminalTab);
            platform.openAndPin(editorFile);
            return new OpenedSession(terminalTab, editorFile, platform.terminalView(terminalTab));
        }
        catch (Throwable error) {
            try {
                platform.closeTab(terminalTab);
            }
            catch (Throwable ignored) {
            }
            throw error;
        }
    }

    static boolean hasCommandToSend(@NotNull TerminalTabState tab) {
        return PinTermCommands.isSafeToSend(tab.command);
    }
}
