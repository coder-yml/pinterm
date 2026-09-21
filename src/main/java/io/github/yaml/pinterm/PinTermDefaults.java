package io.github.yaml.pinterm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

final class PinTermDefaults {
    static final String DEFAULT_TAB_NAME = "PinTerm";
    static final String UNNAMED_TAB_NAME = "Tab";
    static final int MAX_DISPLAY_TAB_NAME_LENGTH = 80;

    record DefaultTab(@NotNull String name, @NotNull String command) {
    }

    static final List<DefaultTab> DEFAULT_TABS = List.of(
        new DefaultTab("Codex", "codex"),
        new DefaultTab("Claude", "claude"),
        new DefaultTab("Grok", "grok")
    );

    private PinTermDefaults() {
    }

    static @NotNull String displayTabName(@Nullable String tabName) {
        String value = tabName == null || tabName.isBlank() ? DEFAULT_TAB_NAME : tabName.trim();
        if (value.length() <= MAX_DISPLAY_TAB_NAME_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_DISPLAY_TAB_NAME_LENGTH - 3) + "...";
    }
}
