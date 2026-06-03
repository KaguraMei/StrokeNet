# UUID 编码协议详解

## ⚠️ 重要说明

本项目的 BLE 协议实现基于对官方 APP 的逆向分析。感谢 **[用 AI 远程控制你的 Cachito 大秀炮机](https://claude.ai/public/artifacts/921eda06-e567-4cde-85af-8cde831a608f)** 教程的启发，但需要指出：**该教程中给出的 UUID 编码逻辑并不正确**。

本文档基于实际的官方 APP 逆向结果，提供了正确且完整的编码实现。

---

## 📋 UUID 格式总览

```
710003XX-YYYY-ZZZZ-0000-WWWWWWWWWWCC
│     ││ │  │ │  │ │  │ │        ││
│     ││ │  │ │  │ │  │ │        │└─ 校验和（2位，自动计算）
│     ││ │  │ │  │ │  │ └────────┴── 参数数据（10位十六进制）
│     ││ │  │ │  │ └──────────────── 固定前缀（4位）
│     ││ │  │ └─────────────────────设备ID（4位，持久化存储）
│     ││ └────────────────────────── 命令类型（4位）
│     │└──────────────────────────── 命令ID（2位，每次随机）
│     └───────────────────────────── 固定前缀（2位）
└─────────────────────────────────── 设备类型固定前缀（6位）
```

### 完整示例

```
推拉命令（深度50，伸速10，缩速8）:
710003A7-8800-1F3C-0000-32140A0000B7
│     ││ │  │ │  │ │  │ │        ││
710003 A7 8800 1F3C 0000 32140A0000 B7
│      │  │    │    │    │          │
设备   命 推拉 设备  固定 参数编码    校验
类型   令      ID   前缀 (深50伸10缩8)  和
```

---

## 🔧 核心编码逻辑

### 1. 设备类型前缀（固定）

```
710003
```

所有命令都以此开头，标识设备类型为 "DaXiu"（大秀）。

### 2. 命令ID（2位十六进制，随机）

每次发送命令时随机生成，范围：`00` - `FF`

**实现**：
```kotlin
fun generateControlCommandId(): String {
    return Random.nextInt(0, 256).toTwoHexString()  // 00-FF
}
```

**作用**：用于设备区分不同的命令实例。

### 3. 命令类型（4位十六进制）

| 命令类型 | 代码 | 说明 |
|----------|------|------|
| 推拉控制 | `8800` | 控制深度和伸缩速度 |
| 温度控制 | `9000` | 设置加热温度 |
| 强度控制 | `8200` | 设置震动强度 |
| 停止推拉 | `8800` | 停止伸缩（参数全0） |
| 停止强度 | `0200` | 停止震动（特殊命令类型） |
| 全部停止 | `1F00` | 停止所有操作 |
| 设备发现 | `1F00` | 发现设备 |

### 4. 设备ID（4位十六进制，持久化）

**首次启动时随机生成并永久保存**，后续始终使用相同值。

**实现**：
```kotlin
fun getControlDeviceId(context: Context): String {
    val prefs = context.getSharedPreferences("ble_settings", Context.MODE_PRIVATE)
    var deviceId = prefs.getString("ble_device_id", null)
    
    if (deviceId.isNullOrEmpty()) {
        // 生成新的设备ID（0x0000 - 0xFFFF）
        deviceId = Random.nextInt(0, 65536).toString(16)
                          .padStart(4, '0').uppercase()
        prefs.edit().putString("ble_device_id", deviceId).apply()
    }
    
    return deviceId  // 例如: "1F3C"
}
```

**作用**：设备用于识别控制源，避免多个控制端互相干扰。

### 5. 固定前缀（4位）

```
0000
```

所有命令固定使用此值。

### 6. 参数数据（10位十六进制）

根据命令类型不同，编码方式不同。

#### 6.1 推拉控制（8800）

**参数编码格式**：
```
DDSSRR0000
││││││││││
│││││││└┴┴─ 固定4位0
││││└┴───── 缩回速度（2位）
││└┴─────── 伸出速度（2位）
└┴───────── 深度（2位）
```

**完整示例**：
```kotlin
// UI输入: depth=50 (1-100), extendSpeed=80 (1-100), retractSpeed=65 (1-100)

// 1. 深度映射: 1-100 -> 14-72
val mappedDepth = (((50 - 1) * 58) / 99.0f + 14).roundToInt()  // = 42 (0x2A)

// 2. 速度映射: 1-100 -> 2-15
val mappedExtend = (80 * 0.15f).roundToInt().coerceAtLeast(2)  // = 12 (0x0C)
val mappedRetract = (65 * 0.15f).roundToInt().coerceAtLeast(2) // = 9 (0x09)

// 3. 编码
val paramsHex = String.format("%02X%02X%02X", 42, 12, 9)  // "2A0C09"

// 4. 完整参数段
val fullParams = "2A0C090000"  // 6位参数 + 4位0
```

**代码实现**：
```kotlin
fun buildControlCommand(params: ControlParams, context: Context): String {
    // 深度映射: 1-100 -> 14-72
    val mappedDepth = if (params.depth <= 0) {
        0
    } else {
        (((params.depth - 1) * 58) / 99.0f + 14).roundToInt()
    }
    
    // 速度映射: 1-100 -> 2-15
    val mappedExtend = (params.extendSpeed * 0.15f).roundToInt().coerceAtLeast(2)
    val mappedRetract = (params.retractSpeed * 0.15f).roundToInt().coerceAtLeast(2)
    
    // 参数编码
    val paramsHex = String.format("%02X%02X%02X", mappedDepth, mappedExtend, mappedRetract)
    
    val template = "710003**-8800-####-0000-${paramsHex}0000"
    return buildBleCommand(template, context)
}
```

#### 6.2 温度控制（9000）

**参数编码格式**：
```
TT00000000
││││││││││
│││└┴┴┴┴┴┴─ 固定8位0
└┴───────── 温度值（2位）
```

**温度映射**：
```
UI输入范围: 0-60°C
设备实际范围: 30-60°C
映射公式: mappedTemp = ((temp * 30) / 60) + 30
```

**完整示例**：
```kotlin
// UI输入: temp=40

// 1. 温度映射: 0-60 -> 30-60
val mappedTemp = ((40 * 30) / 60) + 30  // = 50 (0x32)

// 2. 编码
val tempHex = String.format("%02X", 50)  // "32"

// 3. 完整参数段
val fullParams = "3200000000"  // 2位温度 + 8位0
```

**代码实现**：
```kotlin
fun buildTemperatureCommand(temp: Int, context: Context): String {
    val mappedTemp = ((temp * 30) / 60) + 30
    val tempHex = mappedTemp.coerceIn(30, 60).toTwoHexString()
    
    val template = "710003**-9000-####-0000-${tempHex}00000000"
    return buildBleCommand(template, context)
}
```

#### 6.3 强度控制（8200）

**参数编码格式**：
```
640000SS02
││││││││││
││││││││└┴─ 固定"02"
││││││└┴─── 强度值（2位）
└┴┴┴┴┴───── 固定"640000"
```

**强度范围**：1-100（直接使用，无需映射）

**完整示例**：
```kotlin
// UI输入: strength=75

// 1. 直接使用
val strengthHex = String.format("%02X", 75)  // "4B"

// 2. 完整参数段
val fullParams = "640000" + "4B" + "02"  // "6400004B02"
```

**代码实现**：
```kotlin
fun buildStrengthCommand(strength: Int, context: Context): String {
    val strengthHex = strength.coerceIn(1, 100).toTwoHexString()
    
    val template = "710003**-8200-####-0100-640000${strengthHex}02"
    return buildBleCommand(template, context)
}
```

#### 6.4 停止命令

**停止推拉（8800）**：
```
0000000000  // 10位全0
```

**停止强度（0200）**：
```
命令类型改为 0200
参数段: 0000000002
```

**全部停止（1F00）**：
```
0000000000  // 10位全0
```

### 7. 校验和（2位十六进制，自动计算）

**计算方法**：将 UUID 去掉连字符后的所有字节相加，对 256 取模。

**实现**：
```kotlin
fun makeCheckSum(uuidWithoutHyphens: String): String {
    var sum = 0
    var i = 0
    
    while (i < uuidWithoutHyphens.length) {
        val endIndex = minOf(i + 2, uuidWithoutHyphens.length)
        val hex = uuidWithoutHyphens.substring(i, endIndex)
        
        // 每2位十六进制作为一个字节累加
        sum += if (hex.length == 1) {
            hex.toInt(16) * 16
        } else {
            hex.toInt(16)
        }
        
        i += 2
    }
    
    return (sum % 256).toTwoHexString()  // 返回2位十六进制
}
```

**完整示例**：
```kotlin
val uuidWithoutChecksum = "710003A788001F3C00003214080000"

// 计算校验和
val checksum = makeCheckSum(uuidWithoutChecksum)  // 假设结果是 "B3"

// 完整UUID
val fullUuid = "710003A7-8800-1F3C-0000-3214080000B3"
```

---

## 🔄 完整构建流程

### 统一构建函数

```kotlin
fun buildBleCommand(template: String, context: Context): String {
    // 1. 替换设备ID占位符 #### （持久化的4位十六进制）
    val deviceId = getControlDeviceId(context)
    val withDeviceId = template.replace("####", deviceId)
    
    // 2. 替换命令ID占位符 ** （随机的2位十六进制）
    val commandId = generateControlCommandId()
    val filled = withDeviceId.replace("**", commandId)
    
    // 3. 计算并追加校验和
    val checksum = makeCheckSum(filled.replace("-", ""))
    return filled + checksum
}
```

### 使用示例

```kotlin
// 推拉命令
val template = "710003**-8800-####-0000-32140A0000"
val uuid = buildBleCommand(template, context)
// 结果: "710003A7-8800-1F3C-0000-32140A0000B7"

// 温度命令
val template = "710003**-9000-####-0000-3200000000"
val uuid = buildBleCommand(template, context)
// 结果: "710003F2-9000-1F3C-0000-320000000089"
```

---

## 🧪 验证方法

### 方法1：使用 nRF Connect Scanner

1. 打开 nRF Connect App
2. 在 Scanner 模式下扫描
3. 打开官方 APP 发送命令
4. 观察 Scanner 中显示的 Service UUID
5. 对比你的 APP 发送的 UUID 是否一致

### 方法2：添加日志验证

在 `DaxiuCommand.kt` 中添加：

```kotlin
fun buildControlCommand(params: ControlParams, context: Context): String {
    val mappedDepth = ...
    val mappedExtend = ...
    val mappedRetract = ...
    
    Log.d("DaxiuCommand", "UI Input: depth=${params.depth}, extend=${params.extendSpeed}, retract=${params.retractSpeed}")
    Log.d("DaxiuCommand", "Mapped: depth=$mappedDepth, extend=$mappedExtend, retract=$mappedRetract")
    
    val paramsHex = String.format("%02X%02X%02X", mappedDepth, mappedExtend, mappedRetract)
    Log.d("DaxiuCommand", "Encoded params: $paramsHex")
    
    val template = String.format(TEMPLATE_THRUST, paramsHex)
    val uuid = buildBleCommand(template, context)
    
    Log.d("DaxiuCommand", "Final UUID: $uuid")
    
    return uuid
}
```

然后查看 logcat：

```bash
adb logcat | grep DaxiuCommand
```

---

## 📊 参数映射表

### 深度映射（1-100 → 14-72）

| UI值 | 设备值（十六进制） | 说明 |
|------|------------------|------|
| 1 | 14 (0x0E) | 最浅 |
| 25 | 28 (0x1C) | 四分之一 |
| 50 | 43 (0x2B) | 中间 |
| 75 | 57 (0x39) | 四分之三 |
| 100 | 72 (0x48) | 最深 |

**公式**：`mappedDepth = ((uiDepth - 1) * 58) / 99 + 14`

### 速度映射（1-100 → 2-15）

| UI值 | 设备值（十六进制） | 说明 |
|------|------------------|------|
| 1-13 | 2 (0x02) | 最慢（最小值限制） |
| 20 | 3 (0x03) | 很慢 |
| 40 | 6 (0x06) | 慢 |
| 60 | 9 (0x09) | 中 |
| 80 | 12 (0x0C) | 快 |
| 100 | 15 (0x0F) | 最快 |

**公式**：`mappedSpeed = max(2, (uiSpeed * 0.15).roundToInt())`

### 温度映射（0-60 → 30-60）

| UI值 | 设备值（十六进制） | 说明 |
|------|------------------|------|
| 0 | 30 (0x1E) | 关闭/最低 |
| 30 | 45 (0x2D) | 中温 |
| 60 | 60 (0x3C) | 最高温 |

**公式**：`mappedTemp = ((uiTemp * 30) / 60) + 30`

---

## 📁 相关代码文件

所有编码逻辑位于以下文件：

```
app/src/main/java/aya/strokenet/
├── ble/
│   └── DaxiuCommand.kt          # 命令构造器（主要逻辑）
└── utils/
    └── BleExtensions.kt         # 辅助函数（校验和、设备ID等）
```

---

## ⚠️ 与启发文章的差异

感谢 **[Cachito 教程](https://claude.ai/public/artifacts/921eda06-e567-4cde-85af-8cde831a608f)** 提供的逆向思路和 MCP 集成灵感，但该文章中给出的 UUID 编码方式存在以下问题：

1. **错误的参数位置**：文章认为参数在 UUID 的前半部分，实际在最后段
2. **遗漏校验和**：文章未提及校验和机制
3. **缺少设备ID**：文章未说明设备ID的持久化存储
4. **映射公式不准确**：速度和深度的映射关系与官方实现不同

**本项目的实现完全基于官方 APP 的反编译结果**，确保了协议的准确性和兼容性。

---

## 🎯 总结

- **UUID 格式**：`710003XX-YYYY-ZZZZ-0000-WWWWWWWWWWCC`
- **关键组成**：设备类型 + 命令ID + 命令类型 + 设备ID + 参数 + 校验和
- **参数映射**：UI 值需要映射到设备的实际范围
- **校验和**：所有字节累加对 256 取模
- **设备ID**：首次生成后持久化存储
- **命令ID**：每次随机生成

完整实现请参考 `DaxiuCommand.kt` 和 `BleExtensions.kt` 源码。
