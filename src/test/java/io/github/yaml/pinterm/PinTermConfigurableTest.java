package io.github.yaml.pinterm;

import com.intellij.openapi.options.ConfigurationException;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("deprecation")
public class PinTermConfigurableTest {
    @Test
    public void detectsLineBreaksInCommands() {
        assertFalse(PinTermConfigurable.containsLineBreak("ssh host"));
        assertTrue(PinTermConfigurable.containsLineBreak("echo one\necho two"));
        assertTrue(PinTermConfigurable.containsLineBreak("echo one\recho two"));
        assertTrue(PinTermConfigurable.containsLineBreak("echo\0hi"));
    }

    @Test
    public void validationMessageIncludesTabName() {
        try {
            PinTermConfigurable.validateSingleLineCommand("Legacy Script", "echo one\necho two");
        }
        catch (ConfigurationException exception) {
            assertEquals("Shell command for \"Legacy Script\" must be a single line.", exception.getMessage());
            return;
        }

        throw new AssertionError("Expected ConfigurationException");
    }

    @Test
    public void rejectsDuplicateTabNames() {
        Set<String> seenNames = new HashSet<>();
        try {
            PinTermConfigurable.validateUniqueTabName("Codex", seenNames);
            PinTermConfigurable.validateUniqueTabName("Codex", seenNames);
        }
        catch (ConfigurationException exception) {
            assertEquals("Tab name \"Codex\" is already used.", exception.getMessage());
            return;
        }

        throw new AssertionError("Expected ConfigurationException");
    }

    @Test
    public void allowsDistinctTabNames() throws ConfigurationException {
        Set<String> seenNames = new HashSet<>();
        PinTermConfigurable.validateUniqueTabName("Codex", seenNames);
        PinTermConfigurable.validateUniqueTabName("Cursor", seenNames);
        assertEquals(2, seenNames.size());
    }
}
