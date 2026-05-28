# UUID 编码配置指南

## 概述

设备通过 BLE 广播接收控制指令，指令编码在 128 位 Service UUID 中。你需要根据实际抓包结果调整 `BleAdvertiser.kt` 中的编码逻辑。

## UUID 格式

```
710003f8-1f00-bbbf-XXXX-XXXXXXXXXXXX
│                  │    │
│                  │    └─ 参数2 (12位十六进制)
│                  └────── 参数1 (4位十六进制)
└───────────────────────── 固定前缀
```

## 逆向步骤

### 1. 抓取 UUID

使用 nRF Connect Scanner 抓取每个操作对应的 UUID：

| 操作 | 参数值 | 抓取到的 UUID 示例 |
|------|--------|-------------------|
| 推拉深度 0 | depth=0 | 710003f8-1f00-bbbf-0000-000000000001 |
| 推拉深度 36 | depth=36 | 710003f8-1f00-bbbf-2400-000000000001 |
| 推拉深度 72 | depth=72 | 710003f8-1f00-bbbf-4800-000000000001 |
| 伸出速度 0 | extend=0 | 710003f8-1f00-bbbf-0000-000000000001 |
| 伸出速度 8 | extend=8 | 710003f8-1f00-bbbf-0080-000000000001 |
| 伸出速度 15 | extend=15 | 710003f8-1f00-bbbf-00f0-000000000001 |
| 强度 50 | strength=50 | 710003f8-1f00-bbbf-1032-000000000003 |
| 温度 40 | temp=40 | 710003f8-1f00-bbbf-2028-000000000004 |
| 停止 | - | 710003f8-1f00-bbbf-0000-000000000005 |

### 2. 分析字节位

对比 UUID，找出变化规律：

#### 参数1 部分 (XXXX)
```
depth=0:  0000
depth=36: 2400  (0x24 = 36)
depth=72: 4800  (0x48 = 72)

extend=0:  XX00
extend=8:  XX80  (0x8 << 4 = 0x80)
extend=15: XXf0  (0xf << 4 = 0xf0)

retract=0:  XX0X
retract=8:  XX8X  (0x8)
retract=15: XXfX  (0xf)
```

可能的编码方式：
```
XXXX = [depth高8位][depth低8位 | extend<<4 | retract]
```

#### 参数2 部分 (XXXXXXXXXXXX)
```
start:    000000000001
thrust:   000000000002
strength: 000000000003
temp:     000000000004
stop:     000000000005
```

这部分似乎是操作类型标识。

### 3. 调整编码函数

根据分析结果，修改 `BleAdvertiser.kt` 中的 `buildControlUuid` 函数：

```kotlin
private fun buildControlUuid(
    action: String,
    depth: Int = 0,
    extendSpeed: Int = 0,
    retractSpeed: Int = 0,
    strength: Int = 0,
    temp: Int = 0
): UUID {
    val param1 = when (action) {
        "start", "thrust" -> {
            // 示例编码：根据实际抓包调整
            // 高字节：depth
            // 低字节：(extend << 4) | retract
            val highByte = depth and 0xFF
            val lowByte = ((extendSpeed and 0x0F) shl 4) or (retractSpeed and 0x0F)
            (highByte shl 8) or lowByte
        }
        "strength" -> {
            // 强度编码：根据实际抓包调整
            0x1000 or (strength and 0xFF)
        }
        "temp" -> {
            // 温度编码：根据实际抓包调整
            0x2000 or (temp and 0xFF)
        }
        "stop" -> 0x0000
        else -> 0x0000
    }
    
    val param2 = when (action) {
        "start" -> 0x000000000001L
        "thrust" -> 0x000000000002L
        "strength" -> 0x000000000003L
        "temp" -> 0x000000000004L
        "stop" -> 0x000000000005L
        else -> 0x000000000000L
    }
    
    val uuidString = String.format(UUID_TEMPLATE, param1, param2)
    return UUID.fromString(uuidString)
}
```

## 验证方法

### 方法1：使用 nRF Connect Advertiser

1. 在 nRF Connect 中切换到 Advertiser 模式
2. 添加 Service UUID
3. 输入构造的 UUID
4. 开始广播
5. 观察设备是否响应

### 方法2：使用本 APP

1. 修改 `BleAdvertiser.kt` 中的编码逻辑
2. 重新编译安装 APP
3. 使用 Intent 发送测试指令
4. 观察设备响应
5. 使用 nRF Connect Scanner 抓取 APP 发出的广播，对比是否正确

## 调试技巧

### 1. 添加日志

在 `buildControlUuid` 函数中添加详细日志：

```kotlin
Log.d(TAG, "Action: $action")
Log.d(TAG, "Params: depth=$depth, extend=$extendSpeed, retract=$retractSpeed")
Log.d(TAG, "Encoded param1: 0x${param1.toString(16).padStart(4, '0')}")
Log.d(TAG, "Encoded param2: 0x${param2.toString(16).padStart(12, '0')}")
Log.d(TAG, "Final UUID: $uuidString")
```

### 2. 使用 Logcat

```bash
adb logcat | grep BleAdvertiser
```

### 3. 对比验证

创建一个对照表，记录：
- 官方 APP 发送的 UUID
- 本 APP 发送的 UUID
- 设备响应情况

## 常见编码模式

### 模式1：直接映射
```kotlin
// 参数值直接对应字节
val param1 = depth  // 0-72 直接映射
```

### 模式2：位移组合
```kotlin
// 多个参数打包到一个字段
val param1 = (depth shl 8) or (speed and 0xFF)
```

### 模式3：查表映射
```kotlin
// 使用预定义的映射表
val depthMap = mapOf(
    0 to 0x00,
    36 to 0x24,
    72 to 0x48
)
val param1 = depthMap[depth] ?: 0x00
```

### 模式4：校验和
```kotlin
// 包含校验位
val checksum = (depth + extendSpeed + retractSpeed) and 0xFF
val param1 = (depth shl 8) or checksum
```

## 注意事项

1. **字节序**：注意大端序（Big-Endian）和小端序（Little-Endian）
2. **范围检查**：确保参数值在有效范围内
3. **固定位**：某些字节位可能是固定值或校验位
4. **组合参数**：推拉三参数必须同时编码
5. **超时设置**：广播超时时间影响设备响应

## 示例：完整的编码分析

假设抓取到以下 UUID：

```
depth=36, extend=8, retract=8:
710003f8-1f00-bbbf-2488-000000000001
                    ││││
                    │││└─ retract = 8 (0x8)
                    ││└── extend = 8 (0x8)
                    │└─── depth低位 = 36 (0x24)
                    └──── depth高位 = 0
```

编码函数：
```kotlin
val param1 = ((depth and 0xFF) shl 8) or 
             ((extendSpeed and 0x0F) shl 4) or 
             (retractSpeed and 0x0F)
// 36 << 8 = 0x2400
// 8 << 4 = 0x80
// 8 = 0x08
// 结果: 0x2488
```

## 需要调整的文件

只需要修改一个文件：
- `app/src/main/java/com/ec/strokenet/BleAdvertiser.kt`

具体修改 `buildControlUuid` 函数中的编码逻辑。

## 获取帮助

如果编码逻辑不正确：
1. 提供完整的抓包 UUID 列表
2. 说明每个 UUID 对应的操作和参数值
3. 我可以帮你分析编码规律并生成正确的代码
