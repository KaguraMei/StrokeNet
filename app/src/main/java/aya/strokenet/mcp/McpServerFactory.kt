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
    
    // 0. 设备使用指南（优先级最高，AI 会首先看到）
    server.addTool(
        name = "get_device_guide",
        description = "【必读】获取设备控制的专家级指南，包含参数搭配建议和使用技巧",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        CallToolResult(
            content = listOf(
                TextContent(text = """
                    ===== StrokeNet 设备控制指南 =====
                    
                    【核心原则】
                    1. 优先使用 send_all 工具一次性设置所有运动参数（推拉+震动）
                    2. 加热必须使用 start_heating 工具，并指定时长（1-10分钟）以确保安全
                    3. 所有参数范围都是 1-100，数值越大效果越强
                    4. 调用 run_preset 前必须先调用 list_presets 获取正确的 ID
                    
                    【参数体感说明】
                    • depth（推拉深度）：
                      - 1-20: 浅尝辄止，轻微抽动
                      - 30-60: 中等幅度，标准体验
                      - 70-100: 深度撞击，完全伸展
                      
                    • extend（伸展速度）：
                      - 1-30: 缓慢推入，温柔体贴
                      - 40-70: 中速节奏，标准舒适
                      - 80-100: 快速冲刺，强烈刺激
                      
                    • retract（收缩速度）：
                      - 1-30: 缓慢抽出，回味绵长
                      - 40-70: 中速回归，节奏感强
                      - 80-100: 快速退出，急促密集
                      
                    • strength（震动强度）：
                      - 1-30: 微弱酥麻，若有若无
                      - 40-70: 适中震感，舒适体验
                      - 80-100: 强烈震颤，注意噪音
                    
                    【推荐模式组合】
                    1. 温柔模式：depth=30, extend=40, retract=40, strength=30
                    2. 标准体验：depth=50, extend=50, retract=50, strength=50
                    3. 激烈撞击：depth=80, extend=90, retract=30, strength=70
                    4. 快速抽插：depth=40, extend=100, retract=100, strength=60
                    5. 深度慢推：depth=90, extend=20, retract=40, strength=50
                    
                    【调整建议】
                    • 用户说"快一点" → 在当前 extend/retract 基础上 +20
                    • 用户说"慢一点" → 在当前 extend/retract 基础上 -20
                    • 用户说"深一点" → depth +20
                    • 用户说"浅一点" → depth -20
                    • 用户说"强一点" → strength +20
                    
                    【加热使用（重要）】
                    1. 加热必须使用 start_heating 工具，必须指定时长（1-10分钟）
                    2. 推荐温度：35-40°C（接近体温最舒适）
                    3. 推荐时长：5-10分钟
                    4. 到时后会自动停止加热，确保安全
                    5. ⚠️ send_all 工具不包含温度参数，避免无定时器的安全隐患
                    
                    【注意事项】
                    1. 停止所有运动使用 stop_all
                    2. 预设会循环播放直到手动停止
                    3. 每次执行后记得在回复中总结当前设备状态
                    
                    ===================================
                """.trimIndent())
            )
        )
    }
    
    // 1. 推拉控制（优化描述）
    server.addTool(
        name = "thrust",
        description = "精确控制设备的物理推拉运动（深度和伸缩速度）。注意：仅控制推拉，不影响震动。如需同时设置推拉和震动，请使用 send_all 工具。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("depth") {
                    put("type", "number")
                    put("description", "推拉的幅度深度。1为极短程快速抽动，100为完全伸展的长程撞击。建议起始值：50")
                    put("minimum", 1)
                    put("maximum", 100)
                    putJsonArray("examples") { add(50); add(30); add(80) }
                }
                putJsonObject("extend") {
                    put("type", "number")
                    put("description", "伸出（推入）动作的速度。100最快（急促冲刺），1最慢（缓慢推入）。建议与retract配合：若extend>retract会有强烈冲入感。建议起始值：50")
                    put("minimum", 1)
                    put("maximum", 100)
                    putJsonArray("examples") { add(50); add(90); add(30) }
                }
                putJsonObject("retract") {
                    put("type", "number")
                    put("description", "收缩（抽出）动作的速度。100最快（快速退出），1最慢（缓慢回归）。与extend配合形成节奏。建议起始值：50")
                    put("minimum", 1)
                    put("maximum", 100)
                    putJsonArray("examples") { add(50); add(30); add(90) }
                }
            },
            required = listOf("depth", "extend", "retract")
        )
    ) { request ->
        try {
            val args = request.arguments
            val depth = args?.get("depth").toSafeInt()
                ?: throw IllegalArgumentException("缺少 depth 参数（推拉深度，范围 1-100）")
            val extend = args?.get("extend").toSafeInt()
                ?: throw IllegalArgumentException("缺少 extend 参数（伸展速度，范围 1-100）")
            val retract = args?.get("retract").toSafeInt()
                ?: throw IllegalArgumentException("缺少 retract 参数（收缩速度，范围 1-100）")
            
            // 验证范围
            if (depth !in 1..100 || extend !in 1..100 || retract !in 1..100) {
                throw IllegalArgumentException("参数超出范围 (1-100)。depth=$depth, extend=$extend, retract=$retract")
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
            
            // 详细的状态反馈
            val rhythm = when {
                extend > retract + 20 -> "强冲入感"
                retract > extend + 20 -> "快速抽出"
                else -> "均匀节奏"
            }
            
            CallToolResult(
                content = listOf(
                    TextContent(text = """
                        ✓ 推拉指令已发送并执行
                        
                        【当前设备状态】
                        • 推拉深度：$depth (${when {
                            depth < 30 -> "浅尝辄止"
                            depth < 70 -> "中等幅度"
                            else -> "深度撞击"
                        }})
                        • 伸展速度：$extend (${when {
                            extend < 40 -> "缓慢推入"
                            extend < 80 -> "中速节奏"
                            else -> "快速冲刺"
                        }})
                        • 收缩速度：$retract (${when {
                            retract < 40 -> "缓慢抽出"
                            retract < 80 -> "中速回归"
                            else -> "快速退出"
                        }})
                        • 节奏特征：$rhythm
                        
                        提示：震动需单独设置，建议使用 send_all 同时设置推拉和震动。
                    """.trimIndent())
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
    
    // 2. 震动强度（单独设置，建议使用 send_all）
    server.addTool(
        name = "strength",
        description = "单独设置震动马达强度。注意：仅设置震动，不影响推拉。建议使用 send_all 工具同时设置推拉和震动。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("value") {
                    put("type", "number")
                    put("description", "震动马达的转速强度。1为微弱酥麻感，100为强烈震颤（可能噪音较大）。建议起始值50。")
                    put("minimum", 1)
                    put("maximum", 100)
                    putJsonArray("examples") { add(50); add(30); add(70) }
                }
            },
            required = listOf("value")
        )
    ) { request ->
        try {
            val args = request.arguments
            val value = args?.get("value").toSafeInt()
                ?: throw IllegalArgumentException("缺少 value 参数（震动强度，范围 1-100）")
            
            if (value !in 1..100) {
                throw IllegalArgumentException("参数超出范围 (1-100)。value=$value")
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
            
            val level = when {
                value < 30 -> "微弱震感"
                value < 70 -> "适中震动"
                else -> "强烈震颤"
            }
            
            CallToolResult(
                content = listOf(
                    TextContent(text = """
                        ✓ 震动强度已设置
                        
                        【当前震动状态】
                        • 强度值：$value ($level)
                        ${if (value > 80) "• 提示：当前强度较高，可能产生噪音" else ""}
                        
                        推拉参数保持不变。如需同时调整推拉和震动，请使用 send_all 工具。
                    """.trimIndent())
                )
            )
        } catch (e: Exception) {
            Log.e("McpServer", "Strength tool failed", e)
            CallToolResult(
                content = listOf(TextContent(text = "✗ 执行失败: ${e.message}")),
                isError = true
            )
        }
    }
    
    // 3. 加热定时器（必须指定时长，会自动关闭）
    server.addTool(
        name = "start_heating",
        description = "启动定时加热功能。【重要】温度加热必须指定时长（1-10分钟），到时会自动关闭。推荐温度35-40°C，时长5-10分钟。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("temperature") {
                    put("type", "number")
                    put("description", "加热目标温度（°C）。范围1-60，推荐35-40°C，接近体温最舒适。")
                    put("minimum", 1)
                    put("maximum", 60)
                    putJsonArray("examples") { add(37); add(40); add(35) }
                }
                putJsonObject("duration") {
                    put("type", "number")
                    put("description", "加热持续时长（分钟）。范围1-10分钟，推荐5-10分钟。到时自动关闭加热。")
                    put("minimum", 1)
                    put("maximum", 10)
                    putJsonArray("examples") { add(5); add(10); add(8) }
                }
            },
            required = listOf("temperature", "duration")
        )
    ) { request ->
        try {
            val args = request.arguments
            val temperature = args?.get("temperature").toSafeInt()
                ?: throw IllegalArgumentException("缺少 temperature 参数（温度，范围 1-60°C）")
            val duration = args?.get("duration").toSafeInt()
                ?: throw IllegalArgumentException("缺少 duration 参数（时长，范围 1-10分钟）")
            
            if (temperature !in 1..60) {
                throw IllegalArgumentException("温度超出范围 (1-60)。temperature=$temperature")
            }
            if (duration !in 1..10) {
                throw IllegalArgumentException("时长超出范围 (1-10)。duration=$duration")
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
            
            val tempLevel = when {
                temperature < 35 -> "微温"
                temperature < 40 -> "温暖舒适（接近体温）"
                temperature < 45 -> "较热"
                else -> "高温（小心烫伤）"
            }
            
            CallToolResult(
                content = listOf(
                    TextContent(text = """
                        ✓ 定时加热已启动
                        
                        【加热设置】
                        • 目标温度：${temperature}°C ($tempLevel)
                        • 持续时长：$duration 分钟
                        • 自动关闭：到时后自动停止加热
                        
                        提示：加热过程中设备会逐步升温至目标温度，$duration 分钟后自动关闭以确保安全。
                    """.trimIndent())
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
    
    // 5. 【推荐】全参数发送（AI 应优先使用此工具）
    server.addTool(
        name = "send_all",
        description = "【推荐：优先使用】一键设置设备的运动参数（推拉+震动）。当你需要同时调整设备状态时，请务必优先使用此工具，而非多次调用单个工具。注意：温度加热请使用 start_heating 工具（需指定时长以确保安全）。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("depth") {
                    put("type", "number")
                    put("description", "推拉深度。1为浅，100为深。建议起始值50。")
                    put("minimum", 1)
                    put("maximum", 100)
                    putJsonArray("examples") { add(50); add(30); add(80) }
                }
                putJsonObject("extend") {
                    put("type", "number")
                    put("description", "伸展（推入）速度。100最快，1最慢。建议起始值50。")
                    put("minimum", 1)
                    put("maximum", 100)
                    putJsonArray("examples") { add(50); add(90); add(30) }
                }
                putJsonObject("retract") {
                    put("type", "number")
                    put("description", "收缩（抽出）速度。100最快，1最慢。建议起始值50。")
                    put("minimum", 1)
                    put("maximum", 100)
                    putJsonArray("examples") { add(50); add(30); add(90) }
                }
                putJsonObject("strength") {
                    put("type", "number")
                    put("description", "震动强度。1为微弱，100为强烈。建议起始值50。")
                    put("minimum", 1)
                    put("maximum", 100)
                    putJsonArray("examples") { add(50); add(70); add(30) }
                }
            },
            required = listOf("depth", "extend", "retract", "strength")
        )
    ) { request ->
        try {
            val args = request.arguments
            val depth = args?.get("depth").toSafeInt()
                ?: throw IllegalArgumentException("缺少 depth 参数（推拉深度，范围 1-100）")
            val extend = args?.get("extend").toSafeInt()
                ?: throw IllegalArgumentException("缺少 extend 参数（伸展速度，范围 1-100）")
            val retract = args?.get("retract").toSafeInt()
                ?: throw IllegalArgumentException("缺少 retract 参数（收缩速度，范围 1-100）")
            val strength = args?.get("strength").toSafeInt()
                ?: throw IllegalArgumentException("缺少 strength 参数（震动强度，范围 1-100）")
            
            // 验证范围
            if (depth !in 1..100 || extend !in 1..100 || retract !in 1..100 || strength !in 1..100) {
                throw IllegalArgumentException("推拉/震动参数超出范围 (1-100)。depth=$depth, extend=$extend, retract=$retract, strength=$strength")
            }
            
            val intent = Intent(context, BleService::class.java).apply {
                putExtra("action", BleService.ACTION_SEND_ALL)
                putExtra(BleService.EXTRA_DEPTH, depth)
                putExtra(BleService.EXTRA_EXTEND, extend)
                putExtra(BleService.EXTRA_RETRACT, retract)
                putExtra(BleService.EXTRA_STRENGTH, strength)
                // 不再传递温度参数，温度固定为 0
                putExtra(BleService.EXTRA_TEMP, 0)
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            // 详细的状态反馈
            val depthLevel = when {
                depth < 30 -> "浅尝辄止"
                depth < 70 -> "中等幅度"
                else -> "深度撞击"
            }
            
            val speedPattern = when {
                extend > retract + 20 -> "强冲入，慢抽出"
                retract > extend + 20 -> "慢推入，快抽出"
                extend > 70 && retract > 70 -> "高速抽插"
                extend < 40 && retract < 40 -> "缓慢律动"
                else -> "均匀节奏"
            }
            
            val vibLevel = when {
                strength < 30 -> "微弱震感"
                strength < 70 -> "适中震动"
                else -> "强烈震颤"
            }

            
            CallToolResult(
                content = listOf(
                    TextContent(
                        text = """
                            ✓ 全部参数已发送并执行成功
                            
                            【当前设备状态】
                            • 推拉深度：$depth ($depthLevel)
                            • 伸展速度：$extend
                            • 收缩速度：$retract
                            • 速度模式：$speedPattern
                            • 震动强度：$strength ($vibLevel)
                            
                            提示：如需启动加热，请使用 start_heating 工具（必须指定时长以确保安全）。
                            设备正在以当前参数运行，用户可随时调整或停止。
                        """.trimIndent()
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
    
    // 9. 【紧急】全部停止
    server.addTool(
        name = "stop_all",
        description = "【紧急停止】立即停止设备的所有运动（推拉+震动+循环预设）。当用户要求停止、暂停或结束时使用此工具。",
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
            
            // 同时停止循环预设服务
            context.stopService(Intent(context, LoopPresetService::class.java))
            
            CallToolResult(
                content = listOf(
                    TextContent(text = """
                        ✓ 所有运动已停止
                        
                        已执行操作：
                        • 推拉运动：已停止
                        • 震动马达：已停止
                        • 循环预设：已停止（如果正在运行）
                        
                        设备现在处于静止状态，可随时发送新指令。
                    """.trimIndent())
                )
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
    
    // 10. 列出所有预设（查询预设 ID）
    server.addTool(
        name = "list_presets",
        description = "列出所有可用的循环预设（包括官方预设和自定义预设）。【必须】在调用 run_preset 之前，先调用此工具获取预设ID列表。",
        inputSchema = ToolSchema(properties = buildJsonObject {})
    ) { _ ->
        try {
            val repository = aya.strokenet.data.repository.PresetRepository(context)
            val allPresets = repository.getAllPresets()
            
            if (allPresets.isEmpty()) {
                return@addTool CallToolResult(
                    content = listOf(TextContent(text = "暂无可用预设"))
                )
            }
            
            val presetList = allPresets.mapIndexed { index, preset ->
                val duration = preset.commands.sumOf { it.time } / 1000
                """
                ${index + 1}. ${preset.name}
                   • ID: ${preset.id}
                   • 描述: ${preset.description}
                   • 动作数: ${preset.commands.size} 个
                   • 单轮时长: ${duration} 秒
                   • 类型: ${if (preset.isCustom) "自定义" else "官方"}
                """.trimIndent()
            }.joinToString("\n\n")
            
            CallToolResult(
                content = listOf(
                    TextContent(text = """
                        ✓ 共 ${allPresets.size} 个可用预设
                        
                        $presetList
                        
                        使用说明：复制想要运行的预设ID，然后调用 run_preset 工具并传入该ID。
                    """.trimIndent())
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
    
    // 11. 运行预设（必须先调用 list_presets）
    server.addTool(
        name = "run_preset",
        description = "运行指定的循环预设（会循环播放直到手动停止）。【重要】调用此工具前，必须先调用 list_presets 以获取正确的预设ID，禁止猜测ID！预设会按照预定的动作序列循环播放，直到用户要求停止。",
        inputSchema = ToolSchema(
            properties = buildJsonObject {
                putJsonObject("preset_id") {
                    put("type", "string")
                    put("description", "预设的唯一标识符。必须从 list_presets 工具的返回结果中获取，不要猜测或编造ID。")
                }
            },
            required = listOf("preset_id")
        )
    ) { request ->
        try {
            val args = request.arguments
            val presetId = args?.get("preset_id")?.jsonPrimitive?.content
                ?: throw IllegalArgumentException("缺少 preset_id 参数")
            
            val repository = aya.strokenet.data.repository.PresetRepository(context)
            val preset = repository.getAllPresets().find { it.id == presetId }
                ?: throw IllegalArgumentException("预设不存在: $presetId。请先调用 list_presets 获取有效的预设ID。")
            
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
            
            val totalDuration = preset.commands.sumOf { it.time } / 1000
            
            CallToolResult(
                content = listOf(
                    TextContent(text = """
                        ✓ 循环预设已启动
                        
                        【预设信息】
                        • 预设名称：${preset.name}
                        • 预设描述：${preset.description}
                        • 动作数量：${preset.commands.size} 个
                        • 单轮时长：${totalDuration} 秒
                        • 运行模式：循环播放（无限重复）
                        
                        预设将按照预定的动作序列不断循环，直到用户要求停止。
                        可使用 stop_preset 或 stop_all 工具停止。
                    """.trimIndent())
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
 * 安全地从 JsonElement 提取整数
 * 支持字符串、浮点数等格式，提高 AI 调用容错性
 */
private fun JsonElement?.toSafeInt(): Int? {
    return when {
        this == null -> null
        this is JsonPrimitive -> {
            when {
                this.isString -> this.content.toDoubleOrNull()?.toInt()
                else -> this.intOrNull
            }
        }
        else -> null
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
