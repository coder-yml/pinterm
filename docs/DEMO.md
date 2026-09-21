# 演示图 / GIF 制作

README 顶部与 JetBrains Marketplace 插件页的 Media 区**共用同一份真实录屏素材**，都在 `docs/screenshots/`（一张流程动图 + 三张静态截图，全部 1200×760）。素材来源与生成命令见文末「八、Marketplace Media 素材」。

本仓库**不含**原始录屏：`docs/功能演示.mov` 已被 `.gitignore` 排除（体积大），只保留从它派生的 `docs/screenshots/`。要重做素材，得先自己录一段（见下）。下面是从录制到转 GIF 的完整步骤。

## 一、准备沙箱 IDE

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 21)" ./gradlew runIde
```

- 会启动一个独立沙箱 IDE（配置目录在 `build/idea-sandbox`），不影响你日常的 IDE。
- 准备一个示例项目（任意即可），或用现成的小工程。
- 在 `Settings / Tools / PinTerm` 里配好 1–2 个标签用于演示，例如：名称 `Codex`、命令 `codex`。
- 建议深色主题、窗口宽 1000–1280，便于录制和压缩。

## 二、演示脚本（约 12–16 秒）

1. （可选）打开 `Settings / Tools / PinTerm`，展示标签列表与 Tab Name / Shell Command 字段，停留约 3 秒。
2. 回到编辑器，起点是一个普通项目（例如 `Main.java`）。
3. 点击右上角 PinTerm（仅图标）→ 下拉出现 Codex / Claude / Grok。
4. 选择 Codex → 终端在编辑器区打开并自动固定（固定标签单独成行）。
5. 稍等，终端进入运行态后自动出现 `codex` 命令。
6. 结束。

不带设置界面时约 8–12 秒即可；鼠标移动放慢一些。录制前清掉会暴露隐私的内容（真实路径、token、私有项目名），并注意别让设置页里含内部信息的命令入镜。

## 三、录制（macOS）

**图形方式**：按 `Cmd+Shift+5` → 选「录制所选部分」→ 点「录制」→ 结束后视频自动存到桌面（`.mov`）。

**命令行**（录主屏，最多 12 秒，显示点击，静音）：

```bash
screencapture -v -V 12 -k -x /tmp/pinterm-demo.mov
```

> CLI 的 `-v` 录整块屏幕（`-R` 区域在视频模式下不可靠）。只想录一小块，就用 `Cmd+Shift+5` 的「所选部分」，或录完整屏后用 ffmpeg 裁剪。

## 四、转 GIF（含调色板，画质最好）

```bash
# 先看时长，以便决定裁剪区间
ffprobe -v error -show_entries format=duration -of default=nw=1:nk=1 /tmp/pinterm-demo.mov

# 裁剪(可选) + 缩放 + 调色板 → GIF
ffmpeg -y -ss 1 -t 10 -i /tmp/pinterm-demo.mov \
  -vf "fps=15,scale=1000:-1:flags=lanczos,split[s0][s1];[s0]palettegen[p];[s1][p]paletteuse" \
  -loop 0 docs/demo.gif
```

- `-ss 1 -t 10`：从第 1 秒起取 10 秒；不需要就删掉。
- 需要裁剪区域时，在最前面加 `crop=w:h:x:y`，例如 `crop=1000:640:140:120,fps=15,...`。
- `fps=15` 通常够用。GIF 太大就降到 `fps=12`，或把 `scale=1000` 改成 `scale=800`。

## 五、MP4（可选，体积更小、更清晰）

```bash
ffmpeg -y -ss 1 -t 10 -i /tmp/pinterm-demo.mov \
  -vf "fps=30,scale=1200:-2:flags=lanczos" \
  -c:v libx264 -pix_fmt yuv420p -movflags +faststart docs/demo.mp4
```

> GitHub README 不会内联播放仓库里的 MP4，所以 README 里仍应使用 GIF 或 SVG；MP4 更适合放到 Release 附件或文档站。

## 六、放进 README

README 顶部用的是**真实录屏**素材：一张动图 + 三张静态截图，全部 `1200×760`（与 Marketplace Media 共用同一份文件，见「八」）。

```markdown
![PinTerm 实际使用演示](docs/screenshots/demo-real.gif)

