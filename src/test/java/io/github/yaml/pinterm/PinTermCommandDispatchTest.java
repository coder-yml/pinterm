package io.github.yaml.pinterm;

import com.intellij.terminal.frontend.view.TerminalViewSessionState;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PinTermCommandDispatchTest {
    @Test
    public void sendsWhenTheSessionIsRunning() {
        assertEquals(
            PinTermCommandDispatch.Decision.SEND,
            PinTermCommandDispatch.decide(TerminalViewSessionState.Running.INSTANCE)
        );
    }

    @Test
    public void failsWhenTheSessionTerminatedBeforeSend() {
        assertEquals(
            PinTermCommandDispatch.Decision.FAIL_TERMINATED,
            PinTermCommandDispatch.decide(TerminalViewSessionState.Terminated.INSTANCE)
        );
    }

    @Test
    public void waitsUntilTheSessionStarts() {
        assertEquals(
            PinTermCommandDispatch.Decision.WAIT,
            PinTermCommandDispatch.decide(TerminalViewSessionState.NotStarted.INSTANCE)
        );
        assertEquals(
            PinTermCommandDispatch.Decision.WAIT,
            PinTermCommandDispatch.decide(null)
        );
    }
}
