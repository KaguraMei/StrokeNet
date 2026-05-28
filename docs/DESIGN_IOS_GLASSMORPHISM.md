# iOS 玻璃拟态设计系统

## 🎨 设计理念

从构成主义的硬朗蓝图风格转变为**苹果美学的玻璃拟态（Glassmorphism）**设计语言：

- **纯白干净**：去除厚重卡片和强烈阴影
- **半透明悬浮**：玻璃面板效果，轻盈优雅
- **iOS 配色**：经典的 iOS 蓝、绿、红色系
- **大圆角**：24dp 圆角，柔和现代
- **悬浮导航**：底部 Dock 式导航栏

## 🎯 核心颜色系统

```kotlin
// iOS 风格颜色
val iOSBg = Color(0xFFF2F2F7)           // 浅灰背景
val iOSBlue = Color(0xFF007AFF)         // iOS 蓝
val iOSGreen = Color(0xFF34C759)        // iOS 绿
val iOSRed = Color(0xFFFF3B30)          // iOS 红
val iOSTextPrimary = Color(0xFF1C1C1E) // 主文本
val iOSTextSecondary = Color(0xFF8E8E93) // 次要文本
```

## 📦 核心组件

### 1. GlassPanel - 玻璃面板

半透明白色面板，是整个设计系统的基础：

```kotlin
@Composable
fun GlassPanel(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
)
```

**特性：**
- 75% 透明度白色背景
- 24dp 大圆角
- 极淡阴影（3% 透明度）
- 90% 透明度白色描边
- 20dp 内边距

### 2. GlassTopBar - 悬浮顶栏

半透明顶部导航栏：

```kotlin
@Composable
fun GlassTopBar(title: String)
```

**特性：**
- 85% 透明度白色背景
- 自动适配状态栏高度
- 18sp 半粗体标题
- 居中对齐

### 3. GlassBottomDock - 悬浮 Dock 导航

底部悬浮胶囊式导航栏：

```kotlin
@Composable
fun GlassBottomDock(
    currentRoute: String,
    onNavigate: (String) -> Unit
)
```

**特性：**
- 85% 透明度白色背景
- 完全圆角（CircleShape）
- 悬浮在底部 32dp
- 8% 透明度阴影
- 图标 + 文字双层显示
- 选中状态用 iOS 蓝高亮

## 🖼️ 页面布局

### 控制中心 (ControlScreen)

**结构：**
1. 状态指示器（运行状态 + 蓝牙状态）
2. 推拉参数面板（深度、伸出速度、缩回速度）
3. 增强设置面板（震动强度、加热温度）
4. 操作按钮区（启动/调节 + 紧急停止）

**滑块设计：**
- 标签 + 数值双行显示
- iOS 蓝色激活轨道
- 8% 透明度未激活轨道
- 白色滑块按钮

**按钮设计：**
- 56dp 高度
- 16dp 圆角
- iOS 蓝（主操作）/ iOS 红（停止）
- 17sp 半粗体文字

### 预设模式 (PresetsScreen)

**结构：**
1. 说明文字
2. 玻璃面板包裹的预设列表
3. 每个预设项：图标 + 名称描述 + 右箭头

**预设项设计：**
- 36dp 圆角图标背景（10% 透明度 iOS 蓝）
- 20dp 图标尺寸
- 17sp 主标题 + 13sp 副标题
- 5% 透明度分割线

### 设置 (SettingsScreen)

**结构：**
1. 应用信息面板
2. 技术规格面板
3. 外部控制接口面板
4. 权限说明卡片

**设置项设计：**
- 标签 + 值左右对齐
- 15sp 字号
- 5% 透明度分割线

**权限卡片：**
- 10% 透明度 iOS 红背景
- 30% 透明度 iOS 红边框
- 16dp 圆角
- 警告图标 + 说明文字

## 🎭 视觉层级

### 阴影系统

```kotlin
// 玻璃面板阴影
elevation = 16.dp
spotColor = Color.Black.copy(alpha = 0.03f)

// Dock 导航阴影
elevation = 20.dp
spotColor = Color.Black.copy(alpha = 0.08f)
```

### 圆角系统

```kotlin
// 大面板
RoundedCornerShape(24.dp)

// 按钮
RoundedCornerShape(16.dp)

// 小图标背景
RoundedCornerShape(8.dp)

// 预设图标
RoundedCornerShape(10.dp)

// Dock 导航
CircleShape
```

### 间距系统

```kotlin
// 页面边距
horizontal = 20.dp
vertical = 16.dp

// 组件间距
verticalArrangement = Arrangement.spacedBy(20.dp)

// 面板内边距
padding = 20.dp

// Dock 底部间距
bottom = 32.dp
```

## 🔄 与构成主义设计的对比

| 特性 | 构成主义 | iOS 玻璃拟态 |
|------|---------|-------------|
| **背景** | 网格蓝图 | 纯净浅灰 |
| **面板** | 厚重边框 | 半透明玻璃 |
| **圆角** | 直角 | 大圆角 |
| **阴影** | 无 | 极淡光晕 |
| **颜色** | 高对比 | 柔和克制 |
| **字体** | Serif + Mono | System Default |
| **导航** | 标准底栏 | 悬浮 Dock |
| **风格** | 工业技术 | 现代简约 |

## 📱 适配说明

### 状态栏和导航栏

```kotlin
// 顶栏自动适配状态栏
.windowInsetsPadding(WindowInsets.statusBars)

// Dock 自动适配导航栏
.windowInsetsPadding(WindowInsets.navigationBars)
```

### 滚动区域底部留白

所有可滚动页面底部添加 80dp 留白，为悬浮 Dock 预留空间：

```kotlin
Spacer(modifier = Modifier.height(80.dp))
```

## 🎨 设计原则

1. **克制用色**：只在必要时使用 iOS 蓝、绿、红
2. **留白充足**：组件间距至少 16dp
3. **层次清晰**：玻璃面板 → 内容 → 操作
4. **触控友好**：按钮最小 48dp 高度
5. **视觉降噪**：去除不必要的装饰元素

## 🚀 实现文件

### 核心组件
- `ui/components/GlassComponents.kt` - 玻璃拟态组件库
- `ui/components/StatusIndicator.kt` - 状态指示器

### 页面
- `ui/screens/ControlScreen.kt` - 控制中心
- `ui/screens/PresetsScreen.kt` - 预设模式
- `ui/screens/SettingsScreen.kt` - 设置页面

### 主题
- `ui/theme/Color.kt` - iOS 颜色系统
- `MainActivity.kt` - 主界面架构

## 📝 使用示例

### 创建玻璃面板

```kotlin
GlassPanel {
    Text("标题", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
    Spacer(modifier = Modifier.height(16.dp))
    Text("内容", fontSize = 15.sp, color = iOSTextSecondary)
}
```

### 创建 iOS 风格滑块

```kotlin
AppleStyleSlider(
    label = "推拉深度",
    value = depth,
    range = 0f..72f,
    onValueChange = { depth = it }
)
```

### 创建设置项

```kotlin
SettingItem(label = "版本号", value = "1.0.0")
```

## 🎯 设计目标达成

✅ 纯白干净的简洁风格  
✅ 半透明悬浮玻璃面板  
✅ 苹果美学配色系统  
✅ 悬浮 Dock 式导航  
✅ 大圆角柔和设计  
✅ 极简视觉降噪  

---

**设计更新日期**: 2026.05.28  
**设计语言**: iOS Glassmorphism  
**目标平台**: Android 12+ (API 31+)
