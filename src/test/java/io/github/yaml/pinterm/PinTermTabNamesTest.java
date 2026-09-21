package io.github.yaml.pinterm;

import org.junit.Test;

import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PinTermTabNamesTest {
    @Test
    public void doesNotAlwaysTreatTheDefaultNameAsAPluginTab() {
        Set<String> configuredNames = PinTermTabNames.fromTabs(List.of(TerminalTabState.createNamed("Codex")));

        assertFalse(PinTermTabNames.isPinTermTabName(PinTermDefaults.DEFAULT_TAB_NAME, configuredNames));
    }

    @Test
    public void matchesTheDefaultNameOnlyWhenConfigured() {
        Set<String> configuredNames = PinTermTabNames.fromTabs(
            List.of(TerminalTabState.createNamed(PinTermDefaults.DEFAULT_TAB_NAME))
        );

        assertTrue(PinTermTabNames.isPinTermTabName(PinTermDefaults.DEFAULT_TAB_NAME, configuredNames));
    }

    @Test
    public void matchesConfiguredTabName() {
        Set<String> configuredNames = PinTermTabNames.fromTabs(List.of(TerminalTabState.createNamed("Codex")));

        assertTrue(PinTermTabNames.isPinTermTabName("Codex", configuredNames));
    }

    @Test
    public void trimsPersistedTabNameBeforeMatching() {
        Set<String> configuredNames = PinTermTabNames.fromTabs(List.of(TerminalTabState.createNamed("Cursor")));

        assertTrue(PinTermTabNames.isPinTermTabName("  Cursor  ", configuredNames));
    }

    @Test
    public void keepsNonPluginTabNames() {
        Set<String> configuredNames = PinTermTabNames.fromTabs(List.of(TerminalTabState.createNamed("Codex")));

        assertFalse(PinTermTabNames.isPinTermTabName("Local", configuredNames));
    }

    @Test
    public void keepsBlankAndNullNames() {
        Set<String> configuredNames = PinTermTabNames.fromTabs(List.of(TerminalTabState.createNamed("Codex")));

        assertFalse(PinTermTabNames.isPinTermTabName("", configuredNames));
        assertFalse(PinTermTabNames.isPinTermTabName("   ", configuredNames));
        assertFalse(PinTermTabNames.isPinTermTabName(null, configuredNames));
    }
}
