package io.github.yaml.pinterm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PinTermDefaultsTest {
    @Test
    public void usesDefaultNameForBlankDisplayNames() {
        assertEquals(PinTermDefaults.DEFAULT_TAB_NAME, PinTermDefaults.displayTabName(null));
        assertEquals(PinTermDefaults.DEFAULT_TAB_NAME, PinTermDefaults.displayTabName("  "));
        assertEquals("Codex", PinTermDefaults.displayTabName(" Codex "));
    }

    @Test
    public void truncatesLongDisplayNamesForNotifications() {
        String longName = "n".repeat(PinTermDefaults.MAX_DISPLAY_TAB_NAME_LENGTH + 20);
        String displayed = PinTermDefaults.displayTabName(longName);

        assertEquals(PinTermDefaults.MAX_DISPLAY_TAB_NAME_LENGTH, displayed.length());
        assertTrue(displayed.endsWith("..."));
    }
}
