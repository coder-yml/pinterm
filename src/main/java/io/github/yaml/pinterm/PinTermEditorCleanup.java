package io.github.yaml.pinterm;

final class PinTermEditorCleanup {
    private PinTermEditorCleanup() {
    }

    static boolean shouldCloseEditorFile(boolean owned, boolean tracked, boolean includeTrackedFiles) {
        if (tracked) {
            return includeTrackedFiles;
        }
        return owned;
    }
}
