# StrokeNet 2.0 - LLM 开发指南

> 📌 **本文档专为 LLM（AI Agent/编程助手）设计**  
> 提供项目全景视图、快速定位、开发决策支持

**版本**: 2.0.1  
**最后更新**: 2026-06-03  
**状态**: ✅ 生产就绪

---

## 🎯 一句话总结

**StrokeNet 是一个基于 BLE 广播协议的 Android 控制应用，内置官方 Kotlin MCP SDK，支持 AI 原生控制，提供完整的自定义预设管理系统。**

---

## 📊 项目核心信息

### 技术栈

```yaml
平台: Android 7+ (API 24+)
语言: Kotlin 2.0
UI: Jetpack Compose (Material 3)
架构: MVVM + Repository Pattern
关键技术:
  - BluetoothLeAdvertiser (BLE 广播)
  - Foreground Service (前台服务)
  - Kotlin MCP SDK 0.6.0 (官方)
  - Ktor 3.0 (HTTP Server)
  - Kotlin Serialization (JSON)
```

### 核心功能矩阵

| 功能 | 实现状态 | 关键文件 | 备注 |
|------|---------|---------|------|
| BLE 广播控制 | ✅ | `BleAdvertiser.kt` | 基于官方 APP 反编译 |
| 前台服务 | ✅ | `BleService.kt` | 自动重试 2 次 |
| 内置 MCP Server | ✅ | `McpServerService.kt` | 官方 SDK 实现 |
| 自定义预设 | ✅ | `PresetRepository.kt` | JSON 导入导出 |
| 预设循环播放 | ✅ | `LoopPresetService.kt` | 后台循环 |
| 加热定时器 | ✅ | `HeatingTimerService.kt` | 1-10 分钟 |
| MCP 预设管理 | ✅ | `McpServerFactory.kt` | 20+ 工具 |

---

## 🗂️ 项目结构（关键路径）

```
StrokeNet/
├── app/src/main/java/aya/strokenet/
│   ├── MainActivity.kt                    # 主入口、预设预加载
│   ├── BleService.kt                      # BLE 前台服务
│   ├── LoopPresetService.kt               # 预设循环服务
│   ├── HeatingTimerService.kt             # 加热定时服务
│   ├── McpServerService.kt                # MCP HTTP 服务
│   │
│   ├── ble/
│   │   ├── DaxiuBleAdvertiser.kt          # BLE 广播核心 ⚠️
│   │   ├── DaxiuCommand.kt                # 命令构建
│   │   └── SingleBleAdvertiser.kt         # 单次广播
│   │
│   ├── mcp/
│   │   └── McpServerFactory.kt            # MCP 工具注册（20+ 工具）
│   │
│   ├── data/
│   │   ├── model/
│   │   │   ├── ControlParams.kt           # 参数定义
│   │   │   ├── Preset.kt                  # 预设数据模型
│   │   │   └── PresetCommand.kt           # 预设命令
│   │   └── repository/
│   │       └── PresetRepository.kt        # 预设数据仓库
│   │
│   └── ui/
│       ├── screens/
│       │   ├── ControlScreen.kt           # 控制页面
│       │   ├── PresetsScreen.kt           # 官方预设
│       │   ├── CustomPresetsScreen.kt     # 自定义预设
│       │   ├── PresetEditorScreen.kt      # 预设编辑器
│       │   ├── McpScreen.kt               # MCP 管理界面
│       │   └── SettingsScreen.kt          # 设置页面
│       └── viewmodel/
│           ├── ControlViewModel.kt        # 控制逻辑
│           ├── PresetViewModel.kt         # 预设逻辑
│           └── McpViewModel.kt            # MCP 状态管理
│
├── app/src/main/assets/
│   └── presets.json                       # 官方预设配置
│
├── docs/
│   ├── LLM.md                             # 本文档 ⭐
│   ├── README.md                          # 用户文档
│   ├── UUID_ENCODING.md                   # 协议详解 ⚠️ 必读
│   ├── MCP_QUICKSTART.md                  # 快速开始
│   ├── MCP_PRESET_GUIDE.md                # 预设指南
│   └── ...
│
└── mcp/                                    # Termux MCP 脚本（可选）
    ├── daxiu_mcp_auto.py
    ├── daxiu_mcp_http.py
    └── daxiu_mcp_full.py
```

---

## 🔑 关键决策和约束

### 1. UUID 编码协议 ⚠️ **极其重要**

