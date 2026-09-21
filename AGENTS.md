# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

PinTerm 是一个 IntelliJ Platform 插件（group 为 `io.github.yaml`，id 为 `io.github.yaml.pinterm`）。它把配置好的终端标签页打开到编辑器区域并固定，在会话进入运行态后可选地发送一行 shell 命令。它依赖 IDE 自带的 *Reworked Terminal*（`org.jetbrains.plugins.terminal`，since-build 为 `262`），因此代码中很多地方用到了没有公开等价物的内部/实验性终端 API。

## 命令

需要 Gradle 8.13+（使用 wrapper）。所有命令都需要本机安装 IntelliJ IDEA 2026.2+。

**JDK 分离（重要）：** 插件源码用 Java 25 工具链编译（`gradle.properties` 中 `java { toolchain { languageVersion = 25 } }`），但 **Gradle 守护进程本身必须跑在 JDK 21 上** —— Kotlin DSL 无法解析 Java 25.0.3 这种版本号。启动 gradlew 时让 `JAVA_HOME` 指向 JDK 21：

```bash
# 运行测试套件（JUnit 4）
JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew test

# 运行单个测试类
JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew test --tests "io.github.yaml.pinterm.PinTermOpenFlowTest"

# 构建可分发的插件 zip -> build/distributions/pinterm-0.1.0.zip
JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew buildPlugin

# 启动一个安装了本插件、带沙箱的 IDE
JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew runIde
```

**本地 IDE 路径与 CI。** IDEA 安装位置按顺序解析：`gradle.local.properties` 中的 `localIdePath=`、环境变量 `PINTERM_LOCAL_IDE`、再是 `-PlocalIdePath=`。显式配置的路径一定会被采用；否则仅当默认路径 `/Applications/IntelliJ IDEA.app` 存在时才用本地 IDE。**都不可用（例如 CI）时会退化为下载式平台** `intellijIdea(platformVersion)`，`platformVersion` 可用 `-PplatformVersion=` 覆盖，默认 `2026.2.3`。测试的 classpath 来自插件提供的 `intellijPlatformTestClasspath` 配置（含平台与 bundled 插件/模块），因此本地与 CI 两种模式都不再依赖从本地 IDE 抽取的 jar。`gradle.local.properties` 已被 gitignore —— 切勿把 IDE/JDK 路径提交进仓库。CI 上仍需自行准备 JDK 21（Gradle 守护进程）与 Java 25 工具链（编译）。

**CI 与发布。** `.github/workflows/ci.yml` 在 push/PR 上跑 `test` + `buildPlugin` 并上传 zip；`.github/workflows/release.yml` 在 `v*` tag（或手动触发）上跑 `test` → `buildPlugin` → `publishPlugin`，并在 tag 上用 `gh release create --notes-file` 创建 GitHub Release、附带 `build/distributions/pinterm-<version>.zip`（手动触发不建 Release）。tag 名 `v0.1.0` 会作为 `pluginVersion` 传入（手动触发则取 `gradle.properties`）。

**变更日志（单一来源）。** `CHANGELOG.md` 是发布说明的唯一来源，遵循 Keep a Changelog 格式，每个版本一个 `## [<version>]` 段。发布时该段用作 Git tag 的 Release notes；同时 `build.gradle.kts` 里的 `patchPluginXml.changeNotes` 会把同一段转成 HTML 写进构建产物 `plugin.xml` 的 `<change-notes>`（`plugin.xml` 里的同名元素只是占位/兜底）。发版时**必须**为新的 `pluginVersion` 在 `CHANGELOG.md` 顶部补一段，否则产物不带 change-notes、Release 会退化为 GitHub 自动生成的 notes。两个工作流都装 JDK 21 与 25，并用 `-Dorg.gradle.java.installations.fromEnv=JAVA_HOME_25_X64,JAVA_HOME_25_ARM64` 让 Gradle 找到 25 工具链。`build.gradle.kts` 已把 `PUBLISH_TOKEN`、`CERTIFICATE_CHAIN`、`PRIVATE_KEY`、`PRIVATE_KEY_PASSWORD` 映射到 `intellijPlatform` 的 `publishing`/`signing`；签名变量缺失时 `signPlugin` 跳过，发布未签名插件。

