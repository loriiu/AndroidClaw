# AndroidClaw

Android 平台的个人 AI 助手应用，基于 Clean Architecture + MVVM 架构。

## 项目简介

AndroidClaw 是 JavaClaw 项目的 Android 移植版本，旨在为 Android 设备提供类似的 AI 助手能力。项目采用 Kotlin 开发，使用 Jetpack Compose 构建 UI，遵循现代 Android 最佳实践。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Kotlin | 1.9.22 | 开发语言 |
| Jetpack Compose | BOM 2024.02.00 | UI 框架 |
| Hilt | 2.50 | 依赖注入 |
| Room | 2.6.1 | 本地数据库 |
| DataStore | 1.0.0 | 配置存储 |
| WorkManager | 2.9.0 | 后台任务调度 |
| OkHttp + Retrofit | 4.12 / 2.9 | 网络请求 |

## 架构

```
├── app/                    # 应用入口模块
│   ├── ui/                 # Compose UI
│   ├── di/                 # Hilt 模块
│   └── ...
│
├── domain/                 # 领域层
│   ├── model/             # 领域模型
│   ├── repository/        # 仓储接口
│   └── usecase/           # 用例
│
├── data/                   # 数据层
│   ├── local/             # 本地数据源
│   ├── remote/            # 远程数据源
│   └── repository/        # 仓储实现
│
└── infrastructure/         # 基础设施层
    ├── llm/               # LLM Provider
    ├── mcp/               # MCP 客户端
    └── security/          # 安全相关
```

## 功能特性

- [x] AI 对话（支持 OpenAI、Anthropic、Ollama）
- [x] 任务管理（创建、调度、状态跟踪）
- [x] 技能系统（SKILL.md 解析与加载）
- [x] 首次引导配置
- [x] LLM 配置管理
- [x] 用户偏好设置
- [ ] 流式输出
- [ ] MCP 服务器连接
- [ ] 通知与提醒

## 开发

### 环境要求

- Android Studio Hedgehog (2023.1.1) 或更高
- JDK 17
- Android SDK 34

### 构建

```bash
# 同步依赖
./gradlew sync

# Debug 构建
./gradlew assembleDebug

# Release 构建
./gradlew assembleRelease
```

### 项目结构

```
AndroidClaw/
├── app/                    # 应用模块
├── gradle/                 # Gradle 配置
└── gradle.properties       # 项目属性
```

## 许可证

本项目基于 Apache License 2.0 开源。

## 参考

- [JavaClaw](https://github.com/your-org/JavaClaw) - 后端参考实现
- [Jetpack Compose](https://developer.android.com/compose) - UI 框架
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) - 依赖注入