```kotlin
// 格式：710003XX-YYYY-ZZZZ-0000-WWWWWWWWWWCC
// 
// XX: 设备 ID（SharedPreferences 持久化）
// YYYY: 固定 8800
// ZZZZ: 动态序列号（递增）
// WWWWWWWWWW: 参数编码（10 位十六进制）
//   - 前 2 位：深度（depth）
//   - 3-4 位：伸展速度（extend）
//   - 5-6 位：收缩速度（retract）
//   - 7-8 位：强度（strength）
//   - 9-10 位：温度（temp）
// CC: 校验和（所有参数字节之和 & 0xFF）
```

**来源**: 官方 APP 反编译 + nRF Connect 抓包验证  
**文档**: `docs/UUID_ENCODING.md`  
**代码**: `ble/DaxiuCommand.kt:buildCommandUuid()`

### 2. 参数范围映射

| UI 范围 | 设备范围 | 说明 |
|---------|---------|------|
| 1-100 (depth) | 0x01-0x64 | 直接映射 |
| 1-100 (extend/retract) | 0x01-0x64 | 直接映射 |
| 1-100 (strength) | 0x01-0x64 | 直接映射 |
| 0-60 (temp) | 0x00-0x3C | 温度（℃）|

### 3. 前台服务架构

```
用户操作/MCP 调用
    ↓
启动 BleService (前台)
    ↓
单次指令 → SingleBleAdvertiser
循环预设 → LoopPresetService
加热定时 → HeatingTimerService
    ↓
BLE 广播（失败自动重试 2 次）
    ↓
通知栏反馈 + 自动停止
```

**重试逻辑**:
- 最多重试 2 次
- 每次间隔 300ms
- 失败后保持通知 3 秒

### 4. MCP 工具设计原则

```kotlin
// 所有 MCP 工具遵循统一模式：
server.addTool(
    name = "tool_name",
    description = "工具描述",
    inputSchema = ToolSchema(
        properties = buildJsonObject { /* 参数定义 */ },
        required = listOf(/* 必填参数 */)
    )
) { request ->
    try {
        // 1. 解析参数
        // 2. 验证范围
        // 3. 启动 Service
        // 4. 返回成功响应
    } catch (e: Exception) {
        // 返回错误响应
    }
}
```

**工具分类**:
- **控制类** (9个): thrust, strength, temperature, send_all, stop_*
- **加热类** (2个): start_heating, stop_heating
- **预设类** (9个): list, get_*, create, update, delete, run, stop, export, import

### 5. 预设数据格式

```json
{
  "id": "uuid-or-slug",
  "name": "预设名称",
  "description": "预设描述",
  "commands": [
    {
      "command": "710003**-8800-####-0000-322828280000",
      "time": 2000
    }
  ],
  "isCustom": true
}
```

**存储位置**:
- 官方预设: `assets/presets.json`（只读）
- 自定义预设: `SharedPreferences`（可读写）

---

## 🚀 快速开发指南

### 添加新的 MCP 工具

1. **打开**: `mcp/McpServerFactory.kt`
2. **定位**: `registerTools()` 函数
3. **添加工具**:

```kotlin
server.addTool(
    name = "new_tool",
    description = "新工具描述",
    inputSchema = ToolSchema(
        properties = buildJsonObject {
            putJsonObject("param") {
                put("type", "number")
                put("description", "参数描述")
                put("minimum", 1)
                put("maximum", 100)
            }
        },
        required = listOf("param")
    )
) { request ->
    try {
        val param = request.arguments?.get("param")?.jsonPrimitive?.int
            ?: throw IllegalArgumentException("缺少 param")
        
        // 调用 Service
        val intent = Intent(context, BleService::class.java).apply {
            putExtra("action", BleService.ACTION_CUSTOM)
            putExtra("param", param)
        }
        context.startForegroundService(intent)
        
        CallToolResult(
            content = listOf(TextContent(text = "✓ 执行成功"))
        )
    } catch (e: Exception) {
        CallToolResult(
            content = listOf(TextContent(text = "✗ 失败: ${e.message}")),
            isError = true
        )
    }
}
```

### 修改 UUID 编码（如果协议变更）

1. **使用 nRF Connect 抓取新的 UUID**
2. **分析字节位变化规律**
3. **修改**: `ble/DaxiuCommand.kt:buildCommandUuid()`
4. **测试**: 对比不同参数值的 UUID 输出
5. **文档**: 更新 `docs/UUID_ENCODING.md`

### 添加新的预设到官方列表

1. **编辑**: `app/src/main/assets/presets.json`
2. **格式**:

```json
{
  "official": [
    {
      "id": "new-preset",
      "name": "新预设",
      "description": "描述",
      "commands": [
        {
          "command": "710003**-8800-####-0000-DDEERRSS00CC",
          "time": 2000
        }
      ],
      "isCustom": false
    }
  ]
}
```

