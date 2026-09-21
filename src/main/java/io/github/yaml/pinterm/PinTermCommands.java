package io.github.yaml.pinterm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PinTermCommands {
    private PinTermCommands() {
    }

    static boolean containsUnsafeControlChars(@Nullable String command) {
        if (command == null) {
            return false;
        }
        for (int index = 0; index < command.length(); index++) {
            char character = command.charAt(index);
            if (character == '\n' || character == '\r' || character == '\0') {
                return true;
            }
        }
        return false;
    }

    static boolean isSafeToSend(@Nullable String command) {
        return command != null && !command.isBlank() && !containsUnsafeControlChars(command);
    }

    static @NotNull String migrateLegacyShellScript(@Nullable String command, @Nullable String shellScript) {
        if (command != null && !command.isEmpty()) {
            return command;
        }
        if (shellScript == null || shellScript.isEmpty()) {
            return command == null ? "" : command;
        }
        if (containsUnsafeControlChars(shellScript)) {
            return "";
        }
        return shellScript;
    }
}