| 工具栏下拉选择标签 | 终端固定到编辑器并执行命令 | 配置界面 |
| --- | --- | --- |
| ![工具栏下拉](docs/screenshots/01-toolbar-dropdown.png) | ![终端固定](docs/screenshots/02-pinned-terminals.png) | ![配置界面](docs/screenshots/03-settings.png) |
```

建议把 GIF 控制在约 10 秒、3–5 MB 以内。

## 七、其它工具

- **Gifski**（转 GIF 画质最佳）：`brew install gifski`
- **Kap / LICEcap**：直接录成 GIF
- **CleanShot X**：macOS 商业录屏工具

## 八、Marketplace Media 素材

Marketplace 插件页的 **Media（截图）区**与 README **共用同一份素材**：`docs/screenshots/`。

| 文件 | 画面 | 用途 |
| --- | --- | --- |
| `demo-real.gif` | 完整流程 | README 顶部主图；也可作 Media 第一张 |
| `01-toolbar-dropdown.png` | 工具栏下拉选择标签 | 静态截图 |
| `02-pinned-terminals.png` | 终端固定到编辑器并已执行命令 | 静态截图 |
| `03-settings.png` | `Settings / Tools / PinTerm` 配置页 | 静态截图 |

### 为什么是 1200×760

Marketplace 官方要求：截图最小推荐 **1200×760**，且**同一插件的所有截图必须同一比例**。这批素材统一 1200×760，README 与插件页共用，不会再出现两套比例互相漂移。

### 从录屏抽帧（素材来源）

原始录屏是 3024×1964 的 Retina 全屏（约 22 秒、~45 fps）。**它不在仓库里**——`docs/功能演示.mov` 被 `.gitignore` 排除，只有派生的 `docs/screenshots/` 入库。下面命令里的 `docs/功能演示.mov` 是你本地的文件名，重做素材时按实际路径替换。

录屏比例是 **1.540:1**，Media 要 **1.579:1**；裁掉底部 49px（状态栏）得到的 `3024×1915` 正好是 1.579:1，缩到 `1200×760` 即对齐。

```bash
# 静态截图：裁掉底部状态栏 → 缩放到 1200x760
for spec in "3.0:01-toolbar-dropdown" "11.6:02-pinned-terminals" "15.0:03-settings"; do
  t="${spec%%:*}"; n="${spec##*:}"
  ffmpeg -y -ss "$t" -i "docs/功能演示.mov" -frames:v 1 \
    -vf "crop=3024:1915:0:0,scale=1200:760:flags=lanczos" \
    "docs/screenshots/${n}.png"
done

# 流程 GIF：4 段拼接 → 1.5 倍速 → 12fps → 调色板优化
ffmpeg -y -i "docs/功能演示.mov" -filter_complex "\
[0:v]split=4[v1][v2][v3][v4];\
[v1]trim=start=1.7:end=6.6,setpts=PTS-STARTPTS[a];\
[v2]trim=start=10.8:end=12.2,setpts=PTS-STARTPTS[b];\
[v3]trim=start=12.3:end=14.7,setpts=PTS-STARTPTS[c];\
[v4]trim=start=14.8:end=18.2,setpts=PTS-STARTPTS[d];\
[a][b][c][d]concat=n=4:v=1:a=0[cat];\
[cat]crop=3024:1915:0:0,setpts=PTS/1.5,fps=12,scale=1200:760:flags=lanczos,split[s0][s1];\
[s0]palettegen=stats_mode=diff[p];[s1][p]paletteuse=dither=bayer:bayer_scale=5:diff_mode=rectangle[out]" \
  -map "[out]" -loop 0 "docs/screenshots/demo-real.gif"
```

当前参数：12 fps、96 帧（约 8 秒）、1200×760，约 1.1 MB。

> **四段是刻意剪的**：`7.3–10.2s` 是 Grok CLI 的信任提示，画面里有 `/Users/<用户名>/...` 真实路径。拼接区间 `1.7–6.6` / `10.8–12.2` / `12.3–14.7` / `14.8–18.2` 绕开了它。重录或改区间后，务必逐段复查没把这段带进来。

### 上传清单（作者后台手工操作）

Media **不随插件包发布**，`publishPlugin` 不会上传，Gradle 也不做版本管理。

1. 前置：PinTerm 已上传到 Marketplace（Media 只有在插件登记后才可编辑）。
2. 打开 `https://plugins.jetbrains.com/author/me` → PinTerm → **Media**。
3. 依次上传 `docs/screenshots/` 里的 `demo-real.gif`、`01-toolbar-dropdown.png`、`02-pinned-terminals.png`、`03-settings.png`。
4. 自查：全部 1200×760、不混比例；无真实路径 / 用户名 / token / 私人项目名。
5. 每次发版后复查 Media——它不会自动更新。

## 注意

- 不要出现真实路径、token、密钥、私有仓库名。
- 特别注意 **macOS 用户名**：CLI（Codex / Grok 等）的信任提示会显示 `/Users/<用户名>/...` 完整路径。当前素材靠**剪辑绕开**该画面（见「八」），不是打码；改区间后必须重新逐段复查。
- 深色主题、窗口 1000–1280 宽最通用。
- 避免画面里出现含内部信息的 `pinterm.xml` 命令内容。
