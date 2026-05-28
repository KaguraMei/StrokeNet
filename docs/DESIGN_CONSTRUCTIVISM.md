# StrokeNet 构成主义设计系统

## 🎨 设计理念

本应用采用**构成主义 (Constructivism)** 和**蓝图/工程图纸**风格，完全抛弃 Material Design 3 的柔和圆润风格，转而使用：

- ✅ **锐利的直角** - 所有容器使用 `RectangleShape`
- ✅ **粗边框** - 2dp 黑色或蓝色边框
- ✅ **网格背景** - Canvas 绘制的几何网格
- ✅ **高对比度** - 工程蓝、工业黄、极黑、图纸白
- ✅ **技术字体** - Monospace 等宽字体 + Serif 衬线字体
- ✅ **几何装饰** - 准星、圆环、虚线、坐标标记

## 🎨 配色系统

### 主色调

```kotlin
val ConstructBlue = Color(0xFF1332A6)      // 克莱因蓝/工程蓝
val ConstructYellow = Color(0xFFFFD600)    // 工业黄
val ConstructBlack = Color(0xFF0F0F0F)     // 极黑
val ConstructWhite = Color(0xFFF0F4F8)     // 蓝图纸白
val ConstructGray = Color(0xFF9E9E9E)      // 注释灰
val ConstructGridLine = Color(0xFFD0D7E2)  // 网格线颜色
```

### 色彩使用规则

| 颜色 | 用途 | 示例 |
|------|------|------|
| **ConstructBlue** | 主要边框、标题、强调 | 容器边框、大标题 |
| **ConstructYellow** | 活动状态、数值显示 | 按钮高亮、参数值 |
| **ConstructBlack** | 文字、分割线、停止按钮 | 正文、Divider |
| **ConstructWhite** | 背景、容器填充 | 页面底色、卡片背景 |
| **ConstructGray** | 次要文字、注释 | 描述文字、说明 |
| **ConstructGridLine** | 网格线、装饰线 | 背景网格 |

## 📐 几何背景系统

### ConstructBackground 组件

使用 Canvas 绘制的蓝图风格背景，包含：

1. **基准网格** - 40dp 间距的正交网格
2. **几何圆环** - 中心位置的同心圆（实线+虚线）
3. **准星十字** - 交叉的度量线
4. **技术标记线** - 右下角的装饰线

```kotlin
@Composable
fun ConstructBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize().background(ConstructWhite)) {
        // 绘制网格、圆环、准星等
    }
}
```

## 🔲 容器系统

### ConstructBlock 组件

替代 Material Design 的 Card，使用双层边框设计：

```kotlin
@Composable
fun ConstructBlock(
    modifier: Modifier = Modifier,
    title: String? = null,
    label: String = "+ DATA",
    content: @Composable () -> Unit
)
```

**特点**：
- 外层：2dp 蓝色边框
- 内层：1dp 蓝色边框
- 右上角：技术标签 "L1: {label}"
- 背景：半透明白色

## 🎚️ 控件设计

### 1. ControlSlider（滑块）

**设计元素**：
- 标题：大写 + Serif 字体 + 粗体
- 描述：`//` 前缀 + Monospace 字体
- 数值显示：蓝色背景 + 黄色文字 + `VAL_` 前缀
- 滑块：黄色滑块 + 黑色轨道
- 刻度线：底部虚线装饰

```kotlin
ControlSlider(
    label = "DEPTH",
    value = depth,
    valueRange = 0f..72f,
    onValueChange = { depth = it },
    valueText = "${depth.toInt()}",
    description = "STROKE AMPLITUDE"
)
```

### 2. StatusIndicator（状态指示器）

**设计元素**：
- 左侧色块：运行时蓝色，停止时黑色
- 状态文字：`ON` / `OFF`
- 系统状态：`ENGINE RUNNING...` / `SYSTEM HALTED.`
- 蓝牙状态：`[+] BT_LINK_ACTIVE` / `[-] BT_LINK_LOST`

### 3. PresetCard（预设卡片）

