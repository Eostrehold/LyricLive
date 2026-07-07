# LyricLive 模组架构设计

## 项目概述
LyricLive 是一个基于 Fabric 26.1.2 的纯客户端模组，用于在 Minecraft 游戏内举办唱歌比赛或演唱会时，为玩家提供歌词显示与歌词发送功能。

## 模块划分

### 1. 核心模块 (Core)
- **PlaybackController** - 管理播放状态（播放、暂停、停止）
- **TimelineManager** - 管理歌曲时间轴和歌词同步

### 2. LRC 解析模块 (LRC Parser)
- **LrcParser** - 解析 .lrc 文件
- **LrcLyric** - 表示单行歌词（时间戳 + 文本）
- **LyricTrack** - 表示整首歌词（包含标题、艺术家、歌词列表）

### 3. 显示模块 (Display)
- **LyricRenderer** - 在屏幕上渲染歌词
- **DisplayConfig** - 管理显示配置（颜色、大小、位置等）

### 4. 发送模块 (Sender)
- **ChatSender** - 将歌词发送到聊天栏
- **CommandSender** - 将歌词作为指令发送

### 5. GUI 模块 (GUI)
- **MainScreen** - 主界面，包含播放控制、文件选择等
- **SettingsScreen** - 设置界面

## 数据流

```
LRC 文件 → LrcParser → LyricTrack → TimelineManager → PlaybackController
                                                            ↓
                                                    LyricRenderer (显示)
                                                    ChatSender (发送)
```

## 关键技术点

1. **时间同步精度** - 使用 `System.nanoTime()` 或 Minecraft 的游戏时间来保证高精度同步
2. **客户端渲染** - 使用 Minecraft 的渲染系统在屏幕上绘制歌词
3. **聊天发送** - 使用 Minecraft 的聊天系统发送消息
4. **GUI 设计** - 遵循 Minecraft 原版界面风格

## 目录结构

```
src/
├── client/
│   ├── java/com/eostrehold/lyriclive/client/
│   │   ├── LyricLiveClient.java (客户端初始化)
│   │   ├── core/
│   │   │   ├── PlaybackController.java
│   │   │   └── TimelineManager.java
│   │   ├── lrc/
│   │   │   ├── LrcParser.java
│   │   │   ├── LrcLyric.java
│   │   │   └── LyricTrack.java
│   │   ├── display/
│   │   │   ├── LyricRenderer.java
│   │   │   └── DisplayConfig.java
│   │   ├── sender/
│   │   │   ├── ChatSender.java
│   │   │   └── CommandSender.java
│   │   └── gui/
│   │       ├── MainScreen.java
│   │       └── SettingsScreen.java
│   └── resources/
│       └── assets/lyriclive/
│           ├── icon.png
│           └── textures/
└── main/
    ├── java/com/eostrehold/lyriclive/
    │   └── LyricLive.java (模组初始化)
    └── resources/
        └── fabric.mod.json
```

## 实现步骤

1. 首先实现 LRC 解析器
2. 然后实现核心播放控制模块
3. 接着实现歌词显示功能
4. 实现歌词发送功能
5. 最后实现 GUI 界面