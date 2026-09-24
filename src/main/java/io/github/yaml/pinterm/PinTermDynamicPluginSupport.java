package io.github.yaml.pinterm;

import com.intellij.ide.plugins.DynamicPluginListener;
import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.jetbrains.annotations.NotNull;

public final class PinTermDynamicPluginSupport implements DynamicPluginListener {
    private static final PluginId PLUGIN_ID = PluginId.getId("io.github.yaml.pinterm");

    @Override
    public void beforePluginUnload(@NotNull IdeaPluginDescriptor pluginDescriptor, boolean isUpdate) {
        if (!PLUGIN_ID.equals(pluginDescriptor.getPluginId())) {
            return;
        }

        for (Project project : ProjectManager.getInstance().getOpenProjects()) {
            if (project.isDisposed()) {
                continue;
            }

            project.getService(PinTermService.class).closeAllOpenTerminals();
        }
    }
}
