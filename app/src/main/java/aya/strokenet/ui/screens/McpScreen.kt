package aya.strokenet.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import aya.strokenet.ui.components.GlassPanel
import aya.strokenet.ui.theme.*
import aya.strokenet.ui.viewmodel.McpViewModel
import aya.strokenet.ui.viewmodel.ServerStatus

/**
 * MCP Server 管理页面（iOS 风格）
 */
@Composable
fun McpScreen(
    viewModel: McpViewModel = viewModel()
) {
    val context = LocalContext.current
    
    // 注册广播接收器
    DisposableEffect(Unit) {
        viewModel.registerReceiver(context)
        onDispose {
            viewModel.unregisterReceiver(context)
        }
    }
    
    // 每次进入页面时查询最新状态
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(300)
        viewModel.queryCurrentStatus(context)
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 说明卡片
        InfoCard()
        
        // 服务器状态卡片
        StatusCard(
            viewModel = viewModel,
            status = viewModel.serverStatus,
            localUrl = viewModel.localUrl,
            errorMessage = viewModel.errorMessage
        )
        
        // 启动/停止按钮
        ControlCard(
            status = viewModel.serverStatus,
            onStart = { viewModel.startServer(context) },
            onStop = { viewModel.stopServer(context) }
        )
        
        // 工具列表
        ToolsInfoCard()
    }
}

/**
 * 说明卡片
 */
