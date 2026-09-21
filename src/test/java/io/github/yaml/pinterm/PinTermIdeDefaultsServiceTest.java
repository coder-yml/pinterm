package io.github.yaml.pinterm;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PinTermIdeDefaultsServiceTest {
    @Test
    public void marksPinnedTabsSeparateRowDefaultOnlyOnce() {
        PinTermIdeDefaultsService.ServiceState state = new PinTermIdeDefaultsService.ServiceState();

        assertTrue(PinTermIdeDefaultsService.markPinnedTabsSeparateRowDefaultApplied(state));
        assertTrue(state.pinnedTabsSeparateRowDefaultApplied);
        assertFalse(PinTermIdeDefaultsService.markPinnedTabsSeparateRowDefaultApplied(state));
    }
}
