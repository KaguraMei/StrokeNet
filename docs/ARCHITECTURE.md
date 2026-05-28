# StrokeNet 项目架构说明

## 📁 项目结构

```
app/src/main/java/aya/strokenet/
├── MainActivity.kt                      # 主入口 Activity
├── BleAdvertiser.kt                     # BLE 广播工具类
├── BleService.kt                        # 前台服务（重试+通知）
│
├── data/                                # 数据层
│   └── model/                           # 数据模型
│       ├── ControlParams.kt             # 控制参数数据类
│       └── Preset.kt                    # 预设模式数据类
│
└── ui/                                  # UI 层
    ├── navigation/                      # 导航配置
    │   └── Screen.kt                    # 屏幕路由定义
    │
    ├── screens/                         # 页面
    │   ├── ControlScreen.kt             # 控制页面
    │   ├── PresetsScreen.kt             # 预设模式页面
    │   └── SettingsScreen.kt            # 设置页面
    │
    ├── components/                      # 可复用组件
    │   ├── ControlSlider.kt             # 滑块组件
    │   ├── PresetCard.kt                # 预设卡片组件
    │   └── StatusIndicator.kt           # 状态指示器组件
    │
    └── theme/                           # 主题配置
        ├── Color.kt                     # 颜色定义
        ├── Theme.kt                     # 主题配置
        └── Type.kt                      # 字体配置
```

## 🏗️ 架构设计

### 1. 分层架构

```
┌─────────────────────────────────────┐
│         Presentation Layer          │  UI 层
│  (Screens, Components, Navigation)  │
├─────────────────────────────────────┤
│          Service Layer              │  服务层
│  (BleService - 前台服务+重试逻辑)    │
├─────────────────────────────────────┤
│          Business Logic             │  业务逻辑层
│       (BleAdvertiser)               │
├─────────────────────────────────────┤
│           Data Layer                │  数据层
│    (Models, Repository)             │
└─────────────────────────────────────┘
```

### 2. 模块职责

#### MainActivity.kt
- **职责**：应用入口，生命周期管理
- **功能**：
  - 权限请求和管理
  - 蓝牙状态检查
  - Intent 参数处理（MCP 调用入口）
  - 启动 BleService

#### BleService.kt
- **职责**：前台服务，确保指令可靠送达
- **功能**：
  - 前台服务（通知栏显示，系统不杀）
  - 重试逻辑（失败时最多重试 2 次）
  - 通知更新（显示发送状态）
  - 后台运行能力
  - 自动停止（任务完成后）

#### BleAdvertiser.kt
- **职责**：BLE 广播核心逻辑
- **功能**：
  - 构造控制 UUID
  - 发送 BLE 广播
  - 管理广播生命周期
  - 错误处理和回调

#### Data Layer (data/)
- **ControlParams.kt**：控制参数数据模型
  - 封装所有控制参数
  - 参数验证
  - 预设参数定义
  
- **Preset.kt**：预设模式数据模型
  - 预设模式定义
  - 内置预设列表

#### UI Layer (ui/)

**Navigation (ui/navigation/)**
- **Screen.kt**：路由定义
  - 定义所有页面路由
  - 导航图标和标题
  - 统一管理导航

**Screens (ui/screens/)**
- **ControlScreen.kt**：主控制页面
  - 参数滑块
  - 启动/停止按钮
  - 状态显示
  
- **PresetsScreen.kt**：预设模式页面
  - 预设列表
  - 预设详情对话框
  - 快速启动
  
- **SettingsScreen.kt**：设置页面
  - 应用信息
  - 权限管理
  - 技术信息

**Components (ui/components/)**
- **ControlSlider.kt**：滑块组件
  - 可复用的参数滑块
  - 标签、数值显示
  - 自定义样式
  
- **PresetCard.kt**：预设卡片
  - 预设信息展示
  - 点击交互
  - 参数预览
  
- **StatusIndicator.kt**：状态指示器
  - 运行状态显示
  - 蓝牙状态显示
  - 动画效果

## 🔄 数据流

### 1. 用户操作流程

```
用户操作 → Screen → 启动 BleService → BleAdvertiser → BLE 广播 → 设备响应
                         ↓
                    通知栏显示状态
                         ↓
                    重试（如果失败）
                         ↓
                    自动停止服务
```

### 2. Intent 调用流程（MCP）

```
外部 Intent → MainActivity.handleIntent() → 启动 BleService
                                                ↓
                                          BleAdvertiser
                                                ↓
                                          BLE 广播
                                                ↓
                                          通知栏反馈
```

### 3. 预设模式流程

```
选择预设 → PresetsScreen → 确认对话框 → 启动 BleService → 应用参数
                                              ↓
                                        依次发送：推拉、强度、温度
```

## 🎨 UI 设计原则

### 1. Material Design 3
- 使用 Material 3 组件
- 遵循 Material Design 规范
- 支持动态主题

### 2. 组件化
- 可复用组件独立封装
- 统一的样式和交互
- 易于维护和扩展

### 3. 响应式设计
- 适配不同屏幕尺寸
- 支持横竖屏切换
- 流畅的动画效果

## 🔌 导航方式

### 当前实现：NavigationSuiteScaffold

