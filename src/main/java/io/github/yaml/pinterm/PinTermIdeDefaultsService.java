package io.github.yaml.pinterm;

import com.intellij.ide.ui.UISettings;
import com.intellij.ide.ui.UISettingsState;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import org.jetbrains.annotations.NotNull;

@State(name = "PinTermIdeDefaults", storages = @Storage("pinterm-ide-defaults.xml"))
public final class PinTermIdeDefaultsService implements PersistentStateComponent<PinTermIdeDefaultsService.ServiceState> {
    private ServiceState state = new ServiceState();

    public static PinTermIdeDefaultsService getInstance() {
        return ApplicationManager.getApplication().getService(PinTermIdeDefaultsService.class);
    }

    public PinTermIdeDefaultsService() {
        ApplicationManager.getApplication().invokeLater(this::applyEditorTabDefaultsIfNeeded);
    }

    @Override
    public @NotNull ServiceState getState() {
        return state;
    }

    @Override
    public void loadState(@NotNull ServiceState state) {
        this.state = state;
    }

    private void applyEditorTabDefaultsIfNeeded() {
        if (!markPinnedTabsSeparateRowDefaultApplied(state)) {
            return;
        }

        UISettings uiSettings = UISettings.getInstance();
        UISettingsState uiState = uiSettings.getState();
        if (!uiState.getShowPinnedTabsInASeparateRow()) {
            uiState.setShowPinnedTabsInASeparateRow(true);
            uiSettings.fireUISettingsChanged();
        }
    }

    static boolean markPinnedTabsSeparateRowDefaultApplied(@NotNull ServiceState state) {
        if (state.pinnedTabsSeparateRowDefaultApplied) {
            return false;
        }

        state.pinnedTabsSeparateRowDefaultApplied = true;
        return true;
    }

    public static final class ServiceState {
        public boolean pinnedTabsSeparateRowDefaultApplied;
    }
}
