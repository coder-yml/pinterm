package io.github.yaml.pinterm;

import com.intellij.openapi.util.UserDataHolderBase;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PinTermKeysTest {
    @Test
    public void treatsUnmarkedHoldersAsNotOwned() {
        UserDataHolderBase holder = new UserDataHolderBase();

        assertFalse(PinTermKeys.isOwned(holder));
        assertFalse(PinTermKeys.isOwned(null));
    }

    @Test
    public void marksAndRecognizesOwnedHolders() {
        UserDataHolderBase holder = new UserDataHolderBase();

        PinTermKeys.markOwned(holder);

        assertTrue(PinTermKeys.isOwned(holder));
    }
}