@Composable
private fun InfoCard() {
    GlassPanel {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = iOSBlue,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "MCP Server",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = iOSTextPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Model Context Protocol（模型上下文协议）服务，让 AI 助手能够通过局域网远程控制你的设备。",
            fontSize = 14.sp,
            color = iOSTextSecondary,
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "✨ 快速开始：",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = iOSTextPrimary
        )
        
        Spacer(modifier = Modifier.height(6.dp))
        
        Text(
            text = """
                1️⃣ 启动服务 - 点击下方「启动服务」按钮
                2️⃣ 复制地址 - 复制显示的服务地址
                3️⃣ 配置 AI - 在 AI 工具中添加该地址
                4️⃣ 开始使用 - 重启 AI 后即可语音控制
            """.trimIndent(),
            fontSize = 13.sp,
            color = iOSTextSecondary,
            lineHeight = 19.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 配置示例
        Surface(
            color = iOSTextSecondary.copy(alpha = 0.05f),
            shape = MaterialTheme.shapes.small
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "💡 Claude Desktop 配置示例：",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = iOSTextPrimary
                )
                
                Text(
                    text = """
                        {
                          "mcpServers": {
                            "strokenet": {
                              "command": "none",
                              "url": "http://你的IP:8080/mcp"
                            }
                          }
                        }
                    """.trimIndent(),
                    fontSize = 11.sp,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    color = iOSTextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // 使用提示
        Surface(
            color = iOSRed.copy(alpha = 0.08f),
            shape = MaterialTheme.shapes.small
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = iOSRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "重要提示",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )
                }
                
                Text(
                    text = "• 手机和 AI 工具设备需在同一 WiFi 网络下",
                    fontSize = 12.sp,
                    color = iOSTextSecondary,
                    lineHeight = 18.sp
                )
                
                Text(
                    text = "• 建议多任务列表中给本应用加锁，保持后台运行，或者也可以使用分屏模式",
                    fontSize = 12.sp,
                    color = iOSTextSecondary,
                    lineHeight = 18.sp
                )
                
                Text(
                    text = "• 若 AI 工具获取工具列表时间很长，请重新唤起本 APP，一般即可正常加载",
                    fontSize = 12.sp,
                    color = iOSTextSecondary,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

/**
 * 状态卡片
 */
@Composable
private fun StatusCard(
    viewModel: McpViewModel,
    status: ServerStatus,
    localUrl: String?,
    errorMessage: String?
) {
    val context = LocalContext.current
    
    GlassPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = when (status) {
                        ServerStatus.Stopped -> Icons.Default.PowerOff
                        ServerStatus.Starting, ServerStatus.Stopping -> Icons.Default.Sync
                        ServerStatus.RunningLocal -> Icons.Default.CheckCircle
                        ServerStatus.Error -> Icons.Default.Error
                    },
                    contentDescription = null,
                    tint = when (status) {
                        ServerStatus.RunningLocal -> iOSGreen
                        ServerStatus.Error -> iOSRed
                        else -> iOSTextSecondary
                    },
                    modifier = Modifier.size(20.dp)
                )
                
                Text(
                    text = when (status) {
                        ServerStatus.Stopped -> "已停止"
                        ServerStatus.Starting -> "正在启动..."
                        ServerStatus.Stopping -> "正在停止..."
                        ServerStatus.RunningLocal -> "运行中"
                        ServerStatus.Error -> "错误"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
            }
            
            // 刷新按钮
            IconButton(
                onClick = {
                    viewModel.queryCurrentStatus(context)
                    Toast.makeText(context, "正在刷新状态...", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "刷新状态",
                    tint = iOSBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        
        // 服务地址
        localUrl?.let {
            Spacer(modifier = Modifier.height(12.dp))
            UrlDisplay(
                label = "服务地址（可直接复制到 AI 工具配置）",
                url = it,
                onCopy = { 
                    copyToClipboard(context, it)
                    Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                }
            )
            
            // localhost 地址提示
            Spacer(modifier = Modifier.height(8.dp))
            val localhostUrl = it.replace(Regex("http://[0-9.]+:"), "http://localhost:")
            UrlDisplay(
                label = "本机调用地址（同一设备上的应用可使用）",
                url = localhostUrl,
                onCopy = { 
                    copyToClipboard(context, localhostUrl)
                    Toast.makeText(context, "已复制本机地址到剪贴板", Toast.LENGTH_SHORT).show()
                }
            )
            // SSE 连接提示
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = iOSBlue.copy(alpha = 0.08f),
                shape = MaterialTheme.shapes.small
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = iOSBlue,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = "若 AI 工具获取工具列表时间很长，请重新唤起本 APP，一般即可正常加载",
                        fontSize = 12.sp,
                        color = iOSTextSecondary,
                        lineHeight = 16.sp
                    )
                }
            }

        }
        
        // 错误信息
        errorMessage?.let {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "错误: $it",
                fontSize = 14.sp,
                color = iOSRed
            )
        }
    }
}

/**
 * URL 显示组件
 */
@Composable
private fun UrlDisplay(
    label: String,
    url: String,
    onCopy: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = iOSTextSecondary
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = url,
                fontSize = 13.sp,
                color = iOSBlue,
                modifier = Modifier.weight(1f)
            )
            
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "复制",
                    tint = iOSBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * 控制卡片（启动/停止）
 */
@Composable
private fun ControlCard(
    status: ServerStatus,
    onStart: () -> Unit,
    onStop: () -> Unit
) {
    val isRunning = status == ServerStatus.RunningLocal
    val isLoading = status == ServerStatus.Starting || status == ServerStatus.Stopping
    
    GlassPanel {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Wifi,
                contentDescription = null,
                tint = iOSBlue,
                modifier = Modifier.size(20.dp)
            )
            
            Column {
                Text(
                    text = "本地服务",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
                Text(
                    text = "需要在同一 WiFi 网络下访问",
                    fontSize = 13.sp,
                    color = iOSTextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Button(
            onClick = { if (isRunning) onStop() else onStart() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isRunning) iOSRed else iOSBlue
            )
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRunning) "停止服务" else if (isLoading) "请稍候..." else "启动服务",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/**
 * 工具列表说明（完整能力清单）
 */
@Composable
private fun ToolsInfoCard() {
    GlassPanel {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Build,
                contentDescription = null,
                tint = iOSBlue,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "MCP 工具能力清单",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = iOSTextPrimary
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "AI 可通过以下 20 个工具远程控制设备：",
            fontSize = 13.sp,
            color = iOSTextSecondary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 分组显示工具
        ToolCategory(
            title = "📖 必读指南",
            tools = listOf(
                ToolItem("get_device_guide", "设备控制专家指南", "包含参数说明和推荐模式")
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ToolCategory(
            title = "🎮 基础控制（推荐优先使用 send_all）",
            tools = listOf(
                ToolItem("send_all", "一键设置所有参数", "推拉+震动，最高效", isHighlighted = true),
                ToolItem("thrust", "控制推拉运动", "深度、伸展速度、收缩速度"),
                ToolItem("strength", "设置震动强度", "1-100 可调")
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ToolCategory(
            title = "🔥 加热功能",
            tools = listOf(
                ToolItem("start_heating", "启动定时加热", "需指定温度和时长"),
                ToolItem("stop_heating", "停止加热", "手动停止加热")
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ToolCategory(
            title = "⏹️ 停止控制",
            tools = listOf(
                ToolItem("stop_all", "全部停止", "推拉+震动+预设", isHighlighted = true),
                ToolItem("stop_thrust", "停止推拉", ""),
                ToolItem("stop_strength", "停止震动", ""),
                ToolItem("stop_preset", "停止循环预设", "")
            )
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        ToolCategory(
            title = "🎯 预设管理（10个工具）",
            tools = listOf(
                ToolItem("list_presets", "列出所有预设", "必须先调用此工具获取ID"),
                ToolItem("run_preset", "运行循环预设", "无限循环直到停止"),
                ToolItem("get_official_presets", "获取官方预设详情", ""),
                ToolItem("get_custom_presets", "获取自定义预设详情", ""),
                ToolItem("get_preset_detail", "查看预设动作参数", ""),
                ToolItem("create_custom_preset", "创建自定义预设", ""),
                ToolItem("update_custom_preset", "更新自定义预设", ""),
                ToolItem("delete_custom_preset", "删除自定义预设", ""),
                ToolItem("export_custom_presets", "导出预设为JSON", ""),
                ToolItem("import_custom_presets", "从JSON导入预设", "")
            )
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // 提示信息
        Surface(
            color = iOSBlue.copy(alpha = 0.08f),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = iOSBlue,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "AI 首次连接时会自动调用 get_device_guide 获取使用说明",
                    fontSize = 12.sp,
                    color = iOSTextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

/**
 * 工具分类展示
 */
@Composable
private fun ToolCategory(
    title: String,
    tools: List<ToolItem>
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = iOSTextPrimary
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        tools.forEach { tool ->
            ToolRow(tool)
        }
    }
}

/**
 * 工具行
 */
@Composable
private fun ToolRow(tool: ToolItem) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Icon(
            imageVector = if (tool.isHighlighted) Icons.Default.Star else Icons.Default.Circle,
            contentDescription = null,
            tint = if (tool.isHighlighted) iOSBlue else iOSTextSecondary.copy(alpha = 0.5f),
            modifier = Modifier.size(12.dp).padding(top = 2.dp)
        )
        
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tool.name,
                fontSize = 13.sp,
                fontWeight = if (tool.isHighlighted) FontWeight.SemiBold else FontWeight.Normal,
                color = if (tool.isHighlighted) iOSBlue else iOSTextPrimary
            )
            
            if (tool.description.isNotEmpty()) {
                Text(
                    text = tool.description,
                    fontSize = 12.sp,
                    color = iOSTextSecondary
                )
            }
            
            if (tool.note.isNotEmpty()) {
                Text(
                    text = tool.note,
                    fontSize = 11.sp,
                    color = iOSTextSecondary.copy(alpha = 0.7f),
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }
        }
    }
}

/**
 * 工具数据类
 */
private data class ToolItem(
    val name: String,
    val description: String,
    val note: String = "",
    val isHighlighted: Boolean = false
)

/**
 * 复制到剪贴板
 */
private fun copyToClipboard(context: android.content.Context, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("MCP URL", text)
    clipboard.setPrimaryClip(clip)
}