## 架构

### 打开并固定的流程
核心调用链是 `PinTermToolbarDropdownAction` → `PinTermService.openTerminalInEditor(TerminalTabState)` → `PinTermOpenFlow.openPinned(Platform, tab, workingDir)`。

`PinTermOpenFlow` 是一个 **纯的、与平台无关的流程编排器**（`src/main/java/io/github/yaml/pinterm/PinTermOpenFlow.java`）。它定义了 `Platform` 接口（createTab → detachTab → createEditorFile → openAndPin → terminalView），并约定回滚契约：如果 createTab 到 openAndPin 之间任何一步抛异常，OpenFlow 会调用 `closeTab` 以避免泄漏工具窗口里的标签页。真正的实现是 `PinTermService` 内部的 `EditorOpenPlatform` 内部类，它把每一步都委托给 `PinTermPlatformTerminals`。测试使用 `RecordingOpenPlatform`（`src/test/...` 里的假对象）来在不启动真实 IDE 的情况下断言调用顺序。

`PinTermPlatformTerminals` 是 **唯一接触 Reworked Terminal 内部/实验性 API 的类**：`TerminalToolWindowTabsManager`、`TerminalToolWindowTab`、`TerminalView`、`TerminalViewVirtualFile`，以及持久化 API `TerminalTabsStorage` / `TerminalSessionPersistedTab`。终端 API 的使用应只集中在这里。

### 命令派发（信任 + 会话状态）
打开之后，`PinTermService` 的内部类 `CommandExecutionSession` 会轮询 `TerminalView.getSessionState()`，直到会话变为 `Running`，再通过 `PinTermPlatformTerminals.sendExecutedText(view, command)` 发送命令（其内部调用 `view.createSendTextBuilder().shouldExecute().send(cmd)`）。轮询使用 `AppExecutorUtil`，超时时间 10 秒。

- **`PinTermCommandDispatch.decide(state)`** 把 `TerminalViewSessionState` 映射到 `SEND` / `FAIL_TERMINATED` / `WAIT`，方式是按状态类名的后缀匹配（`$Running`、`$Terminated`）。这是故意基于字符串匹配的，因为这些状态类是内部的。
- **信任闸门：** 只有在 `TrustedProjects.isProjectTrusted(project)` 为真时才会发送命令；未受信任的项目仍会打开终端，但绝不发送命令。
- **安全闸门：** `PinTermCommands.isSafeToSend` 会拒绝空命令，或任何包含 `\n`、`\r`、`\0` 的命令。`PinTermOpenFlow.hasCommandToSend` 和派发逻辑都会参考它。

### 归属跟踪与清理
- `PinTermKeys.OWNED` 是挂在 `TerminalViewVirtualFile` 的 UserData 上的 `Key<Boolean>`，用于标记本插件打开的文件。它 **无法在 IDE 重启后存活**（这是已知事实，不是 bug）。
- `PinTermService` 在 `openTerminalFiles`（`ConcurrentHashMap`，以 `TerminalViewVirtualFile` 为键）中跟踪已打开的文件。它监听 `FileEditorManagerListener.fileClosed` 和 `ProjectManagerListener.projectClosingBeforeSave` 来释放/销毁它们。
- 在项目关闭 / 插件卸载时（`PinTermDynamicPluginSupport.beforePluginUnload`），`closeAllOpenTerminals()` 会关闭归属本插件的编辑器文件 **并** 通过 `PinTermPlatformTerminals.removeStoredPluginTabs` 从 `TerminalTabsStorage` 中剥离本插件持久化的终端标签页，匹配依据仅限 **配置中出现过的 tab 显示名**（`PinTermTabNames`）。这就是 tab 名不能与用户自己的 Terminal 会话重名的原因 —— 重名可能在清理时误删用户的会话记录。
- **启动清理：** 重启后，先前固定的编辑器文件已消失（OWNED 丢失），但持久化的 tab 可能仍在。`scheduleStartupCleanup` 在 5 秒窗口内的 0/500/1500/3000/5000 毫秒处重试关闭，按名称移除被恢复的插件 tab。

