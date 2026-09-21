package io.github.yaml.pinterm;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@SuppressWarnings("deprecation")
public class PinTermSettingsTest {
    @Test
    public void createsCodexClaudeAndGrokTabsByDefault() {
        PinTermSettings.State state = PinTermSettings.createDefaultState();

        assertEquals(3, state.tabs.size());
        assertEquals("Codex", state.tabs.get(0).name);
        assertEquals("codex", state.tabs.get(0).command);
        assertEquals("Claude", state.tabs.get(1).name);
        assertEquals("claude", state.tabs.get(1).command);
        assertEquals("Grok", state.tabs.get(2).name);
        assertEquals("grok", state.tabs.get(2).command);
        assertNotNull(state.tabs.get(0).id);
        assertNotNull(state.tabs.get(1).id);
        assertNotNull(state.tabs.get(2).id);
    }

    @Test
    public void createsDefaultTabsWhenStateIsEmpty() {
        PinTermSettings settings = new PinTermSettings();
        settings.loadState(new PinTermSettings.State());

        assertEquals(3, settings.getTabs().size());
        assertEquals("Codex", settings.getTabs().get(0).name);
        assertEquals("codex", settings.getTabs().get(0).command);
        assertEquals("Claude", settings.getTabs().get(1).name);
        assertEquals("claude", settings.getTabs().get(1).command);
        assertEquals("Grok", settings.getTabs().get(2).name);
        assertEquals("grok", settings.getTabs().get(2).command);
    }

    @Test
    public void normalizesBlankTabNameWithoutUsingTheDefaultCleanupName() {
        PinTermSettings settings = new PinTermSettings();
        PinTermSettings.State state = new PinTermSettings.State();
        TerminalTabState tab = new TerminalTabState();
        tab.name = "   ";
        tab.command = "echo hi";
        state.tabs.add(tab);

        settings.loadState(state);

        assertEquals(PinTermDefaults.UNNAMED_TAB_NAME, settings.getTabs().get(0).name);
        assertEquals("echo hi", settings.getTabs().get(0).command);
    }

    @Test
    public void deduplicatesLoadedTabNames() {
        PinTermSettings settings = new PinTermSettings();
        PinTermSettings.State state = new PinTermSettings.State();
        state.tabs.add(TerminalTabState.createNamed("Codex"));
        state.tabs.add(TerminalTabState.createNamed("Codex"));

        settings.loadState(state);

        assertEquals("Codex", settings.getTabs().get(0).name);
        assertEquals("Codex 2", settings.getTabs().get(1).name);
    }

    @Test
    public void preservesConfiguredCommandWhitespace() {
        PinTermSettings settings = new PinTermSettings();
        PinTermSettings.State state = new PinTermSettings.State();
        TerminalTabState tab = TerminalTabState.createNamed("Remote");
        tab.command = "  ssh host  ";
        state.tabs.add(tab);

        settings.loadState(state);

        assertEquals("  ssh host  ", settings.getTabs().get(0).command);
    }

    @Test
    public void migratesLegacyShellScriptToCommand() {
        PinTermSettings settings = new PinTermSettings();
        PinTermSettings.State state = new PinTermSettings.State();
        TerminalTabState tab = TerminalTabState.createNamed("Legacy");
        tab.shellScript = "echo hi";
        state.tabs.add(tab);

        settings.loadState(state);

        TerminalTabState loadedTab = settings.getTabs().get(0);
        assertEquals("echo hi", loadedTab.command);
        assertNull(loadedTab.shellScript);
    }

    @Test
    public void findsConfiguredTabByIdAndIgnoresBlankIds() {
        PinTermSettings settings = new PinTermSettings();
        PinTermSettings.State state = new PinTermSettings.State();
        TerminalTabState tab = TerminalTabState.createNamed("Codex");
        tab.command = "echo hi";
        state.tabs.add(tab);
        settings.loadState(state);

        TerminalTabState found = settings.findTabById(tab.id);
        assertNotNull(found);
        assertEquals("Codex", found.name);
        assertEquals("echo hi", found.command);
        assertNull(settings.findTabById(" "));
        assertNull(settings.findTabById("missing"));
    }

    @Test
    public void dropsMigratedMultiLineCommandsInsteadOfKeepingThemExecutable() {
        PinTermSettings settings = new PinTermSettings();
        PinTermSettings.State state = new PinTermSettings.State();
        TerminalTabState tab = TerminalTabState.createNamed("Script");
        tab.shellScript = "#!/bin/bash\n  echo hello\n\n";
        state.tabs.add(tab);

        settings.loadState(state);

        assertEquals("", settings.getTabs().get(0).command);
        assertNull(settings.getTabs().get(0).shellScript);
    }

    @Test
    public void clearsLoadedCommandsThatContainLineBreaks() {
        PinTermSettings settings = new PinTermSettings();
        PinTermSettings.State state = new PinTermSettings.State();
        TerminalTabState tab = TerminalTabState.createNamed("Script");
        tab.command = "echo one\necho two";
        state.tabs.add(tab);

        settings.loadState(state);

        assertEquals("", settings.getTabs().get(0).command);
    }
}
