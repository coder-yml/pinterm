package io.github.yaml.pinterm;

import com.intellij.terminal.frontend.view.TerminalViewSessionState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

final class PinTermCommandDispatch {
    enum Decision {
        SEND,
        FAIL_TERMINATED,
        WAIT
    }

    private PinTermCommandDispatch() {
    }

    static @NotNull Decision decide(@Nullable TerminalViewSessionState state) {
        if (state == null) {
            return Decision.WAIT;
        }
        return decideByClassName(state.getClass().getName());
    }

    static @NotNull Decision decideByClassName(@Nullable String sessionStateClassName) {
        if (sessionStateClassName == null) {
            return Decision.WAIT;
        }
        if (sessionStateClassName.endsWith("$Running")) {
            return Decision.SEND;
        }
        if (sessionStateClassName.endsWith("$Terminated")) {
            return Decision.FAIL_TERMINATED;
        }
        return Decision.WAIT;
    }
}
