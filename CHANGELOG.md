# Changelog

PinTerm 的所有重要变更都记录在本文件。

格式参考 [Keep a Changelog](https://keepachangelog.com/zh-CN/1.1.0/)，
版本号遵循 [语义化版本](https://semver.org/lang/zh-CN/)。

本文件是发布说明的唯一来源：与构建的 `pluginVersion` 匹配的 `## [<version>]` 段，会同时用作 Git tag 的 Release notes **以及**在构建期转成插件的 `<change-notes>`。因此请保持条目简洁、面向用户。

## [0.1.3] - 2026-09-22

### 变更

- 插件描述为中文，开头保留一段英文，以满足 Marketplace 对描述必须以拉丁字符开头的要求。
- 作者链接指向本仓库（https://github.com/coder-yml/pinterm）。

## [0.1.2] - 2026-09-22

### 变更

- 插件描述改为中文，并在开头保留一段英文，以满足 Marketplace 对描述必须以拉丁字符开头的要求。
- 作者链接指向本仓库（https://github.com/coder-yml/pinterm）。

## [0.1.1] - 2026-09-22

### 变更

- 插件描述改为中文。
- 作者链接指向本仓库（https://github.com/coder-yml/pinterm）。

## [0.1.0] - 2026-09-21

### 新增

- PinTerm 首次开源发布。
- 仅图标的主工具栏下拉入口，用于打开已配置的终端标签（默认提供 Codex、Claude、Grok）。
- 将 Reworked Terminal 会话打开到编辑器区域并固定。
- 会话进入运行态后，可选地发送一行 shell 命令。
- 项目关闭或插件卸载时清理 PinTerm 打开的终端。
- 在 `Settings / Tools / PinTerm` 中管理标签名称与命令。

### 说明

- 需要 IntelliJ IDEA 2026.2+ 及 IDE 自带的 Reworked Terminal。
- 不可信项目不会发送已配置的命令。