3. **重新编译**: 预设会在应用启动时自动加载

### 调试技巧

```bash
# 查看所有日志
adb logcat -s MainActivity BleService LoopPresetService HeatingTimerService McpServerService

# 查看 BLE 广播
adb logcat -s DaxiuBleAdvertiser

# 查看 MCP 请求
adb logcat -s McpServerFactory

# 查看预设加载
adb logcat -s PresetRepository

# 测试 MCP 健康检查
curl http://192.168.x.x:8080/

# 测试 MCP 工具列表
npx @modelcontextprotocol/inspector http://192.168.x.x:8080/mcp
```

---

## ⚠️ 常见陷阱

### 1. 推拉参数必须同时设置

```kotlin
// ❌ 错误：只设置深度
am("thrust", depth=50)  // 设备不会响应

// ✅ 正确：同时设置三个参数
am("thrust", depth=50, extend=60, retract=60)
```

### 2. UUID 编码顺序

```kotlin
// ❌ 错误：参数位置错误
"710003XX-8800-####-0000-${extend}${depth}${retract}${strength}${temp}${checksum}"

// ✅ 正确：按照协议顺序
"710003XX-8800-####-0000-${depth}${extend}${retract}${strength}${temp}${checksum}"
```

### 3. MCP 路径配置

```json
// ❌ 错误：缺少 /mcp 路径
{"url": "http://192.168.1.5:8080"}

// ✅ 正确：包含 /mcp
{"url": "http://192.168.1.5:8080/mcp"}
```

### 4. 自定义预设 ID 冲突

```kotlin
// ❌ 错误：使用固定 ID
val preset = LoopPreset(id = "my-preset", ...)

// ✅ 正确：使用 UUID
val preset = LoopPreset(
    id = java.util.UUID.randomUUID().toString(),
    ...
)
```

### 5. Service 未启动问题

```kotlin
// ❌ 错误：直接调用 startService (Android 8+)
context.startService(intent)

// ✅ 正确：使用 startForegroundService
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    context.startForegroundService(intent)
} else {
    context.startService(intent)
}
```

---

## 📚 文档导航

### 按需求查找

| 需求 | 文档 | 用途 |
|------|------|------|
| 快速了解项目 | `README.md` | 用户视角 |
| LLM 开发支持 | `LLM.md` (本文档) | 全景视图 |
| MCP 快速开始 | `MCP_QUICKSTART.md` | 5分钟上手 |
| MCP 完整功能 | `MCP_INTEGRATION_GUIDE.md` | 详细文档 |
| 预设管理 | `MCP_PRESET_GUIDE.md` | 创建/编辑 |
| UUID 协议 | `UUID_ENCODING.md` | ⚠️ 必读 |
| 服务架构 | `SERVICE_INTEGRATION.md` | 技术细节 |
| 发布打包 | `RELEASE_BUILD_GUIDE.md` | 构建指南 |
| Termux 方式 | `TERMUX_MCP_GUIDE.md` | 可选方案 |

### 文档更新优先级

1. **UUID_ENCODING.md** - 协议变更时立即更新
2. **LLM.md** (本文档) - 架构变化时更新
3. **MCP_PRESET_GUIDE.md** - 新增工具时更新
4. **README.md** - 版本发布时更新

---

## 🧠 架构决策记录 (ADR)

### ADR-001: 使用官方 Kotlin MCP SDK

**决策**: 使用官方 `kotlin-sdk-server 0.6.0` 而非 Python fastMCP

**原因**:
- 原生 Android 集成，无需外部依赖
- 标准协议实现，兼容性好
- 性能优于跨进程调用
- 易于维护和调试

**影响**:
- ✅ 一键启动，用户体验好
- ✅ 稳定性高
- ❌ 自定义工具需要重新编译

### ADR-002: 预设存储方案

**决策**: 官方预设 assets + 自定义预设 SharedPreferences

**原因**:
- 官方预设不可修改，保证稳定性
- 自定义预设轻量级存储，无需数据库
- 支持 JSON 导入导出，易于分享

**影响**:
- ✅ 简单可靠
- ✅ 快速加载
- ❌ 大量预设时可能性能下降（可优化为 Room）

### ADR-003: 多 Service 架构

**决策**: 
- BleService: 单次指令
- LoopPresetService: 循环播放
- HeatingTimerService: 加热定时

**原因**:
- 职责分离，易于维护
- 独立生命周期，互不干扰
- 可单独停止某项功能

**影响**:
- ✅ 架构清晰
- ✅ 扩展性好
- ⚠️ 略微增加复杂度

---

## 🔮 未来优化方向

### 短期（1-2 周）
- [ ] MCP 工具添加认证机制
- [ ] 预设编辑器增强（拖拽排序）
- [ ] 更多官方预设