**设计元素**：
- 左侧黄色标记块：显示首字母
- 标题：大写 + 蓝色
- 描述：Monospace 字体
- 参数标签：黑色边框小标签

### 4. Button（按钮）

**设计规则**：
- 形状：`RectangleShape`（强制直角）
- 边框：2dp 黑色边框
- 文字：Monospace + 粗体 + 大写
- 主按钮：蓝色背景 + 白色文字
- 停止按钮：黑色背景 + 黄色文字

```kotlin
Button(
    onClick = { /* ... */ },
    shape = RectangleShape,
    colors = ButtonDefaults.buttonColors(
        containerColor = ConstructBlue,
        contentColor = ConstructWhite
    ),
    modifier = Modifier.border(2.dp, ConstructBlack)
) {
    Text(
        text = "INITIATE // START",
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Black
    )
}
```

## 🔤 字体系统

### 字体选择

| 用途 | 字体 | 示例 |
|------|------|------|
| **大标题** | FontFamily.Serif + Black | STROKE.NET |
| **技术文字** | FontFamily.Monospace | VAL_36, SYS_STATUS |
| **小标题** | FontFamily.Serif + Bold | PRIMARY METRICS |
| **描述** | FontFamily.Monospace | //STROKE AMPLITUDE |

### 字号规范

```kotlin
// 超大标题
fontSize = 42.sp  // PRESETS, SETTINGS

// 大标题
fontSize = 32.sp  // STROKE.NET

// 标题
fontSize = 18.sp  // PRIMARY METRICS

// 正文
fontSize = 14.sp  // 按钮文字

// 数值显示
fontSize = 14.sp  // VAL_36

// 小字
fontSize = 12.sp  // 副标题

// 注释
fontSize = 10.sp  // 描述文字

// 装饰
fontSize = 8.sp   // 刻度标记
```

## 📏 间距系统

```kotlin
// 页面边距
padding = 24.dp

// 组件间距
verticalArrangement = Arrangement.spacedBy(16.dp)

// 内部间距
padding = 12.dp

// 小间距
padding = 8.dp

// 边框宽度
border = 2.dp  // 主边框
border = 1.dp  // 次边框
```

## 🎭 页面设计

### 1. ControlScreen（控制页面）

**布局结构**：
```
ConstructBackground (几何网格背景)
└── Column
    ├── 标题区
    │   ├── STROKE.NET (大标题)
    │   ├── V 1.0.0 // KINETIC CONTROL (副标题)
    │   └── N/N (装饰黄块)
    ├── Divider (2dp 黑线)
    ├── StatusIndicator (状态指示器)
    ├── ConstructBlock "AXIS_CTRL"
    │   ├── PRIMARY METRICS (小标题)
    │   ├── DEPTH 滑块
    │   ├── EXT.VELOCITY 滑块
    │   └── RET.VELOCITY 滑块
    ├── ConstructBlock "ENV_CTRL"
    │   ├── SECONDARY SENSORS (小标题)
    │   ├── VIBRATION 滑块
    │   └── THERMAL 滑块
    ├── 按钮区
    │   ├── INITIATE // START (蓝色)
    │   └── HALT // STOP (黑色)
    └── 底部声明
```

### 2. PresetsScreen（预设页面）

**布局结构**：
```
ConstructBackground
└── Column
    ├── PRESETS (超大标题)
    ├── SELECT CONFIGURATION TEMPLATE (副标题)
    ├── PresetCard (轻柔模式)
    ├── PresetCard (标准模式)
    └── PresetCard (强力模式)
```

### 3. SettingsScreen（设置页面）

**布局结构**：
```
ConstructBackground
└── Column
    ├── SETTINGS (超大标题)
    ├── SYSTEM CONFIGURATION (副标题)
    ├── ConstructBlock "APP_INFO"
    ├── ConstructBlock "TECH_SPEC"
    ├── ConstructBlock "INTENT_API"
    └── 权限说明框 (黄色边框)
```

## 🎨 设计原则

