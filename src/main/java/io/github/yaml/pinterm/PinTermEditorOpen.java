package io.github.yaml.pinterm;

final class PinTermEditorOpen {
    static final int MAX_OPEN_ATTEMPTS = 2;

    private PinTermEditorOpen() {
    }

    static boolean isOpenSuccessful(boolean fileIsOpen) {
        return fileIsOpen;
    }

    static boolean shouldRetryOpen(boolean opened, int attempt) {
        return !opened && attempt + 1 < MAX_OPEN_ATTEMPTS;
    }

    static boolean canPinInWindow(boolean windowPresent, boolean fileOpenInThatWindow) {
        return windowPresent && fileOpenInThatWindow;
    }
}
