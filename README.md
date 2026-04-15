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
│   └── di/                 # Hilt 模块
│
├── domain/                 # 领域层
│   ├── model/todo/        # 待办领域模型
│   ├── service/todo/      # 待办服务接口
│   └── usecase/todo/      # 待办用例
│
├── infrastructure/         # 基础设施层
│   ├── todo/              # 待办系统实现
│   │   ├── db/           # Room 数据库
│   │   ├── repository/   # 仓储实现
│   │   └── service/     # 引擎实现
│   ├── cache/            # 缓存基础设施
│   ├── cost/             # 成本追踪基础设施
│   ├── llm/              # LLM Provider
│   ├── mcp/              # MCP 客户端
│   └── security/         # 安全相关
│
└── di/                     # 依赖注入模块
```

## 功能特性

### 核心功能
- [x] AI 对话（支持 OpenAI、Anthropic、Ollama）
- [x] 技能系统（SKILL.md 解析与加载）
- [x] 首次引导配置
- [x] LLM 配置管理
- [x] 用户偏好设置

### 智能待办系统 (SmartTodo) ✅ 新增
- [x] 待办数据模型（重要度、紧急性、截止时间、地点、标签、状态、进度）
- [x] 子任务拆分与进度追踪
- [x] 里程碑设置
- [x] 阻塞原因记录
- [x] 艾森豪威尔矩阵优先级排序
- [x] 动态权重调整（临近截止时间权重提升）
- [x] 用户习惯学习
- [x] 时间冲突检测与智能建议
- [x] 地点冲突检测
- [x] 待办意图分类器（显式/隐式触发）
- [x] Room 数据库持久化
- [x] Hilt 依赖注入

### 待开发
- [ ] 流式输出
- [ ] MCP 服务器连接
- [ ] 天气提醒集成
- [ ] 节假日提醒集成
- [ ] 位置提醒（Geofencing）

## 待办系统详解

### 数据模型
```kotlin
data class Todo(
    val id: String,
    val title: String,
    val importance: Int = 3,    // 1-5级
    val urgency: Int = 3,       // 1-5级
    val deadline: Long?,
    val estimatedDuration: Int?, // 分钟
    val location: TodoLocation?,
    val tags: List<String>,
    val status: TodoStatus,
    val progress: Int,          // 0-100%
    val subTasks: List<SubTask>,
    val computedPriority: Float,
    // ...
)
```

### 智能排序算法
- **基础权重**: `importance × urgency × 0.1`
- **截止时间提升**:
  - 1小时内: +3.0
  - 4小时内: +2.0
  - 24小时内: +1.0
  - 3天内: +0.5
- **用户习惯**: 基于时段生产力评分调整

### 冲突检测
- 时间重叠检测（5分钟容差）
- 时间紧邻检测（<15分钟间隔）
- 地点冲突检测（100米范围）

### 意图分类
支持显式和隐式触发：
- 显式: "今天有什么待办？"
- 隐式: "帮我安排行程" → 自动拉取待办上下文

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

## 许可证

本项目基于 Apache License 2.0 开源。

## 参考

- [JavaClaw](https://github.com/your-org/JavaClaw) - 后端参考实现
- [Jetpack Compose](https://developer.android.com/compose) - UI 框架
- [Hilt](https://developer.android.com/training/dependency-injection/hilt-android) - 依赖注入
