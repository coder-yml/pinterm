package io.github.yaml.pinterm;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PinTermOpenFlowTest {
    @Test
    public void opensByCreatingDetachingThenOpeningAndPinningTheEditorFile() {
        RecordingOpenPlatform platform = new RecordingOpenPlatform();
        TerminalTabState tab = TerminalTabState.createNamed("Codex");
        tab.command = "echo hi";

        PinTermOpenFlow.OpenedSession session = PinTermOpenFlow.openPinned(
            platform,
            tab,
            "/tmp/project"
        );

        assertEquals(
            List.of(
                "createTab:/tmp/project:Codex",
                "detachTab:tab",
                "createEditorFile:tab",
                "openAndPin:file",
                "terminalView:tab"
            ),
            platform.calls
        );
        assertEquals("tab", session.terminalTab());
        assertEquals("file", session.editorFile());
        assertEquals("view", session.terminalView());
    }

    @Test
    public void closesTheToolWindowTabWhenDetachFails() {
        RecordingOpenPlatform platform = new RecordingOpenPlatform();
        platform.failDetach = true;
        TerminalTabState tab = TerminalTabState.createNamed("Codex");

        try {
            PinTermOpenFlow.openPinned(platform, tab, "/tmp/project");
            fail("Expected detach failure");
        }
        catch (IllegalStateException error) {
            assertEquals("detach failed", error.getMessage());
        }

        assertEquals(
            List.of(
                "createTab:/tmp/project:Codex",
                "detachTab:tab",
                "closeTab:tab"
            ),
            platform.calls
        );
    }

    @Test
    public void closesTheToolWindowTabWhenOpenAndPinFails() {
        RecordingOpenPlatform platform = new RecordingOpenPlatform();
        platform.failOpenAndPin = true;
        TerminalTabState tab = TerminalTabState.createNamed("Codex");

        try {
            PinTermOpenFlow.openPinned(platform, tab, "/tmp/project");
            fail("Expected open failure");
        }
        catch (IllegalStateException error) {
            assertEquals("open failed", error.getMessage());
        }

        assertEquals(
            List.of(
                "createTab:/tmp/project:Codex",
                "detachTab:tab",
                "createEditorFile:tab",
                "openAndPin:file",
                "closeTab:tab"
            ),
            platform.calls
        );
    }

    @Test
    public void sendsOnlySafeNonBlankCommands() {
        TerminalTabState blank = TerminalTabState.createNamed("Blank");
        blank.command = "  ";
        TerminalTabState multiline = TerminalTabState.createNamed("Script");
        multiline.command = "echo one\necho two";
        TerminalTabState configured = TerminalTabState.createNamed("Run");
        configured.command = "ls";

        assertFalse(PinTermOpenFlow.hasCommandToSend(blank));
        assertFalse(PinTermOpenFlow.hasCommandToSend(multiline));
        assertTrue(PinTermOpenFlow.hasCommandToSend(configured));
    }

    @Test
    public void reusesTheSameCreatedTabForEditorFileAndView() {
        RecordingOpenPlatform platform = new RecordingOpenPlatform();
        TerminalTabState tab = TerminalTabState.createNamed("Codex");

        PinTermOpenFlow.OpenedSession session = PinTermOpenFlow.openPinned(
            platform,
            tab,
            "/work"
        );

        assertSame(platform.createdTab, session.terminalTab());
        assertSame(platform.createdFile, session.editorFile());
        assertSame(platform.createdView, session.terminalView());
    }
}
