<p align="center">
  <img src="./assets/readme/hero.svg" width="100%" alt="LyricLive — 在 Minecraft 中举办卡拉 OK 与唱歌比赛">
</p>

<p align="center">
  <a href="https://github.com/Eostrehold/LyricLive/actions/workflows/build.yml"><img src="https://img.shields.io/github/actions/workflow/status/Eostrehold/LyricLive/build.yml?style=flat-square&label=Build&color=5ABF6A" alt="Build"></a>
  <a href="https://fabricmc.net"><img src="https://img.shields.io/badge/Minecraft-26.1.2-5ABF6A?style=flat-square&labelColor=1A1716" alt="Minecraft 26.1.2"></a>
  <a href="https://fabricmc.net"><img src="https://img.shields.io/badge/Fabric-0.19.2B-D4A843?style=flat-square&labelColor=1A1716" alt="Fabric 0.19.3+"></a>
  <a href="https://jdk.java.net/25/"><img src="https://img.shields.io/badge/Java-25-6A5ACD?style=flat-square&labelColor=1A1716" alt="Java 25"></a>
  <a href="./LICENSE"><img src="https://img.shields.io/badge/MIT-A0988E?style=flat-square&labelColor=1A1716" alt="MIT License"></a>
</p>

LyricLive 是一个基于 **Fabric 26.1.2** 的纯客户端 Minecraft 模组。在游戏中举办卡拉 OK 与唱歌比赛时，导入 `.lrc` 歌词文件即可在 HUD 上实时显示歌词，并支持自动发送到聊天栏。

---

## 快速开始

**5 步，即可在 Minecraft 开唱：**

1. 安装 **Minecraft 26.1.2** + **Fabric Loader 0.19.3+**
2. 将 LyricLive `.jar` 放入 `.minecraft/mods` 文件夹
3. 启动游戏，按 **`L`** 打开 LyricLive 界面
4. 选择 `.lrc` 歌词文件加载
5. 按 **`P`** 播放，歌词实时显示在屏幕上

> 按 **`J`** 切换自动发送，歌词将自动出现在聊天栏。

---

## 核心功能

<p align="center">
  <img src="./assets/readme/features.svg" width="100%" alt="LyricLive 六大核心功能">
</p>

**歌曲时间控制** — 播放 / 暂停 / 停止 / 快进。基于 `System.nanoTime()` 的高精度时间同步，支持手动 seek 与起始偏移设置。

**LRC 歌词导入** — 解析标准 `.lrc` 文件，支持 `[mm:ss.SSS]` 毫秒级时间戳与 `[ti:]` `[ar:]` `[al:]` `[by:]` 元数据标签。多时间戳可指向同一行。支持运行时重新加载与切换。

**图形化界面** — 按 `L` 键唤出主界面，歌词文件浏览、播放控制（含可点击进度条）、独立设置面板（位置 / 颜色 / 字体 / 透明度 / 动画）。

**歌词 HUD 渲染** — 当前歌词 ± 前 2 行 + 后 2 行上下文，lerp 平滑滚动与淡入淡出动画，透明度随距离衰减，信息栏显示播放状态与时间。

**自动发送** — 按 `J` 切换，时间轴驱动，支持聊天栏直发或带前缀指令发送。

**手动发送** — 按 `K` 或 GUI 按钮随时发送当前歌词。自动发送开启时自动禁用手动，避免重复。

---

## 快捷键

| 按键 | 功能 |
|------|------|
| `L` | 打开 LyricLive 界面 |
| `P` | 播放 / 暂停 |
| `O` | 停止 |
| `K` | 发送当前歌词 |
| `J` | 切换自动发送 |

---

## 工作流程

<p align="center">
  <img src="./assets/readme/workflow.svg" width="100%" alt="LyricLive 工作流程">
</p>

LyricLive 采用单向数据流的分层架构：

```text
LRC 文件 → LrcParser → LyricTrack → TimelineManager
                                        ↓
                                 PlaybackController
                                  ↙              ↘
                        LyricRenderer          LyricSender
                        (HUD 画面渲染)        (聊天栏发送)
```

**各层职责**

- **LRC 解析层** — `LrcParser` 将 `.lrc` 文件解析为 `LyricTrack`，包含有序歌词列表与元数据。
- **核心控制层** — `PlaybackController` 管理播放状态（播放 / 暂停 / 停止 / seek）；`TimelineManager` 通过二分查找匹配当前时间戳对应的歌词行。
- **显示层** — `LyricRenderer` 在游戏 HUD 上渲染歌词，支持滚动动画与淡入淡出。
- **发送层** — `LyricSender` 将歌词发送到聊天栏，支持自动 / 手动两种模式。

---

## 配置

### 显示

| 选项 | 说明 | 范围 |
|------|------|------|
| X / Y 位置 | 歌词在屏幕上的位置 | 0.0 – 1.0 |
| 字体大小 | 歌词字体 | 8 – 64 |
| 字体颜色 | 十六进制颜色值 | `#000000` – `#FFFFFF` |
| 透明度 | 歌词不透明度 | 0.0 – 1.0 |
| 阴影 | 文字阴影 | 开 / 关 |
| 居中 | 居中显示 | 开 / 关 |
| 渐入渐出 | 淡入淡出动画 | 开 / 关 |

### 发送

| 选项 | 说明 |
|------|------|
| 聊天发送 | 启用 / 禁用自动发送到聊天栏 |
| 指令发送 | 启用 / 禁用指令模式 |
| 指令模板 | `{lyric}` 会被替换为歌词文本 |

---

## LRC 格式参考

```lrc
[ti:歌曲标题]
[ar:艺术家]
[al:专辑]
[by:歌词作者]

[00:00.000]第一行歌词
[00:05.000]第二行歌词
[00:10.000][00:20.000]重复句
```

歌词文件放置在 `.minecraft/lyriclive/` 目录下。

---

## 开发

| 项目 | 版本 |
|------|------|
| Minecraft | 26.1.2 |
| Fabric Loader | 0.19.3 |
| Fabric API | 0.154.0+26.1.2 |
| Java | 25 |
| 构建 | Gradle + Fabric Loom 1.17 |

纯客户端模组，服务端无需安装。编译后的 `.jar` 直接放入 `mods` 文件夹即可。

---

## 许可证

[MIT License](./LICENSE) &copy; 2026 Eostrehold