```kotlin
NavigationSuiteScaffold(
    navigationSuiteItems = {
        Screen.screens.forEach { screen ->
            item(
                icon = { Icon(screen.icon) },
                label = { Text(screen.title) },
                selected = currentScreen == screen.route,
                onClick = { currentScreen = screen.route }
            )
        }
    }
) {
    // 页面内容
}
```

**优点**：
- 简单直观
- 适合少量页面（3-5个）
- 自动适配底部导航栏/侧边栏
- 无需额外依赖

**适用场景**：
- 扁平的页面结构
- 无需复杂的页面跳转
- 主要页面之间的切换

### 可选方案：Jetpack Navigation Compose

如果未来需要更复杂的导航（如页面栈、深层链接、参数传递），可以迁移到 Navigation Compose：

```kotlin
// 添加依赖
implementation("androidx.navigation:navigation-compose:2.7.0")

// 定义导航图
@Composable
fun NavGraph(
    navController: NavHostController,
    bleAdvertiser: BleAdvertiser
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Control.route
    ) {
        composable(Screen.Control.route) {
            ControlScreen(bleAdvertiser)
        }
        composable(Screen.Presets.route) {
            PresetsScreen(bleAdvertiser)
        }
        composable(Screen.Settings.route) {
            SettingsScreen()
        }
    }
}
```

**优点**：
- 支持页面栈管理
- 支持深层链接
- 支持页面间参数传递
- 支持页面转场动画
- 更好的状态保存

**何时迁移**：
- 需要页面栈（返回上一页）
- 需要页面间传递复杂参数
- 需要深层链接支持
- 页面数量增加（>5个）

## 📦 依赖管理

### 当前依赖
```kotlin
// Compose
implementation("androidx.compose.ui:ui")
implementation("androidx.compose.material3:material3")
implementation("androidx.compose.material3:material3-adaptive-navigation-suite")

// Activity
implementation("androidx.activity:activity-compose")

// Lifecycle
implementation("androidx.lifecycle:lifecycle-runtime-ktx")
```

### 未来可能添加
```kotlin
// Navigation (如果需要)
implementation("androidx.navigation:navigation-compose:2.7.0")

// ViewModel (如果需要状态管理)
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")

// Room (如果需要本地数据库)
implementation("androidx.room:room-runtime:2.6.0")
implementation("androidx.room:room-ktx:2.6.0")

// DataStore (如果需要持久化设置)
implementation("androidx.datastore:datastore-preferences:1.0.0")
```

## 🔧 扩展建议

### 1. 添加 ViewModel（推荐）

当状态管理变复杂时，引入 ViewModel：

```kotlin
class ControlViewModel : ViewModel() {
    private val _controlParams = MutableStateFlow(ControlParams())
    val controlParams: StateFlow<ControlParams> = _controlParams.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    fun updateDepth(value: Int) {
        _controlParams.update { it.copy(depth = value) }
    }
    
    fun start(bleAdvertiser: BleAdvertiser) {
        // 启动逻辑
    }
}
```

### 2. 添加 Repository

如果需要数据持久化：

```kotlin
class PresetRepository(private val dataStore: DataStore<Preferences>) {
    suspend fun savePreset(preset: Preset) {
        // 保存预设
    }
    
    fun getPresets(): Flow<List<Preset>> {
        // 获取预设列表
    }
}
```

### 3. 添加 UseCase

如果业务逻辑复杂：

```kotlin
class StartDeviceUseCase(
    private val bleAdvertiser: BleAdvertiser
) {
    operator fun invoke(params: ControlParams): Result<Unit> {
        // 启动设备的业务逻辑
    }
}
```

## 📝 代码规范

### 1. 文件命名
- Activity: `MainActivity.kt`
- Screen: `ControlScreen.kt`
- Component: `ControlSlider.kt`
- Model: `ControlParams.kt`
- ViewModel: `ControlViewModel.kt`

### 2. 包结构
- 按功能分包（推荐）
- 按层级分包（当前）

### 3. Composable 命名
- 大写开头：`ControlScreen()`
- 描述性名称：`StatusIndicator()`
- Preview 后缀：`ControlScreenPreview()`

### 4. 状态管理
- 使用 `remember` 管理局部状态
- 使用 `rememberSaveable` 保存配置变化
- 使用 `ViewModel` 管理共享状态（推荐）

## 🎯 最佳实践

### 1. 单一职责
每个文件/类只负责一个功能

### 2. 组件复用
提取可复用的 UI 组件

### 3. 状态提升
将状态提升到合适的层级

### 4. 不可变数据
使用 `data class` 和 `copy()`

### 5. 错误处理
完善的错误处理和用户反馈

## 🚀 性能优化

### 1. 避免重组
- 使用 `remember` 缓存计算结果
- 使用 `derivedStateOf` 派生状态
- 使用 `key` 优化列表

### 2. 懒加载
- 使用 `LazyColumn` 替代 `Column`
- 按需加载数据

### 3. 异步操作
- 使用 `LaunchedEffect` 执行副作用
- 使用 `rememberCoroutineScope` 启动协程

## 📚 参考资源

- [Jetpack Compose 官方文档](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Android Architecture Components](https://developer.android.com/topic/architecture)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)
