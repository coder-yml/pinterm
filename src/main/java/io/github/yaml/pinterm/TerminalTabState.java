package io.github.yaml.pinterm;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

public final class TerminalTabState {
    public String id = UUID.randomUUID().toString();
    public String name = PinTermDefaults.DEFAULT_TAB_NAME;
    public String command = "";

    /** Legacy single-line field; loaded into command then cleared. Multi-line values are dropped. */
    @Deprecated
    public String shellScript = null;

    public TerminalTabState() {
    }

    public TerminalTabState(@NotNull TerminalTabState other) {
        id = other.id;
        name = other.name;
        command = other.command;
        shellScript = other.shellScript;
    }

    public static @NotNull TerminalTabState createDefault() {
        return createNamed(PinTermDefaults.DEFAULT_TAB_NAME);
    }

    public static @NotNull TerminalTabState createNamed(@NotNull String name) {
        return createNamed(name, "");
    }

    public static @NotNull TerminalTabState createNamed(@NotNull String name, @NotNull String command) {
        TerminalTabState tab = new TerminalTabState();
        tab.name = name;
        tab.command = command;
        return tab;
    }

    public @NotNull TerminalTabState copy() {
        return new TerminalTabState(this);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof TerminalTabState)) {
            return false;
        }
        TerminalTabState that = (TerminalTabState) object;
        return Objects.equals(id, that.id)
            && Objects.equals(name, that.name)
            && Objects.equals(command, that.command);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, command);
    }
}
