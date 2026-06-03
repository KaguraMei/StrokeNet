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
            text = "Model Context Protocol（模型上下文协议）服务，允许 AI 助手（Claude Desktop、Kiro）通过网络远程控制设备。",
            fontSize = 14.sp,
            color = iOSTextSecondary,
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = "使用方法：",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = iOSTextPrimary
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "1. 点击下方「启动服务」按钮\n2. 复制显示的服务地址\n3. 在 AI 工具（如 Claude Desktop）的 MCP 配置中添加该地址\n4. 重启 AI 工具后即可使用",
            fontSize = 13.sp,
            color = iOSTextSecondary,
            lineHeight = 19.sp
        )
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
 * 工具列表说明
 */
@Composable
private fun ToolsInfoCard() {
    GlassPanel {
        Text(
            text = "可用工具",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = iOSTextPrimary
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        val tools = listOf(
            "thrust - 控制推拉",
            "strength - 设置震动强度",
            "start_heating - 启动加热",
            "stop_heating - 停止加热",
            "send_all - 批量发送参数",
            "stop_all - 全部停止"
        )
        
        tools.forEach { tool ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Build,
                    contentDescription = null,
                    tint = iOSTextSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = tool,
                    fontSize = 13.sp,
                    color = iOSTextSecondary
                )
            }
        }
    }
}

/**
 * 复制到剪贴板
 */
private fun copyToClipboard(context: android.content.Context, text: String) {
    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    val clip = android.content.ClipData.newPlainText("MCP URL", text)
    clipboard.setPrimaryClip(clip)
}