### 1. 去圆角化
- ❌ 不使用 `RoundedCornerShape`
- ✅ 全部使用 `RectangleShape`
- ✅ 所有容器都是直角矩形

### 2. 强边框
- ❌ 不使用阴影 (elevation)
- ✅ 使用粗边框 (2dp)
- ✅ 双层边框增强层次

### 3. 技术感字体
- ❌ 不使用默认 Sans-serif
- ✅ 大量使用 Monospace（等宽字体）
- ✅ 标题使用 Serif（衬线字体）

### 4. 高对比度
- ❌ 不使用柔和的渐变
- ✅ 使用纯色色块
- ✅ 黑白对比 + 蓝黄点缀

### 5. 几何装饰
- ❌ 不使用图标
- ✅ 使用几何形状（方块、线条）
- ✅ 使用技术标记（坐标、刻度）

## 🔧 实现细节

### 取消 Material 3 默认样式

```kotlin
// 按钮
shape = RectangleShape  // 强制直角

// 对话框
shape = RectangleShape  // 强制直角

// 卡片
// 不使用 Card，改用 Box + border
```

### Canvas 绘制技巧

```kotlin
// 虚线效果
pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))

// 描边样式
style = Stroke(width = 2f)

// 网格绘制
for (i in 0..(width / gridSize).toInt()) {
    drawLine(...)
}
```

### 文字装饰

```kotlin
// 技术前缀
text = "VAL_$value"      // 数值
text = "//description"   // 注释
text = "[+] status"      // 状态
text = "L1: label"       // 标签
```

## 📱 响应式设计

虽然专注手机端，但设计系统具有良好的可扩展性：

- 网格背景自适应屏幕尺寸
- 组件使用 `fillMaxWidth()` 自适应宽度
- 文字大小使用 `sp` 单位
- 间距使用 `dp` 单位

## 🎯 设计目标达成

✅ **彻底取消圆角** - 所有容器使用 RectangleShape  
✅ **底层画布** - ConstructBackground 绘制网格和几何图形  
✅ **字体选择** - Monospace + Serif 的冲突美学  
✅ **色彩碰撞** - 工程蓝 + 工业黄 + 极黑 + 图纸白  
✅ **技术标签** - 大量使用前缀和装饰性文字  
✅ **几何感** - 方块、线条、网格、准星  
✅ **高对比度** - 纯色色块，无渐变  

## 🚀 使用示例

### 创建新页面

```kotlin
@Composable
fun NewScreen() {
    Box(modifier = Modifier.fillMaxSize()) {
        // 1. 添加几何背景
        ConstructBackground()
        
        // 2. 添加内容
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 3. 大标题
            Text(
                text = "NEW PAGE",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = 42.sp,
                color = ConstructBlue
            )
            
            // 4. 使用 ConstructBlock
            ConstructBlock(label = "SECTION_1") {
                // 内容
            }
        }
    }
}
```

### 创建新组件

```kotlin
@Composable
fun NewComponent() {
    Box(
        modifier = Modifier
            .border(2.dp, ConstructBlue)
            .background(ConstructWhite)
            .padding(12.dp)
    ) {
        // 组件内容
    }
}
```

## 📚 参考资源

- [构成主义艺术运动](https://en.wikipedia.org/wiki/Constructivism_(art))
- [工程图纸设计](https://en.wikipedia.org/wiki/Technical_drawing)
- [Bauhaus 设计风格](https://en.wikipedia.org/wiki/Bauhaus)
- [De Stijl 运动](https://en.wikipedia.org/wiki/De_Stijl)

## 🎨 设计灵感来源

本设计系统受以下风格启发：

1. **俄罗斯构成主义** - 几何形状、强烈对比
2. **工程蓝图** - 网格、坐标、技术标注
3. **包豪斯** - 功能主义、几何简洁
4. **De Stijl** - 直线、矩形、原色

---

**设计系统版本**: 1.0.0  
**最后更新**: 2026-05-28  
**设计风格**: 构成主义 / 蓝图工程图纸
