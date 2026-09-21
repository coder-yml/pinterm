package io.github.yaml.pinterm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PinTermToolbarDropdownActionTest {
    @Test
    public void enablesTheActionWhenAProjectIsOpenAndTabsExist() {
        assertTrue(PinTermToolbarDropdownAction.isEnabledFor(true, true));
    }

    @Test
    public void disablesTheActionWithoutAProject() {
        assertFalse(PinTermToolbarDropdownAction.isEnabledFor(false, true));
    }

    @Test
    public void disablesTheActionWhenNoTabsAreConfigured() {
        assertFalse(PinTermToolbarDropdownAction.isEnabledFor(true, false));
    }

    @Test
    public void keepsTheToolbarButtonIconOnly() {
        assertEquals("", PinTermToolbarDropdownAction.toolbarButtonText());
    }
}
