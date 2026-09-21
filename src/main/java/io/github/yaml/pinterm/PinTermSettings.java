package io.github.yaml.pinterm;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.RoamingType;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@State(
    name = "PinTermSettings",
    storages = @Storage(value = "pinterm.xml", roamingType = RoamingType.DISABLED)
)
@SuppressWarnings("deprecation")
public final class PinTermSettings implements PersistentStateComponent<PinTermSettings.State> {
    private State state = createDefaultState();

    public static PinTermSettings getInstance() {
        return ApplicationManager.getApplication().getService(PinTermSettings.class);
    }

    @Override
    public @NotNull State getState() {
        state = normalizeState(state);
        return state;
    }

    @Override
    public void loadState(@NotNull State state) {
        this.state = normalizeState(state);
    }

    public @NotNull State copyState() {
        return new State(getState());
    }

    public boolean hasTabs() {
        return !getState().tabs.isEmpty();
    }

    public @NotNull List<TerminalTabState> getTabs() {
        List<TerminalTabState> copies = new ArrayList<>();
        for (TerminalTabState tab : getState().tabs) {
            copies.add(tab.copy());
        }
        return Collections.unmodifiableList(copies);
    }

    public @Nullable TerminalTabState findTabById(@Nullable String tabId) {
        if (tabId == null || tabId.isBlank()) {
            return null;
        }

        for (TerminalTabState tab : getState().tabs) {
            if (tabId.equals(tab.id)) {
                return tab.copy();
            }
        }
        return null;
    }

    static @NotNull State createDefaultState() {
        State state = new State();
        for (PinTermDefaults.DefaultTab defaultTab : PinTermDefaults.DEFAULT_TABS) {
            state.tabs.add(TerminalTabState.createNamed(defaultTab.name(), defaultTab.command()));
        }
        return state;
    }

    static @NotNull State normalizeState(@Nullable State source) {
        if (source == null) {
            return createDefaultState();
        }

        State normalized = new State();
        Set<String> seenNames = new LinkedHashSet<>();
        for (TerminalTabState tab : source.tabs) {
            if (tab == null) {
                continue;
            }
            TerminalTabState normalizedTab = normalizeTab(tab.copy());
            normalizedTab.name = uniqueName(normalizedTab.name, seenNames);
            seenNames.add(normalizedTab.name);
            normalized.tabs.add(normalizedTab);
        }
        if (normalized.tabs.isEmpty()) {
            return createDefaultState();
        }
        return normalized;
    }

    private static @NotNull TerminalTabState normalizeTab(@NotNull TerminalTabState tab) {
        if (tab.id == null || tab.id.isBlank()) {
            tab.id = UUID.randomUUID().toString();
        }
        String tabName = tab.name == null ? "" : tab.name.trim();
        tab.name = tabName.isEmpty() ? PinTermDefaults.UNNAMED_TAB_NAME : tabName;
        if (tab.command == null) {
            tab.command = "";
        }
        tab.command = PinTermCommands.migrateLegacyShellScript(tab.command, tab.shellScript);
        if (PinTermCommands.containsUnsafeControlChars(tab.command)) {
            tab.command = "";
        }
        tab.shellScript = null;
        return tab;
    }

    static @NotNull String uniqueName(@NotNull String name, @NotNull Set<String> seenNames) {
        if (!seenNames.contains(name)) {
            return name;
        }

        int suffix = 2;
        String candidate = name + " " + suffix;
        while (seenNames.contains(candidate)) {
            suffix++;
            candidate = name + " " + suffix;
        }
        return candidate;
    }

    public static final class State {
        public List<TerminalTabState> tabs = new ArrayList<>();

        public State() {
        }

        public State(@NotNull State other) {
            for (TerminalTabState tab : other.tabs) {
                tabs.add(tab.copy());
            }
        }

        @Override
        public boolean equals(Object object) {
            if (this == object) {
                return true;
            }
            if (!(object instanceof State)) {
                return false;
            }
            State state = (State) object;
            return Objects.equals(tabs, state.tabs);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tabs);
        }
    }
}
