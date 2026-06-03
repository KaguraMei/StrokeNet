package aya.strokenet.mcp

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import aya.strokenet.BleService
import aya.strokenet.HeatingTimerService
import aya.strokenet.LoopPresetService
import aya.strokenet.data.repository.PresetRepository
import aya.strokenet.data.model.LoopPreset
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.ServerOptions
import io.modelcontextprotocol.kotlin.sdk.types.*
import kotlinx.serialization.json.*

/**
 * 创建 StrokeNet MCP Server
 * 使用官方 Kotlin MCP SDK
 */
fun createStrokeNetMcpServer(context: Context): Server {
    val server = Server(
        serverInfo = Implementation(
            name = "StrokeNet",
            version = "1.1.0"
        ),
        options = ServerOptions(
            capabilities = ServerCapabilities(
                tools = ServerCapabilities.Tools(listChanged = false)
            )
        )
    )
    
    // 注册所有工具
    registerTools(server, context)
    
    return server
}

/**
 * 注册所有 MCP 工具
 */
private fun registerTools(server: Server, context: Context) {
    
    // 1. 推拉控制
    server.addTool(
        name = "thrust",
        description = "控制设备推拉运动（深度、伸展速度、收缩速度）",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("depth") {
                    put("type", "number")
                    put("description", "推拉深度 (1-100)")
                    put("minimum", 1)
                    put("maximum", 100)
                }
                putJsonObject("extend") {
                    put("type", "number")
                    put("description", "伸展速度 (1-100)")
                    put("minimum", 1)
                    put("maximum", 100)
                }
                putJsonObject("retract") {
                    put("type", "number")
                    put("description", "收缩速度 (1-100)")
                    put("minimum", 1)
                    put("maximum", 100)
                }
            },
            required = listOf("depth", "extend", "retract")
        )
    ) { request ->
        try {
            val args = request.arguments
            val depth = args?.get("depth")?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("缺少 depth")
            val extend = args["extend"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("缺少 extend")
            val retract = args["retract"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("缺少 retract")
            
            // 验证范围
            if (depth !in 1..100 || extend !in 1..100 || retract !in 1..100) {
                throw IllegalArgumentException("参数超出范围 (1-100)")
            }
            
            val intent = Intent(context, BleService::class.java).apply {
                putExtra("action", BleService.ACTION_THRUST)
                putExtra(BleService.EXTRA_DEPTH, depth)
                putExtra(BleService.EXTRA_EXTEND, extend)
                putExtra(BleService.EXTRA_RETRACT, retract)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            CallToolResult(
                content = listOf(
                    TextContent(text = "✓ 已发送推拉指令: 深度=$depth, 伸=$extend, 缩=$retract")
                )
            )
        } catch (e: Exception) {
            Log.e("McpServer", "Thrust tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 2. 震动强度
    server.addTool(
        name = "strength",
        description = "设置震动强度",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("value") {
                    put("type", "number")
                    put("description", "震动强度 (1-100)")
                    put("minimum", 1)
                    put("maximum", 100)
                }
            },
            required = listOf("value")
        )
    ) { request ->
        try {
            val args = request.arguments
            val value = args?.get("value")?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("缺少 value")
            
            if (value !in 1..100) {
                throw IllegalArgumentException("参数超出范围 (1-100)")
            }
            
            val intent = Intent(context, BleService::class.java).apply {
                putExtra("action", BleService.ACTION_STRENGTH)
                putExtra(BleService.EXTRA_VALUE, value)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            CallToolResult(
                content = listOf(TextContent(text = "✓ 已设置震动强度: $value"))
            )
        } catch (e: Exception) {
            Log.e("McpServer", "Strength tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 3. 加热定时器（设置温度 + 启动定时器，时长必填）
    server.addTool(
        name = "start_heating",
        description = "启动定时加热（设置温度并在指定时长后自动关闭）",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("temperature") {
                    put("type", "number")
                    put("description", "加热温度 (1-60°C)")
                    put("minimum", 1)
                    put("maximum", 60)
                }
                putJsonObject("duration") {
                    put("type", "number")
                    put("description", "加热时长（分钟，1-10）")
                    put("minimum", 1)
                    put("maximum", 10)
                }
            },
            required = listOf("temperature", "duration")
        )
    ) { request ->
        try {
            val args = request.arguments
            val temperature = args?.get("temperature")?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("缺少 temperature")
            val duration = args["duration"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("缺少 duration")
            
            if (temperature !in 1..60) {
                throw IllegalArgumentException("温度超出范围 (1-60)")
            }
            if (duration !in 1..10) {
                throw IllegalArgumentException("时长超出范围 (1-10)")
            }
            
            // 启动加热定时器服务
            val intent = Intent(context, HeatingTimerService::class.java).apply {
                putExtra("action", HeatingTimerService.ACTION_START_TIMER)
                putExtra(HeatingTimerService.EXTRA_DURATION_MINUTES, duration)
                putExtra(HeatingTimerService.EXTRA_TEMPERATURE, temperature)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            CallToolResult(
                content = listOf(
                    TextContent(text = "✓ 加热已启动: ${temperature}°C，将在 $duration 分钟后自动关闭")
                )
            )
        } catch (e: Exception) {
            Log.e("McpServer", "StartHeating tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 4. 停止加热定时器
    server.addTool(
        name = "stop_heating",
        description = "停止加热定时器",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        try {
            val intent = Intent(context, HeatingTimerService::class.java).apply {
                putExtra("action", HeatingTimerService.ACTION_STOP_TIMER)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            CallToolResult(
                content = listOf(TextContent(text = "✓ 加热定时器已停止"))
            )
        } catch (e: Exception) {
            Log.e("McpServer", "StopHeating tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 5. 全参数发送（不包含加热，因为加热需要通过 start_heating 带时长）
    server.addTool(
        name = "send_all",
        description = "批量发送所有参数（推拉+震动，可选温度）",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("depth") {
                    put("type", "number")
                    put("description", "推拉深度 (1-100)")
                    put("minimum", 1)
                    put("maximum", 100)
                }
                putJsonObject("extend") {
                    put("type", "number")
                    put("description", "伸展速度 (1-100)")
                    put("minimum", 1)
                    put("maximum", 100)
                }
                putJsonObject("retract") {
                    put("type", "number")
                    put("description", "收缩速度 (1-100)")
                    put("minimum", 1)
                    put("maximum", 100)
                }
                putJsonObject("strength") {
                    put("type", "number")
                    put("description", "震动强度 (1-100)")
                    put("minimum", 1)
                    put("maximum", 100)
                }
                putJsonObject("temperature") {
                    put("type", "number")
                    put("description", "温度值 (0-60°C，0表示不加热)")
                    put("minimum", 0)
                    put("maximum", 60)
                    put("default", 0)
                }
            },
            required = listOf("depth", "extend", "retract", "strength")
        )
    ) { request ->
        try {
            val args = request.arguments
            val depth = args?.get("depth")?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("缺少 depth")
            val extend = args["extend"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("缺少 extend")
            val retract = args["retract"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("缺少 retract")
            val strength = args["strength"]?.jsonPrimitive?.int
                ?: throw IllegalArgumentException("缺少 strength")
            val temperature = args["temperature"]?.jsonPrimitive?.int ?: 0
            
            // 验证范围
            if (depth !in 1..100 || extend !in 1..100 || retract !in 1..100 || strength !in 1..100) {
                throw IllegalArgumentException("推拉/震动参数超出范围 (1-100)")
            }
            if (temperature !in 0..60) {
                throw IllegalArgumentException("温度超出范围 (0-60)")
            }
            
            val intent = Intent(context, BleService::class.java).apply {
                putExtra("action", BleService.ACTION_SEND_ALL)
                putExtra(BleService.EXTRA_DEPTH, depth)
                putExtra(BleService.EXTRA_EXTEND, extend)
                putExtra(BleService.EXTRA_RETRACT, retract)
                putExtra(BleService.EXTRA_STRENGTH, strength)
                putExtra(BleService.EXTRA_TEMP, temperature)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            val tempInfo = if (temperature > 0) " 温度=${temperature}°C" else ""
            CallToolResult(
                content = listOf(
                    TextContent(
                        text = "✓ 已发送全部参数: 深度=$depth 伸=$extend 缩=$retract 强度=$strength$tempInfo"
                    )
                )
            )
        } catch (e: Exception) {
            Log.e("McpServer", "SendAll tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 6. 停止推拉
    server.addTool(
        name = "stop_thrust",
        description = "停止推拉运动",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        try {
            val intent = Intent(context, BleService::class.java).apply {
                putExtra("action", BleService.ACTION_STOP_THRUST)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            CallToolResult(
                content = listOf(TextContent(text = "✓ 已停止推拉"))
            )
        } catch (e: Exception) {
            Log.e("McpServer", "StopThrust tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 7. 停止震动
    server.addTool(
        name = "stop_strength",
        description = "停止震动",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        try {
            val intent = Intent(context, BleService::class.java).apply {
                putExtra("action", BleService.ACTION_STOP_STRENGTH)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            CallToolResult(
                content = listOf(TextContent(text = "✓ 已停止震动"))
            )
        } catch (e: Exception) {
            Log.e("McpServer", "StopStrength tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 9. 全部停止
    server.addTool(
        name = "stop_all",
        description = "停止所有运动（推拉+震动）",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        try {
            val intent = Intent(context, BleService::class.java).apply {
                putExtra("action", BleService.ACTION_STOP_ALL)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            CallToolResult(
                content = listOf(TextContent(text = "✓ 已停止所有运动"))
            )
        } catch (e: Exception) {
            Log.e("McpServer", "StopAll tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // === 预设管理工具 ===
    
    // 10. 列出所有预设
    server.addTool(
        name = "list_presets",
        description = "列出所有可用的循环预设（包括官方和自定义）",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        try {
            val repository = aya.strokenet.data.repository.PresetRepository(context)
            val allPresets = repository.getAllPresets()
            
            val presetList = allPresets.map { preset ->
                "- ${preset.name} (ID: ${preset.id})\n  描述: ${preset.description}\n  动作数: ${preset.commands.size}\n  类型: ${if (preset.isCustom) "自定义" else "官方"}"
            }.joinToString("\n\n")
            
            CallToolResult(
                content = listOf(
                    TextContent(text = "✓ 共 ${allPresets.size} 个预设:\n\n$presetList")
                )
            )
        } catch (e: Exception) {
            Log.e("McpServer", "ListPresets tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 11. 运行预设
    server.addTool(
        name = "run_preset",
        description = "运行指定的循环预设（会循环播放直到手动停止）",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("preset_id") {
                    put("type", "string")
                    put("description", "预设ID（从 list_presets 获取）")
                }
            },
            required = listOf("preset_id")
        )
    ) { request ->
        try {
            val args = request.arguments
            val presetId = args?.get("preset_id")?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("缺少 preset_id")
            
            val repository = aya.strokenet.data.repository.PresetRepository(context)
            val preset = repository.getAllPresets().find { it.id == presetId }
                ?: throw IllegalArgumentException("预设不存在: $presetId")
            
            // 启动循环预设服务
            val presetJson = kotlinx.serialization.json.Json.encodeToString(
                aya.strokenet.data.model.LoopPreset.serializer(),
                preset
            )
            
            val intent = Intent(context, aya.strokenet.LoopPresetService::class.java).apply {
                putExtra("action", aya.strokenet.LoopPresetService.ACTION_START_LOOP)
                putExtra(aya.strokenet.LoopPresetService.EXTRA_PRESET_JSON, presetJson)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            CallToolResult(
                content = listOf(
                    TextContent(text = "✓ 正在运行预设: ${preset.name}\n提示: 使用 stop_all 或 stop_preset 停止")
                )
            )
        } catch (e: Exception) {
            Log.e("McpServer", "RunPreset tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 12. 停止预设
    server.addTool(
        name = "stop_preset",
        description = "停止当前运行的循环预设",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        try {
            context.stopService(Intent(context, aya.strokenet.LoopPresetService::class.java))
            
            CallToolResult(
                content = listOf(TextContent(text = "✓ 已停止循环预设"))
            )
        } catch (e: Exception) {
            Log.e("McpServer", "StopPreset tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 13. 导出自定义预设
    server.addTool(
        name = "export_custom_presets",
        description = "导出所有自定义预设为JSON格式",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        try {
            val repository = aya.strokenet.data.repository.PresetRepository(context)
            val json = repository.exportCustomPresetsJson()
            
            CallToolResult(
                content = listOf(TextContent(text = "✓ 自定义预设JSON:\n\n$json"))
            )
        } catch (e: Exception) {
            Log.e("McpServer", "ExportCustomPresets tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 14. 导入自定义预设
    server.addTool(
        name = "import_custom_presets",
        description = "从JSON导入自定义预设",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("json") {
                    put("type", "string")
                    put("description", "JSON格式的预设数据")
                }
                putJsonObject("replace") {
                    put("type", "boolean")
                    put("description", "是否替换现有预设（默认false，合并模式）")
                    put("default", false)
                }
            },
            required = listOf("json")
        )
    ) { request ->
        try {
            val args = request.arguments
            val json = args?.get("json")?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("缺少 json")
            val replace = args["replace"]?.jsonPrimitive?.boolean ?: false
            
            val repository = aya.strokenet.data.repository.PresetRepository(context)
            val count = repository.importCustomPresetsJson(json, replace)
            
            val mode = if (replace) "替换" else "合并"
            CallToolResult(
                content = listOf(TextContent(text = "✓ 已导入 $count 个预设（${mode}模式）"))
            )
        } catch (e: Exception) {
            Log.e("McpServer", "ImportCustomPresets tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 15. 获取官方预设列表
    server.addTool(
        name = "get_official_presets",
        description = "获取所有官方预设的详细信息",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        try {
            val repository = aya.strokenet.data.repository.PresetRepository(context)
            val presets = repository.loadOfficialPresets()
            
            val details = presets.map { preset ->
                """
                【${preset.name}】
                ID: ${preset.id}
                描述: ${preset.description}
                动作数: ${preset.commands.size}
                总时长: ${preset.commands.sumOf { it.time } / 1000}秒/轮
                """.trimIndent()
            }.joinToString("\n\n")
            
            CallToolResult(
                content = listOf(TextContent(text = "✓ 官方预设 (${presets.size}个):\n\n$details"))
            )
        } catch (e: Exception) {
            Log.e("McpServer", "GetOfficialPresets tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 16. 获取自定义预设列表
    server.addTool(
        name = "get_custom_presets",
        description = "获取所有自定义预设的详细信息",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        try {
            val repository = aya.strokenet.data.repository.PresetRepository(context)
            val presets = repository.loadCustomPresets()
            
            if (presets.isEmpty()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "暂无自定义预设"))
                )
            }
            
            val details = presets.map { preset ->
                """
                【${preset.name}】
                ID: ${preset.id}
                描述: ${preset.description}
                动作数: ${preset.commands.size}
                总时长: ${preset.commands.sumOf { it.time } / 1000}秒/轮
                """.trimIndent()
            }.joinToString("\n\n")
            
            CallToolResult(
                content = listOf(TextContent(text = "✓ 自定义预设 (${presets.size}个):\n\n$details"))
            )
        } catch (e: Exception) {
            Log.e("McpServer", "GetCustomPresets tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 17. 创建自定义预设
    server.addTool(
        name = "create_custom_preset",
        description = "创建一个新的自定义循环预设",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("name") {
                    put("type", "string")
                    put("description", "预设名称")
                }
                putJsonObject("description") {
                    put("type", "string")
                    put("description", "预设描述")
                }
                putJsonObject("actions") {
                    put("type", "array")
                    put("description", "动作列表，每个动作包含：depth, extend_speed, retract_speed, strength (1-100), duration (毫秒)")
                    putJsonObject("items") {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("depth") {
                                put("type", "number")
                                put("minimum", 1)
                                put("maximum", 100)
                            }
                            putJsonObject("extend_speed") {
                                put("type", "number")
                                put("minimum", 1)
                                put("maximum", 100)
                            }
                            putJsonObject("retract_speed") {
                                put("type", "number")
                                put("minimum", 1)
                                put("maximum", 100)
                            }
                            putJsonObject("strength") {
                                put("type", "number")
                                put("minimum", 1)
                                put("maximum", 100)
                            }
                            putJsonObject("duration") {
                                put("type", "number")
                                put("description", "持续时间（毫秒）")
                                put("minimum", 500)
                                put("maximum", 10000)
                            }
                        }
                        putJsonArray("required") {
                            add("depth")
                            add("extend_speed")
                            add("retract_speed")
                            add("strength")
                            add("duration")
                        }
                    }
                }
            },
            required = listOf("name", "description", "actions")
        )
    ) { request ->
        try {
            val args = request.arguments
            val name = args?.get("name")?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("缺少 name")
            val description = args["description"]?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("缺少 description")
            val actionsArray = args["actions"]?.jsonArray
                ?: throw IllegalArgumentException("缺少 actions")
            
            // 构建命令列表
            val commands = mutableListOf<aya.strokenet.data.model.PresetCommand>()
            
            actionsArray.forEach { actionElement ->
                val action = actionElement.jsonObject
                val depth = action["depth"]?.jsonPrimitive?.int ?: 50
                val extend = action["extend_speed"]?.jsonPrimitive?.int ?: 50
                val retract = action["retract_speed"]?.jsonPrimitive?.int ?: 50
                val strength = action["strength"]?.jsonPrimitive?.int ?: 50
                val duration = action["duration"]?.jsonPrimitive?.int ?: 2000
                
                // 构建命令字符串
                val depthHex = depth.toString(16).padStart(2, '0').uppercase()
                val extendHex = extend.toString(16).padStart(2, '0').uppercase()
                val retractHex = retract.toString(16).padStart(2, '0').uppercase()
                val strengthHex = strength.toString(16).padStart(2, '0').uppercase()
                
                val command = "710003**-8800-####-0000-$depthHex$extendHex$retractHex${strengthHex}0000"
                commands.add(aya.strokenet.data.model.PresetCommand(command, duration))
            }
            
            if (commands.isEmpty()) {
                throw IllegalArgumentException("至少需要一个动作")
            }
            
            // 创建预设
            val preset = aya.strokenet.data.model.LoopPreset(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                description = description,
                commands = commands,
                isCustom = true
            )
            
            val repository = aya.strokenet.data.repository.PresetRepository(context)
            repository.addCustomPreset(preset)
            
            CallToolResult(
                content = listOf(
                    TextContent(
                        text = "✓ 预设已创建: $name\nID: ${preset.id}\n动作数: ${commands.size}"
                    )
                )
            )
        } catch (e: Exception) {
            Log.e("McpServer", "CreateCustomPreset tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 18. 删除自定义预设
    server.addTool(
        name = "delete_custom_preset",
        description = "删除指定的自定义预设（官方预设无法删除）",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("preset_id") {
                    put("type", "string")
                    put("description", "要删除的预设ID")
                }
            },
            required = listOf("preset_id")
        )
    ) { request ->
        try {
            val args = request.arguments
            val presetId = args?.get("preset_id")?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("缺少 preset_id")
            
            val repository = aya.strokenet.data.repository.PresetRepository(context)
            
            // 检查是否为自定义预设
            val customPresets = repository.loadCustomPresets()
            if (customPresets.none { it.id == presetId }) {
                throw IllegalArgumentException("预设不存在或不是自定义预设")
            }
            
            repository.deleteCustomPreset(presetId)
            
            CallToolResult(
                content = listOf(TextContent(text = "✓ 预设已删除: $presetId"))
            )
        } catch (e: Exception) {
            Log.e("McpServer", "DeleteCustomPreset tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 19. 获取预设详细信息
    server.addTool(
        name = "get_preset_detail",
        description = "获取指定预设的详细动作参数",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("preset_id") {
                    put("type", "string")
                    put("description", "预设ID")
                }
            },
            required = listOf("preset_id")
        )
    ) { request ->
        try {
            val args = request.arguments
            val presetId = args?.get("preset_id")?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("缺少 preset_id")
            
            val repository = aya.strokenet.data.repository.PresetRepository(context)
            val preset = repository.getAllPresets().find { it.id == presetId }
                ?: throw IllegalArgumentException("预设不存在: $presetId")
            
            val actionDetails = preset.commands.mapIndexed { index, cmd ->
                val params = parseCommandParams(cmd.command)
                """
                动作 ${index + 1}:
                  深度: ${params["depth"]}
                  伸速度: ${params["extend"]}
                  缩速度: ${params["retract"]}
                  强度: ${params["strength"]}
                  持续时间: ${cmd.time}ms (${cmd.time / 1000.0}秒)
                """.trimIndent()
            }.joinToString("\n\n")
            
            val info = """
            【${preset.name}】
            ID: ${preset.id}
            描述: ${preset.description}
            类型: ${if (preset.isCustom) "自定义" else "官方"}
            动作数: ${preset.commands.size}
            总时长: ${preset.commands.sumOf { it.time } / 1000}秒/轮
            
            动作列表:
            $actionDetails
            """.trimIndent()
            
            CallToolResult(
                content = listOf(TextContent(text = info))
            )
        } catch (e: Exception) {
            Log.e("McpServer", "GetPresetDetail tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 20. 更新自定义预设
    server.addTool(
        name = "update_custom_preset",
        description = "更新现有的自定义预设（只能更新自定义预设，不能更新官方预设）",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("preset_id") {
                    put("type", "string")
                    put("description", "要更新的预设ID")
                }
                putJsonObject("name") {
                    put("type", "string")
                    put("description", "新的预设名称（可选）")
                }
                putJsonObject("description") {
                    put("type", "string")
                    put("description", "新的预设描述（可选）")
                }
                putJsonObject("actions") {
                    put("type", "array")
                    put("description", "新的动作列表（可选），格式同 create_custom_preset")
                    putJsonObject("items") {
                        put("type", "object")
                        putJsonObject("properties") {
                            putJsonObject("depth") {
                                put("type", "number")
                                put("minimum", 1)
                                put("maximum", 100)
                            }
                            putJsonObject("extend_speed") {
                                put("type", "number")
                                put("minimum", 1)
                                put("maximum", 100)
                            }
                            putJsonObject("retract_speed") {
                                put("type", "number")
                                put("minimum", 1)
                                put("maximum", 100)
                            }
                            putJsonObject("strength") {
                                put("type", "number")
                                put("minimum", 1)
                                put("maximum", 100)
                            }
                            putJsonObject("duration") {
                                put("type", "number")
                                put("description", "持续时间（毫秒）")
                                put("minimum", 500)
                                put("maximum", 10000)
                            }
                        }
                        putJsonArray("required") {
                            add("depth")
                            add("extend_speed")
                            add("retract_speed")
                            add("strength")
                            add("duration")
                        }
                    }
                }
            },
            required = listOf("preset_id")
        )
    ) { request ->
        try {
            val args = request.arguments
            val presetId = args?.get("preset_id")?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("缺少 preset_id")
            
            val repository = aya.strokenet.data.repository.PresetRepository(context)
            
            // 查找现有预设
            val existingPreset = repository.loadCustomPresets().find { it.id == presetId }
                ?: throw IllegalArgumentException("预设不存在或不是自定义预设: $presetId")
            
            // 更新名称（如果提供）
            val newName = args["name"]?.jsonPrimitive?.content ?: existingPreset.name
            
            // 更新描述（如果提供）
            val newDescription = args["description"]?.jsonPrimitive?.content ?: existingPreset.description
            
            // 更新动作（如果提供）
            val newCommands = if (args["actions"] != null) {
                val actionsArray = args["actions"]!!.jsonArray
                val commands = mutableListOf<aya.strokenet.data.model.PresetCommand>()
                
                actionsArray.forEach { actionElement ->
                    val action = actionElement.jsonObject
                    val depth = action["depth"]?.jsonPrimitive?.int ?: 50
                    val extend = action["extend_speed"]?.jsonPrimitive?.int ?: 50
                    val retract = action["retract_speed"]?.jsonPrimitive?.int ?: 50
                    val strength = action["strength"]?.jsonPrimitive?.int ?: 50
                    val duration = action["duration"]?.jsonPrimitive?.int ?: 2000
                    
                    // 构建命令字符串
                    val depthHex = depth.toString(16).padStart(2, '0').uppercase()
                    val extendHex = extend.toString(16).padStart(2, '0').uppercase()
                    val retractHex = retract.toString(16).padStart(2, '0').uppercase()
                    val strengthHex = strength.toString(16).padStart(2, '0').uppercase()
                    
                    val command = "710003**-8800-####-0000-$depthHex$extendHex$retractHex${strengthHex}0000"
                    commands.add(aya.strokenet.data.model.PresetCommand(command, duration))
                }
                
                commands
            } else {
                existingPreset.commands
            }
            
            // 创建更新后的预设
            val updatedPreset = aya.strokenet.data.model.LoopPreset(
                id = presetId,
                name = newName,
                description = newDescription,
                commands = newCommands,
                isCustom = true
            )
            
            repository.updateCustomPreset(updatedPreset)
            
            CallToolResult(
                content = listOf(
                    TextContent(
                        text = "✓ 预设已更新: $newName\nID: $presetId\n动作数: ${newCommands.size}"
                    )
                )
            )
        } catch (e: Exception) {
            Log.e("McpServer", "UpdateCustomPreset tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
}

/**
 * 解析命令参数
 */
private fun parseCommandParams(command: String): Map<String, Int> {
    val params = mutableMapOf<String, Int>()
    
    try {
        val parts = command.split("-")
        if (parts.size >= 5) {
            val paramHex = parts[4]
            
            if (paramHex.length >= 8) {
                params["depth"] = paramHex.substring(0, 2).toIntOrNull(16) ?: 0
                params["extend"] = paramHex.substring(2, 4).toIntOrNull(16) ?: 0
                params["retract"] = paramHex.substring(4, 6).toIntOrNull(16) ?: 0
                params["strength"] = paramHex.substring(6, 8).toIntOrNull(16) ?: 0
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return params
}