### 中期（1-2 月）
- [ ] 自定义预设迁移到 Room 数据库
- [ ] WebSocket 支持（实时状态推送）
- [ ] 场景模式（多设备协同）

### 长期（3+ 月）
- [ ] 社区预设市场
- [ ] AI 自动生成预设
- [ ] 云端同步

---

## 💡 开发小贴士

### 快速定位代码

```bash
# 查找 MCP 工具定义
grep -r "server.addTool" app/src/

# 查找 BLE 指令发送
grep -r "advertise(" app/src/

# 查找预设相关
grep -r "PresetRepository" app/src/

# 查找 Service 启动
grep -r "startForegroundService" app/src/
```

### 快速测试流程

```bash
# 1. 编译安装
./gradlew installDebug

# 2. 授予权限
adb shell pm grant aya.strokenet android.permission.BLUETOOTH_ADVERTISE
adb shell pm grant aya.strokenet android.permission.BLUETOOTH_CONNECT

# 3. 启动应用
adb shell am start -n aya.strokenet/.MainActivity

# 4. 测试 Intent
adb shell am start -n aya.strokenet/.MainActivity \
  --es action start --ei depth 50 --ei extend 60 --ei retract 60

# 5. 查看日志
adb logcat -s MainActivity BleService
```

### 性能优化检查点

- BLE 广播频率（不要过高）
- 预设循环间隔（建议 >= 500ms）
- 通知更新频率（不要过频）
- JSON 解析性能（大文件考虑流式）

---

## 🆘 问题排查流程

### MCP Server 无法启动

1. 检查端口占用：`adb shell netstat -tuln | grep 8080`
2. 查看日志：`adb logcat -s McpServerService`
3. 验证权限：网络权限是否授予

### BLE 广播无响应

1. 使用 nRF Connect 验证广播是否发出
2. 检查 UUID 格式：`adb logcat -s DaxiuBleAdvertiser`
3. 验证参数范围是否正确
4. 确认设备是否在 BLE 接收模式

### 预设加载失败

1. 检查 `assets/presets.json` 格式
2. 查看日志：`adb logcat -s PresetRepository`
3. 验证 JSON 语法（使用 jsonlint）

### MCP 工具调用失败

1. 测试健康检查：`curl http://IP:8080/`
2. 检查参数类型和范围
3. 查看 MCP 日志：`adb logcat -s McpServerFactory`
4. 使用 MCP Inspector 调试

---

## 📖 代码示例速查

### 发送单次 BLE 指令

```kotlin
val intent = Intent(context, BleService::class.java).apply {
    putExtra("action", BleService.ACTION_THRUST)
    putExtra(BleService.EXTRA_DEPTH, 50)
    putExtra(BleService.EXTRA_EXTEND, 60)
    putExtra(BleService.EXTRA_RETRACT, 60)
}
context.startForegroundService(intent)
```

### 运行循环预设

```kotlin
val presetJson = Json.encodeToString(LoopPreset.serializer(), preset)
val intent = Intent(context, LoopPresetService::class.java).apply {
    putExtra("action", LoopPresetService.ACTION_START_LOOP)
    putExtra(LoopPresetService.EXTRA_PRESET_JSON, presetJson)
}
context.startForegroundService(intent)
```

### 启动加热定时器

```kotlin
val intent = Intent(context, HeatingTimerService::class.java).apply {
    putExtra("action", HeatingTimerService.ACTION_START_TIMER)
    putExtra(HeatingTimerService.EXTRA_DURATION_MINUTES, 5)
    putExtra(HeatingTimerService.EXTRA_TEMPERATURE, 35)
}
context.startForegroundService(intent)
```

### 创建自定义预设

```kotlin
val preset = LoopPreset(
    id = UUID.randomUUID().toString(),
    name = "自定义预设",
    description = "描述",
    commands = listOf(
        PresetCommand(
            command = "710003**-8800-####-0000-322828280000",
            time = 2000
        )
    ),
    isCustom = true
)
repository.addCustomPreset(preset)
```

---

## 🎯 总结：关键知识点

1. **UUID 协议是核心** - 所有控制基于正确的 UUID 编码
2. **多 Service 架构** - 单次/循环/定时分离
3. **官方 MCP SDK** - 标准实现，易于维护
4. **预设系统完整** - 官方 + 自定义 + 导入导出
5. **前台服务保活** - 自动重试，通知反馈

---

**文档维护**: LLM 在进行重大架构变更后应主动更新本文档  
**版本**: 2.0.1  
**最后更新**: 2026-06-03

---

*本文档由 AI 生成，专为 LLM 设计*
