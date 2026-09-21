package io.github.yaml.pinterm;

import com.intellij.ide.trustedProjects.TrustedProjects;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileEditorManagerListener;
import com.intellij.openapi.fileEditor.FileEditorManagerKeys;
import com.intellij.openapi.fileEditor.ex.FileEditorManagerEx;
import com.intellij.openapi.fileEditor.impl.EditorWindow;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.project.ProjectManagerListener;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.startup.StartupManager;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.terminal.frontend.editor.TerminalViewVirtualFile;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTab;
import com.intellij.terminal.frontend.toolwindow.TerminalToolWindowTabsManager;
import com.intellij.terminal.frontend.view.TerminalView;
import com.intellij.terminal.frontend.view.TerminalViewSessionState;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class PinTermService implements Disposable {
    private static final Logger LOG = Logger.getInstance(PinTermService.class);
    private static final String NOTIFICATION_GROUP_ID = "PinTerm";
    private static final int START_TIMEOUT_SECONDS = 10;
    private static final long STARTUP_CLEANUP_WINDOW_MILLIS = 5_000;
    private static final long[] STARTUP_CLEANUP_DELAYS_MILLIS = {0, 500, 1_500, 3_000, 5_000};

    private final Project project;
    private final Map<TerminalViewVirtualFile, Disposable> openTerminalFiles = new ConcurrentHashMap<>();
    private final List<ScheduledFuture<?>> startupCleanupTasks = new CopyOnWriteArrayList<>();
    private volatile long startupCleanupDeadlineMillis;

    public PinTermService(@NotNull Project project) {
        this.project = project;
        project.getMessageBus().connect(this).subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            new FileEditorManagerListener() {
                @Override
                public void fileOpened(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    trackTerminalFileIfNeeded(file);
                }

                @Override
                public void fileClosed(@NotNull FileEditorManager source, @NotNull VirtualFile file) {
                    if (!(file instanceof TerminalViewVirtualFile terminalFile)) {
                        return;
                    }
                    if (Boolean.TRUE.equals(file.getUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN))) {
                        return;
                    }
                    releaseTrackedFile(terminalFile);
                }
            }
        );
        ApplicationManager.getApplication().getMessageBus().connect(this).subscribe(
            ProjectManager.TOPIC,
            new ProjectManagerListener() {
                @Override
                public void projectClosingBeforeSave(@NotNull Project closingProject) {
                    if (closingProject == project) {
                        closeAllOpenTerminals();
                    }
                }
            }
        );

        StartupManager.getInstance(project).runAfterOpened(() ->
            ApplicationManager.getApplication().invokeLater(this::scheduleStartupCleanup)
        );
    }

    public void openTerminalInEditor(@NotNull TerminalTabState tab) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed()) {
                return;
            }

            String workingDirectory = project.getBasePath();
            if (workingDirectory == null) {
                workingDirectory = guessProjectPath(project);
            }
            if (workingDirectory == null) {
                LOG.warn("Unable to resolve working directory for tab " + tab.id);
                notifyFailure(tab.name, "Unable to resolve the project working directory.");
                return;
            }

            cancelStartupCleanup();
            try {
                PinTermOpenFlow.OpenedSession session = PinTermOpenFlow.openPinned(
                    new EditorOpenPlatform(tab),
                    tab,
                    workingDirectory
                );
                TerminalViewVirtualFile file = (TerminalViewVirtualFile) session.editorFile();
                runConfiguredCommandIfNeeded(
                    (TerminalView) session.terminalView(),
                    trackOpenedFile(file),
                    tab
                );
            }
            catch (Throwable error) {
                LOG.warn("Failed to open pinned terminal using tab " + tab.id, error);
                notifyFailure(tab.name, "Failed to open terminal in editor.");
            }
        });
    }

    private final class EditorOpenPlatform implements PinTermOpenFlow.Platform {
        private final TerminalTabState tab;

        private EditorOpenPlatform(@NotNull TerminalTabState tab) {
            this.tab = tab;
        }

        @Override
        public @NotNull Object createTab(@NotNull String workingDirectory, @NotNull String tabName) {
            return PinTermPlatformTerminals.createTab(project, workingDirectory, tabName);
        }

        @Override
        public void detachTab(@NotNull Object terminalTab) {
            TerminalToolWindowTab toolWindowTab = (TerminalToolWindowTab) terminalTab;
            TerminalToolWindowTabsManager tabsManager = TerminalToolWindowTabsManager.getInstance(project);
            try {
                PinTermPlatformTerminals.detachTab(tabsManager, toolWindowTab);
            }
            catch (Throwable error) {
                LOG.warn("Failed to detach PinTerm tab from tool window using tab " + tab.id, error);
                throw error;
            }
        }

        @Override
        public void closeTab(@NotNull Object terminalTab) {
            PinTermPlatformTerminals.closeTab(
                TerminalToolWindowTabsManager.getInstance(project),
                (TerminalToolWindowTab) terminalTab
            );
        }

        @Override
        public @NotNull Object createEditorFile(@NotNull Object terminalTab) {
            return PinTermPlatformTerminals.createEditorFile((TerminalToolWindowTab) terminalTab);
        }

        @Override
        public void openAndPin(@NotNull Object editorFile) {
            TerminalViewVirtualFile file = (TerminalViewVirtualFile) editorFile;
            PinTermKeys.markOwned(file);
            trackOpenedFile(file);
            file.putUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN, Boolean.TRUE);
            try {
                FileEditorManagerEx editorManager = FileEditorManagerEx.getInstanceEx(project);
                boolean opened = false;
                for (int attempt = 0; attempt < PinTermEditorOpen.MAX_OPEN_ATTEMPTS; attempt++) {
                    editorManager.openFile(file, true);
                    opened = PinTermEditorOpen.isOpenSuccessful(editorManager.isFileOpen(file));
                    if (!PinTermEditorOpen.shouldRetryOpen(opened, attempt)) {
                        break;
                    }
                }
                if (!opened) {
                    throw new IllegalStateException("Editor did not open the terminal file");
                }
                pinOpenedFile(editorManager, file);
            }
            finally {
                file.putUserData(FileEditorManagerKeys.CLOSING_TO_REOPEN, null);
            }
        }

        @Override
        public @NotNull Object terminalView(@NotNull Object terminalTab) {
            return PinTermPlatformTerminals.viewOf((TerminalToolWindowTab) terminalTab);
        }
    }

    public void closeAllOpenTerminals() {
        if (project.isDisposed()) {
            openTerminalFiles.clear();
            return;
        }

        Runnable closeTask = () -> {
            if (project.isDisposed()) {
                openTerminalFiles.clear();
                return;
            }

            FileEditorManagerEx editorManager = FileEditorManagerEx.getInstanceEx(project);
            Set<String> configuredTabNames = getConfiguredTabNames();
            closeEditorTerminalFiles(editorManager, true);
            clearPersistedPluginTerminalTabs(configuredTabNames);
        };

        if (ApplicationManager.getApplication().isDispatchThread()) {
            closeTask.run();
        }
        else {
            ApplicationManager.getApplication().invokeAndWait(closeTask);
        }
    }

    @Override
    public void dispose() {
        cancelStartupCleanup();
        closeAllOpenTerminals();
        for (TerminalViewVirtualFile file : new ArrayList<>(openTerminalFiles.keySet())) {
            releaseTrackedFile(file);
        }
    }

    private @NotNull Disposable trackOpenedFile(@NotNull TerminalViewVirtualFile file) {
        PinTermKeys.markOwned(file);
        Disposable existing = openTerminalFiles.get(file);
        if (existing != null) {
            return existing;
        }

        Disposable fileDisposable = Disposer.newDisposable("PinTermEditorTerminal");
        Disposable previous = openTerminalFiles.putIfAbsent(file, fileDisposable);
        if (previous != null) {
            Disposer.dispose(fileDisposable);
            return previous;
        }

        Disposer.register(this, fileDisposable);
        return fileDisposable;
    }

    private void releaseTrackedFile(@NotNull TerminalViewVirtualFile file) {
        Disposable disposable = openTerminalFiles.remove(file);
        if (disposable != null && !Disposer.isDisposed(disposable)) {
            Disposer.dispose(disposable);
        }
    }

    private void pinOpenedFile(@NotNull FileEditorManagerEx editorManager, @NotNull VirtualFile file) {
        EditorWindow window = null;
        for (EditorWindow candidate : editorManager.getWindows()) {
            if (candidate.isFileOpen(file)) {
                window = candidate;
                break;
            }
        }
        if (window == null) {
            window = editorManager.getCurrentWindow();
        }

        if (window == null || !PinTermEditorOpen.canPinInWindow(true, window.isFileOpen(file))) {
            LOG.warn("Opened terminal editor tab but could not pin it yet");
            return;
        }

        try {
            window.setFilePinned(file, true);
        }
        catch (IllegalArgumentException error) {
            LOG.warn("Opened terminal editor tab but could not pin it yet", error);
        }
    }

    private void runConfiguredCommandIfNeeded(
        @NotNull TerminalView terminalView,
        @NotNull Disposable parentDisposable,
        @NotNull TerminalTabState tab
    ) {
        if (!PinTermOpenFlow.hasCommandToSend(tab)) {
            return;
        }
        if (!TrustedProjects.isProjectTrusted(project)) {
            notifyFailure(tab.name, "The project is not trusted, so the configured command was not sent.");
            return;
        }
        String command = tab.command;

        try {
            new CommandExecutionSession(terminalView, parentDisposable, tab.name, command).start();
        }
        catch (Throwable error) {
            LOG.warn("Failed to start configured terminal command execution", error);
            notifyFailure(tab.name, "Failed to start terminal command.");
        }
    }

    private void notifyFailure(@NotNull String tabName, @NotNull String message) {
        Notification notification = NotificationGroupManager.getInstance()
            .getNotificationGroup(NOTIFICATION_GROUP_ID)
            .createNotification(PinTermDefaults.displayTabName(tabName), message, NotificationType.WARNING);
        notification.notify(project);
    }

    private static @Nullable String guessProjectPath(@NotNull Project project) {
        VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
        return projectDir != null ? projectDir.getPath() : null;
    }

    private void cleanupRestoredPluginTerminals() {
        if (project.isDisposed()) {
            return;
        }

        try {
            Set<String> configuredTabNames = getConfiguredTabNames();
            FileEditorManagerEx editorManager = FileEditorManagerEx.getInstanceEx(project);
            closeEditorTerminalFiles(editorManager, false);
            clearPersistedPluginTerminalTabs(configuredTabNames);
        }
        catch (Throwable error) {
            LOG.warn("Failed to cleanup restored PinTerm tabs after project open", error);
        }
    }

    private void scheduleStartupCleanup() {
        if (project.isDisposed()) {
            return;
        }

        cancelStartupCleanup();
        startupCleanupDeadlineMillis = System.currentTimeMillis() + STARTUP_CLEANUP_WINDOW_MILLIS;
        for (long delayMillis : STARTUP_CLEANUP_DELAYS_MILLIS) {
            startupCleanupTasks.add(AppExecutorHolder.schedule(() ->
                ApplicationManager.getApplication().invokeLater(this::cleanupRestoredPluginTerminals),
                delayMillis
            ));
        }
        startupCleanupTasks.add(AppExecutorHolder.schedule(
            () -> startupCleanupDeadlineMillis = 0,
            STARTUP_CLEANUP_WINDOW_MILLIS
        ));
    }

    private void cancelStartupCleanup() {
        startupCleanupDeadlineMillis = 0;
        for (ScheduledFuture<?> task : startupCleanupTasks) {
            task.cancel(false);
        }
        startupCleanupTasks.clear();
    }

    private void trackTerminalFileIfNeeded(@NotNull VirtualFile file) {
        if (!(file instanceof TerminalViewVirtualFile terminalFile)) {
            return;
        }
        if (!PinTermKeys.isOwned(terminalFile)) {
            return;
        }
        if (isStartupCleanupActive() && !openTerminalFiles.containsKey(terminalFile)) {
            scheduleCloseRestoredTerminalFile(terminalFile);
            return;
        }

        trackOpenedFile(terminalFile);
    }

    private boolean isStartupCleanupActive() {
        return System.currentTimeMillis() <= startupCleanupDeadlineMillis;
    }

    private void scheduleCloseRestoredTerminalFile(@NotNull TerminalViewVirtualFile file) {
        ApplicationManager.getApplication().invokeLater(() -> {
            if (project.isDisposed() || openTerminalFiles.containsKey(file)) {
                return;
            }
            if (!PinTermKeys.isOwned(file)) {
                return;
            }

            try {
                FileEditorManagerEx editorManager = FileEditorManagerEx.getInstanceEx(project);
                closeEditorTerminalFile(editorManager, file);
            }
            catch (Throwable error) {
                LOG.warn("Failed to close restored PinTerm editor tab during startup cleanup", error);
            }
        });
    }

    private void closeEditorTerminalFiles(
        @NotNull FileEditorManagerEx editorManager,
        boolean includeTrackedFiles
    ) {
        Set<TerminalViewVirtualFile> filesToClose = new LinkedHashSet<>();
        for (VirtualFile file : editorManager.getOpenFiles()) {
            if (!(file instanceof TerminalViewVirtualFile terminalFile)) {
                continue;
            }
            boolean tracked = openTerminalFiles.containsKey(terminalFile);
            if (PinTermEditorCleanup.shouldCloseEditorFile(
                PinTermKeys.isOwned(terminalFile),
                tracked,
                includeTrackedFiles
            )) {
                filesToClose.add(terminalFile);
            }
        }
        if (includeTrackedFiles) {
            for (TerminalViewVirtualFile file : openTerminalFiles.keySet()) {
                filesToClose.add(file);
            }
        }

        for (TerminalViewVirtualFile file : filesToClose) {
            closeEditorTerminalFile(editorManager, file);
        }
    }

    private void closeEditorTerminalFile(
        @NotNull FileEditorManagerEx editorManager,
        @NotNull TerminalViewVirtualFile file
    ) {
        try {
            editorManager.closeFile(file);
        }
        catch (Throwable error) {
            LOG.warn("Failed to close PinTerm editor tab during cleanup", error);
        }
        finally {
            releaseTrackedFile(file);
        }
    }

    private void clearPersistedPluginTerminalTabs(@NotNull Set<String> configuredTabNames) {
        try {
            PinTermPlatformTerminals.removeStoredPluginTabs(project, configuredTabNames);
        }
        catch (Throwable error) {
            LOG.warn("Failed to clear persisted PinTerm tabs", error);
        }
    }

    private @NotNull Set<String> getConfiguredTabNames() {
        return PinTermTabNames.fromTabs(PinTermSettings.getInstance().getTabs());
    }

    private final class CommandExecutionSession {
        private final TerminalView terminalView;
        private final Disposable parentDisposable;
        private final String tabName;
        private final String command;
        private final Disposable disposable;

        private boolean completed;
        private ScheduledFuture<?> startupPollFuture;
        private ScheduledFuture<?> startupTimeoutFuture;

        private CommandExecutionSession(
            @NotNull TerminalView terminalView,
            @NotNull Disposable parentDisposable,
            @NotNull String tabName,
            @NotNull String command
        ) {
            this.terminalView = terminalView;
            this.parentDisposable = parentDisposable;
            this.tabName = tabName;
            this.command = command;
            this.disposable = Disposer.newDisposable("PinTermCommandExecution");
        }

        void start() {
            Disposer.register(parentDisposable, disposable);
            Disposer.register(disposable, (Disposable) this::disposeResources);

            scheduleStartupWait();
        }

        private void scheduleStartupWait() {
            startupPollFuture = AppExecutorHolder.schedule(() -> {
                ApplicationManager.getApplication().invokeLater(this::pollSessionState);
            }, 150);

            if (startupTimeoutFuture == null) {
                startupTimeoutFuture = AppExecutorHolder.schedule(() -> {
                    ApplicationManager.getApplication().invokeLater(() -> {
                        if (Disposer.isDisposed(disposable) || completed) {
                            return;
                        }
                        fail("Terminal session did not start within " + START_TIMEOUT_SECONDS + " seconds.");
                    });
                }, START_TIMEOUT_SECONDS * 1000L);
            }
        }

        private void pollSessionState() {
            if (Disposer.isDisposed(disposable)) {
                return;
            }
            TerminalViewSessionState state = terminalView.getSessionState().getValue();
            PinTermCommandDispatch.Decision decision = PinTermCommandDispatch.decide(state);
            if (decision == PinTermCommandDispatch.Decision.SEND) {
                sendCommand();
                return;
            }
            if (decision == PinTermCommandDispatch.Decision.FAIL_TERMINATED) {
                fail("Terminal session ended before the command could start.");
                return;
            }
            scheduleStartupWait();
        }

        private void disposeResources() {
            if (startupPollFuture != null) {
                startupPollFuture.cancel(false);
                startupPollFuture = null;
            }
            if (startupTimeoutFuture != null) {
                startupTimeoutFuture.cancel(false);
                startupTimeoutFuture = null;
            }
        }

        private void fail(@NotNull String reason) {
            if (completed || Disposer.isDisposed(disposable)) {
                return;
            }
            completed = true;

            LOG.warn("PinTerm command failed for tab '" + tabName + "': " + reason);
            notifyFailure(tabName, reason);
            Disposer.dispose(disposable);
        }

        private void sendCommand() {
            if (completed || Disposer.isDisposed(disposable)) {
                return;
            }

            if (!PinTermCommands.isSafeToSend(command)) {
                fail("The configured command is not a single line.");
                return;
            }

            try {
                PinTermPlatformTerminals.sendExecutedText(terminalView, command);
                completed = true;
                Disposer.dispose(disposable);
            }
            catch (Throwable error) {
                LOG.warn("Failed to send terminal command", error);
                fail("Failed to send the command.");
            }
        }
    }

    private static final class AppExecutorHolder {
        private static ScheduledFuture<?> schedule(@NotNull Runnable runnable, long delayMillis) {
            return com.intellij.util.concurrency.AppExecutorUtil.getAppScheduledExecutorService()
                .schedule(runnable, delayMillis, TimeUnit.MILLISECONDS);
        }
    }
}
