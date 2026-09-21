package io.github.yaml.pinterm;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PinTermEditorCleanupTest {
    @Test
    public void closesOwnedEditorFilesEvenWhenTheyAreNotTracked() {
        assertTrue(PinTermEditorCleanup.shouldCloseEditorFile(true, false, false));
        assertTrue(PinTermEditorCleanup.shouldCloseEditorFile(true, false, true));
    }

    @Test
    public void closesTrackedFilesOnlyWhenRequested() {
        assertFalse(PinTermEditorCleanup.shouldCloseEditorFile(false, true, false));
        assertTrue(PinTermEditorCleanup.shouldCloseEditorFile(false, true, true));
    }

    @Test
    public void keepsTrackedOwnedFilesDuringStartupCleanup() {
        assertFalse(PinTermEditorCleanup.shouldCloseEditorFile(true, true, false));
        assertTrue(PinTermEditorCleanup.shouldCloseEditorFile(true, true, true));
    }

    @Test
    public void keepsUnmarkedUntrackedFiles() {
        assertFalse(PinTermEditorCleanup.shouldCloseEditorFile(false, false, false));
        assertFalse(PinTermEditorCleanup.shouldCloseEditorFile(false, false, true));
    }
}
