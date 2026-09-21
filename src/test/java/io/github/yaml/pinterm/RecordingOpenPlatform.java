package io.github.yaml.pinterm;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

final class RecordingOpenPlatform implements PinTermOpenFlow.Platform {
    final List<String> calls = new ArrayList<>();
    boolean failDetach;
    boolean failOpenAndPin;
    final Object createdTab = "tab";
    final Object createdFile = "file";
    final Object createdView = "view";

    @Override
    public @NotNull Object createTab(@NotNull String workingDirectory, @NotNull String tabName) {
        calls.add("createTab:" + workingDirectory + ":" + tabName);
        return createdTab;
    }

    @Override
    public void detachTab(@NotNull Object terminalTab) {
        calls.add("detachTab:" + terminalTab);
        if (failDetach) {
            throw new IllegalStateException("detach failed");
        }
    }

    @Override
    public void closeTab(@NotNull Object terminalTab) {
        calls.add("closeTab:" + terminalTab);
    }

    @Override
    public @NotNull Object createEditorFile(@NotNull Object terminalTab) {
        calls.add("createEditorFile:" + terminalTab);
        return createdFile;
    }

    @Override
    public void openAndPin(@NotNull Object editorFile) {
        calls.add("openAndPin:" + editorFile);
        if (failOpenAndPin) {
            throw new IllegalStateException("open failed");
        }
    }

    @Override
    public @NotNull Object terminalView(@NotNull Object terminalTab) {
        calls.add("terminalView:" + terminalTab);
        return createdView;
    }
}
