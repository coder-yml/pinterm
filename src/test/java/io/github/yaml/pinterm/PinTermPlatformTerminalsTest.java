package io.github.yaml.pinterm;

import org.jetbrains.plugins.terminal.settings.impl.TerminalSessionPersistedTab;
import org.jetbrains.plugins.terminal.startup.TerminalProcessType;
import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PinTermPlatformTerminalsTest {
    @Test
    public void dropsPersistedTabsWhoseNamesMatchConfiguredPluginTabs() {
        TerminalSessionPersistedTab pluginTab = persisted("Codex");
        TerminalSessionPersistedTab defaultPluginTab = persisted(PinTermDefaults.DEFAULT_TAB_NAME);
        TerminalSessionPersistedTab userTab = persisted("Local");

        List<TerminalSessionPersistedTab> remaining = PinTermPlatformTerminals.withoutPluginTabs(
            Arrays.asList(pluginTab, userTab, defaultPluginTab, null),
            Set.of(PinTermDefaults.DEFAULT_TAB_NAME, "Codex")
        );

        assertEquals(2, remaining.size());
        assertTrue(remaining.contains(userTab));
        assertTrue(remaining.contains(null));
    }

    @Test
    public void keepsUnrelatedPersistedTabs() {
        TerminalSessionPersistedTab userTab = persisted("zsh");

        List<TerminalSessionPersistedTab> remaining = PinTermPlatformTerminals.withoutPluginTabs(
            List.of(userTab),
            Set.of("Codex")
        );

        assertEquals(List.of(userTab), remaining);
    }

    @Test
    public void keepsDefaultNamedSessionsWhenTheyAreNotConfigured() {
        TerminalSessionPersistedTab defaultNamed = persisted(PinTermDefaults.DEFAULT_TAB_NAME);

        List<TerminalSessionPersistedTab> remaining = PinTermPlatformTerminals.withoutPluginTabs(
            List.of(defaultNamed),
            Set.of("Codex")
        );

        assertEquals(List.of(defaultNamed), remaining);
    }

    private static TerminalSessionPersistedTab persisted(String name) {
        return new TerminalSessionPersistedTab(
            name,
            true,
            List.of(),
            "/tmp",
            Map.of(),
            TerminalProcessType.SHELL
        );
    }
}
