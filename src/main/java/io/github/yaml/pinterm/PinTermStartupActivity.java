package io.github.yaml.pinterm;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** DumbAware so startup cleanup is not deferred until indexing finishes. */
public final class PinTermStartupActivity implements ProjectActivity, DumbAware {
    @Override
    public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (!project.isDisposed()) {
                project.getService(PinTermService.class).scheduleStartupCleanup();
            }
        });
        return Unit.INSTANCE;
    }
}