### 持久化与默认值
- `PinTermSettings`（`PersistentStateComponent`，状态文件 `pinterm.xml`）保存 tab 列表。`roamingType = RoamingType.DISABLED`，因此配置命令不会同步到其他机器。`normalizeState` 会强制保证去空格后的唯一名称、为缺失 id 补 UUID、把空命令清洗为安全值，并丢弃任何含控制字符的命令。旧的单词行 `shellScript` 会在加载时迁移进 `command`；多行旧值会被丢弃。
- `PinTermIdeDefaultsService`（`pinterm-ide-defaults.xml`）会在 `appStarted` / `pluginLoaded` 时，把 IDE 全局的 **Show pinned tabs in a separate row** 开关精确地翻转一次（`pinnedTabsSeparateRowDefaultApplied` 标志）。它刻意改的是全局 UI 设置，而非 PinTerm 自己的 tab。
- `PinTermDefaults` 持有 `DEFAULT_TABS`（Codex/Claude/Grok，当没有 tab 时作为种子），以及 `displayTabName` 截断辅助方法。
- `TerminalTabState` 是共享数据模型（`id`、`name`、`command`，以及已废弃的 `shellScript`）。

### UI 入口
- `PinTermToolbarDropdownAction`（在 `plugin.xml` 中注册到 `MainToolbarRight`，图标 `/icons/pinterm.svg`）是一个 `ComboBoxAction`：列出每个已配置的 tab（通过 `PinTermService.openTerminalInEditor` 打开），外加一个 "Manage Tabs..." → `PinTermConfigurable`。
- `PinTermConfigurable` 是一个 Swing 的 `SearchableConfigurable`（Settings / Tools / PinTerm），包含一个 tab 的 `JBList` 和名称/命令编辑器。它在 `buildStateFromUi` 中校验命令必须是单行且名称唯一。

## 约定 / 注意点
- 代码风格：工具类都是包级私有（`final class X` 配私有构造器）；只有 `TerminalTabState`、`PinTermSettings`、`PinTermConfigurable`、`PinTermToolbarDropdownAction`、`PinTermService`、`PinTermIdeDefaultsService` 是 public 的。
- 测试是 **JUnit 4**（`org.junit.Test`、`useJUnit()`），位于 `src/test/java/io/github/yaml/pinterm/`。它们 **不会** 为了验证打开/固定逻辑而启动真实 IDE —— 它们借助 `RecordingOpenPlatform` 来驱动 `PinTermOpenFlow`。目前没有“真正打开 IDE 终端并固定”的集成测试（README 中已记载为已知限制）。
- `org.gradle.configuration-cache = true` 已开启；所有任务必须保证缓存安全（cache-safe）。
- 提升 `sinceBuild`（当前为 `262`）可能需要重新接触 `PinTermPlatformTerminals` 里的 Reworked Terminal 内部 API，以及 `PinTermService` 清理路径中的已废弃 `Disposer.isDisposed` 调用。
- 演示素材在 `docs/screenshots/`：`demo-real.gif` + `01-toolbar-dropdown.png` / `02-pinned-terminals.png` / `03-settings.png`，全部 **1200×760**，README 顶部与 Marketplace 插件页 Media **共用同一份**（不会出现两套比例互相漂移）。这些都是从真实录屏抽帧派生的，抽帧与 GIF 命令见 `docs/DEMO.md`「八」。GIF 由 4 段拼接而成，区间**刻意避开** Grok CLI 信任提示里 `/Users/<用户名>/...` 的真实路径——改区间后必须逐段复查。Media **不随插件包发布**，`publishPlugin` 不会上传，只能作者后台手工操作（清单见 `docs/DEMO.md`「八」）。
- 原始录屏（如 `docs/功能演示.mov`）**不入库**：`.gitignore` 已排除 `docs/*.mov` 与 `docs/*.mp4`，只提交派生的 `docs/screenshots/`。重做素材需要本地的原始录像。
