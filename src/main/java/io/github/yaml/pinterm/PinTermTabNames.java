package io.github.yaml.pinterm;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

final class PinTermTabNames {
    private PinTermTabNames() {
    }

    static @NotNull Set<String> fromTabs(@NotNull List<TerminalTabState> tabs) {
        Set<String> names = new LinkedHashSet<>();
        for (TerminalTabState tab : tabs) {
            String normalizedName = normalize(tab == null ? null : tab.name);
            if (normalizedName != null) {
                names.add(normalizedName);
            }
        }
        return names;
    }

    static boolean isPinTermTabName(@Nullable String tabName, @NotNull Set<String> configuredTabNames) {
        String normalizedName = normalize(tabName);
        return normalizedName != null && configuredTabNames.contains(normalizedName);
    }

    private static @Nullable String normalize(@Nullable String tabName) {
        if (tabName == null) {
            return null;
        }
        String normalizedName = tabName.trim();
        return normalizedName.isEmpty() ? null : normalizedName;
    }
}
