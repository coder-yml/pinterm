package io.github.yaml.pinterm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PinTermCommandsTest {
    @Test
    public void rejectsLineBreaksAndNullBytes() {
        assertTrue(PinTermCommands.containsUnsafeControlChars("echo one\necho two"));
        assertTrue(PinTermCommands.containsUnsafeControlChars("echo one\recho two"));
        assertTrue(PinTermCommands.containsUnsafeControlChars("echo\0hi"));
        assertFalse(PinTermCommands.containsUnsafeControlChars("ssh host"));
        assertFalse(PinTermCommands.containsUnsafeControlChars(""));
        assertFalse(PinTermCommands.containsUnsafeControlChars(null));
    }

    @Test
    public void onlySafeNonBlankCommandsAreSendable() {
        assertTrue(PinTermCommands.isSafeToSend("ls"));
        assertFalse(PinTermCommands.isSafeToSend("  "));
        assertFalse(PinTermCommands.isSafeToSend("echo one\necho two"));
        assertFalse(PinTermCommands.isSafeToSend(null));
    }

    @Test
    public void migratesSingleLineLegacyShellScript() {
        assertEquals("echo hi", PinTermCommands.migrateLegacyShellScript("", "echo hi"));
        assertEquals("keep", PinTermCommands.migrateLegacyShellScript("keep", "echo hi"));
    }

    @Test
    public void dropsMultilineLegacyShellScriptInsteadOfExecutingIt() {
        assertEquals("", PinTermCommands.migrateLegacyShellScript("", "#!/bin/bash\necho hello"));
        assertEquals("", PinTermCommands.migrateLegacyShellScript(null, "echo one\recho two"));
    }
}
