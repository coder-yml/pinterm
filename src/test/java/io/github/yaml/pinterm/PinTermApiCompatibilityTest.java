package io.github.yaml.pinterm;

import com.intellij.terminal.frontend.editor.TerminalViewVirtualFile;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager;
import com.intellij.terminal.frontend.view.TerminalView;
import org.jetbrains.plugins.terminal.settings.impl.TerminalTabsStorage;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class PinTermApiCompatibilityTest {
    @Test
    public void detachTabOnTheInstalledIdeReturnsVoid() throws Exception {
        Method detachTab = TerminalToolWindowTabsManager.class.getMethod(
            "detachTab",
            TerminalToolWindowTab.class
        );
        Method shippedDetach = PinTermPlatformTerminals.class.getDeclaredMethod(
            "detachTab",
            TerminalToolWindowTabsManager.class,
            TerminalToolWindowTab.class
        );

        assertEquals(void.class, detachTab.getReturnType());
        assertEquals(detachTab.getReturnType(), shippedDetach.getReturnType());
        assertEquals(detachTab.getParameterTypes()[0], shippedDetach.getParameterTypes()[1]);
    }

    @Test
    public void shippedCreateEditorFileMatchesThe262Constructor() throws Exception {
        Constructor<?> constructor = TerminalViewVirtualFile.class.getConstructors()[0];
        Method createEditorFile = PinTermPlatformTerminals.class.getDeclaredMethod(
            "createEditorFile",
            TerminalToolWindowTab.class
        );

        assertEquals(1, constructor.getParameterCount());
        assertEquals(TerminalToolWindowTab.class, constructor.getParameterTypes()[0]);
        assertEquals(TerminalViewVirtualFile.class, createEditorFile.getReturnType());
        assertEquals(constructor.getParameterTypes()[0], createEditorFile.getParameterTypes()[0]);
    }

    @Test
    public void shippedSendAndViewUseTheInstalledTerminalTypes() throws Exception {
        Method sendExecutedText = PinTermPlatformTerminals.class.getDeclaredMethod(
            "sendExecutedText",
            TerminalView.class,
            String.class
        );
        Method viewOf = PinTermPlatformTerminals.class.getDeclaredMethod(
            "viewOf",
            TerminalToolWindowTab.class
        );

        assertEquals(void.class, sendExecutedText.getReturnType());
        assertEquals(TerminalView.class, viewOf.getReturnType());
        assertNotNull(TerminalView.class.getMethod("createSendTextBuilder"));
    }

    @Test
    public void persistedTabsLiveOnTheReworkedSettingsStorage() {
        assertEquals(
            "org.jetbrains.plugins.terminal.settings.impl.TerminalTabsStorage",
            TerminalTabsStorage.class.getName()
        );
    }
}
