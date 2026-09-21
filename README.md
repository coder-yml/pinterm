# PinTerm

PinTerm is an IntelliJ Platform plugin that opens configured terminal tabs in the editor, pins them, and can send a one-line shell command once the session is running.

PinTerm 是一个 IntelliJ Platform 插件：把预设终端打开到编辑器区域、自动固定标签，并在会话就绪后按需发送一行 shell 命令。首次安装时会打开 IDE 全局的「固定标签单独一行」，让钉住的终端和普通编辑器标签分开。

PinTerm is not a sandbox. Installing it gives the plugin the same privileges as the IDE. Clicking a tab can run a configured command as the current user.

## 功能

- 主工具栏右侧提供仅图标的下拉入口。
- 在 `Settings / Tools / PinTerm` 中维护多个终端标签（名称 + 单行命令）。
- 将选中的终端打开到编辑器区域，而不是留在 Terminal 工具窗口。
- 自动固定打开的终端标签。
- 首次安装时启用 **Show pinned tabs in a separate row**（只改一次，之后尊重用户自己的选择）。
- 终端进入运行态后，可发送配置的单行命令。不可信项目不会发送命令。
- 项目关闭或插件卸载前，清理由 PinTerm 打开的终端。

## 要求

- IntelliJ IDEA 2026.2 或更新（build `262+`）
- Reworked Terminal（IDE 自带 Terminal 插件）
- 构建需要本机已安装对应版本的 IntelliJ IDEA
- 编译插件源码需要 JDK 25（本机 IDEA 2026.2 的平台字节码是 Java 25）
- 运行 Gradle 8.13 守护进程请用 JDK 21（Kotlin DSL 解析不了 Java 25.0.3 这类版本号）

## 使用

1. 安装插件后，工具栏右侧会出现 PinTerm 图标。首次安装下拉里有 **Codex**（`codex`）、**Claude**（`claude`）、**Grok**（`grok`）。
2. 打开 `Settings / Tools / PinTerm`，新增或编辑 tab：
   - **Tab Name**：显示名，保存时不能为空、不能重复。
   - **Shell Command**：可选，必须是单行。
3. 从工具栏下拉选择一个 tab，终端会打开在编辑器中并固定。
4. 如需固定标签单独成行：`Settings / Editor / General / Editor Tabs` → **Show pinned tabs in a separate row**（插件首次启动会帮你打开）。

不要把 PinTerm tab 起成和普通 Terminal 会话相同的名字。项目关闭时，插件会按**当前配置里的 tab 显示名**清理 Terminal 的持久化记录。编辑器里的标签只关带 PinTerm 标记的文件；该标记过不了 IDE 重启，所以崩溃后留在编辑器里的终端不会再被启动清理关掉。

## 风险与信任模型

1. **命令即执行**：`Shell Command` 在终端 Running 后原样 `shouldExecute` 发送，权限 = 当前用户。插件不解释、不沙箱。
2. **`pinterm.xml` 是可信输入**：能改该文件就能改将要执行的命令。加载和发送都会拒绝换行和空字符。不要把密钥写进 command。
3. **命令不漫游**：`pinterm.xml` 关闭了 Settings Sync 漫游，避免把可执行命令同步到其它机器。
4. **cwd = 当前项目** 的 `basePath`（或 `guessProjectDir`），不是 tab 级配置。换项目会用同一条命令、不同工作目录。
5. **不可信项目**：终端仍会打开，但不会发送配置的命令。
6. **持久化清理按 tab 显示名**：只匹配**当前配置里出现过的名字**。不要和普通 Terminal 同名，否则关项目 / 卸插件可能丢掉用户自己的会话记录。
7. **首次安装改的是 IDE 全局**「固定标签单独一行」，不是 PinTerm 自己的 tab。只改一次；改回路径：`Settings / Editor / General / Editor Tabs`。
8. 构建默认本机 `/Applications/IntelliJ IDEA.app`，用环境变量 / `-P` / `gradle.local.properties` 覆盖，路径不要入库。

## 配置模型

每个 tab：

- `id`
- `name`
- `command`

旧版单行 `shellScript` 会在加载时迁到 `command`。含换行的旧脚本会被丢弃，不会执行。不支持多行脚本、独立工作目录或环境变量。

## 从源码构建

不要把本机 IDEA / JDK 路径提交进仓库。本地覆盖方式任选其一：

- 环境变量：`PINTERM_LOCAL_IDE=/Applications/IntelliJ IDEA.app`
- Gradle 属性：`./gradlew test -PlocalIdePath=/path/to/IntelliJ IDEA.app`
- 仓库根目录的 `gradle.local.properties`（已 gitignore）：

```properties
localIdePath=/Applications/IntelliJ IDEA.app
```

未指定时，默认使用 `/Applications/IntelliJ IDEA.app`。非 macOS 必须显式覆盖。

Gradle 守护进程示例：

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew test
./gradlew buildPlugin
./gradlew runIde
```

插件包：`build/distributions/pinterm-0.1.0.zip`

## 已知限制

- 依赖 Reworked Terminal 内部 API，`sinceBuild` 为 `262`；升级 IDE 后这些 API 可能再变。
- 构建绑本机 IDEA，没有通用 CI 配置。
- 没有「真实打开 IDE 终端并固定」的集成测试。
- 不支持按 tab 配置工作目录或环境变量。
- `OWNED` 标记存在 VirtualFile UserData 上，IDE 重启后通常丢失。
- Marketplace / 设置页图标是 `META-INF/pluginIcon.svg`；工具栏图标是 `/icons/pinterm.svg`。
- 安装、更新、卸载声明为不需要重启（`require-restart="false"`）。从磁盘安装 zip 时，平台有时仍会提示重启。

## License

MIT
