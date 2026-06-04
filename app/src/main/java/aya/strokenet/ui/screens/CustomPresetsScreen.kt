package aya.strokenet.ui.screens

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import aya.strokenet.data.model.LoopPreset
import aya.strokenet.ui.components.GlassPanel
import aya.strokenet.ui.theme.*
import aya.strokenet.ui.viewmodel.PresetViewModel

@Composable
fun CustomPresetsScreen(
    onNavigateBack: () -> Unit,
    onEditPreset: (LoopPreset?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresetViewModel = viewModel()
) {
    val context = LocalContext.current
    var showDetailDialog by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf<LoopPreset?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<LoopPreset?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var exportedJson by remember { mutableStateOf("") }
    var importJson by remember { mutableStateOf("") }
    
    // 确保每次进入页面时刷新数据
    LaunchedEffect(Unit) {
        viewModel.loadPresets()
        
        // 检查 LoopPresetService 是否还在运行
        val isServiceRunning = isServiceRunning(context, aya.strokenet.LoopPresetService::class.java)
        if (!isServiceRunning && viewModel.playingPresetId != null) {
            // 服务已停止但 UI 还显示播放中，清空状态
            android.util.Log.d("CustomPresetsScreen", "Service not running, clearing playingPresetId")
            viewModel.playingPresetId = null
        }
    }
    
    // 监听循环播放停止广播
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    aya.strokenet.LoopPresetService.BROADCAST_LOOP_STOPPED -> {
                        android.util.Log.d("CustomPresetsScreen", "Received BROADCAST_LOOP_STOPPED")
                        viewModel.playingPresetId = null
                    }
                    aya.strokenet.LoopPresetService.BROADCAST_LOOP_STARTED -> {
                        // MCP 启动预设时的广播
                        val presetId = intent.getStringExtra("preset_id")
                        if (presetId != null) {
                            android.util.Log.d("CustomPresetsScreen", "Received BROADCAST_LOOP_STARTED: $presetId")
                            viewModel.playingPresetId = presetId
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(aya.strokenet.LoopPresetService.BROADCAST_LOOP_STOPPED)
            addAction(aya.strokenet.LoopPresetService.BROADCAST_LOOP_STARTED)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        
        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                android.util.Log.w("CustomPresetsScreen", "Receiver already unregistered")
            }
        }
    }
    
    // 详情对话框
    if (showDetailDialog && selectedPreset != null) {
        PresetDetailDialog(
            preset = selectedPreset!!,
            isPlaying = viewModel.playingPresetId == selectedPreset!!.id,
            onDismiss = { showDetailDialog = false },
            onRun = {
                showDetailDialog = false
                
                // 启动循环播放服务
                val presetJson = kotlinx.serialization.json.Json.encodeToString(selectedPreset!!)
                val intent = android.content.Intent(context, aya.strokenet.LoopPresetService::class.java).apply {
                    putExtra("action", aya.strokenet.LoopPresetService.ACTION_START_LOOP)
                    putExtra(aya.strokenet.LoopPresetService.EXTRA_PRESET_JSON, presetJson)
                }
                
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
                
                viewModel.playingPresetId = selectedPreset!!.id
                Toast.makeText(context, "正在循环播放「${selectedPreset!!.name}」", Toast.LENGTH_SHORT).show()
            },
            onEdit = {
                showDetailDialog = false
                onEditPreset(selectedPreset)
            },
            onDelete = {
                showDetailDialog = false
                presetToDelete = selectedPreset
                showDeleteDialog = true
            }
        )
    }
    
    // 删除确认对话框
    if (showDeleteDialog && presetToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("删除预设", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("确定要删除「${presetToDelete!!.name}」吗？此操作无法撤销。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomPreset(presetToDelete!!.id)
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                        showDeleteDialog = false
                        presetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSRed)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消", color = iOSTextSecondary)
                }
            }
        )
    }
    
    // 导出对话框
    if (showExportDialog) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = {
                Text("导出预设", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("JSON格式，可分享或备份：")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = exportedJson,
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        textStyle = TextStyle(fontSize = 12.sp)
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // 复制到剪贴板
                        val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) 
                            as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("预设JSON", exportedJson)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
                        showExportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
                ) {
                    Text("复制")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExportDialog = false }) {
                    Text("关闭", color = iOSTextSecondary)
                }
            }
        )
    }
    
    // 导入对话框
    if (showImportDialog) {
        AlertDialog(
            onDismissRequest = { showImportDialog = false },
            title = {
                Text("导入预设", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text("粘贴JSON格式的预设数据：")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = importJson,
                        onValueChange = { importJson = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        textStyle = TextStyle(fontSize = 12.sp),
                        placeholder = { Text("粘贴JSON...") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (importJson.isNotBlank()) {
                            val count = viewModel.importPresets(importJson, replace = false)
                            if (count > 0) {
                                Toast.makeText(context, "导入 $count 个预设", Toast.LENGTH_SHORT).show()
                                importJson = ""
                                showImportDialog = false
                            } else {
                                Toast.makeText(context, "导入失败，请检查JSON格式", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
                ) {
                    Text("导入")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportDialog = false }) {
                    Text("取消", color = iOSTextSecondary)
                }
            }
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBg)
    ) {
        // 内容区域
        if (viewModel.customPresets.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = iOSTextSecondary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "还没有自定义预设",
                        fontSize = 16.sp,
                        color = iOSTextSecondary
                    )
                    Text(
                        text = "点击右下角 + 号创建第一个预设",
                        fontSize = 14.sp,
                        color = iOSTextSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        } else {
            // 预设列表
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .padding(bottom = 80.dp), // 为悬浮按钮留空间
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "共 ${viewModel.customPresets.size} 个自定义预设",
                    fontSize = 14.sp,
                    color = iOSTextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                viewModel.customPresets.forEach { preset ->
                    PresetCardItem(
                        preset = preset,
                        isPlaying = viewModel.playingPresetId == preset.id,
                        onClick = {
                            selectedPreset = preset
                            showDetailDialog = true
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // 左下角悬浮按钮组（避免被全局停止按钮挡住）
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // 导入按钮
            FloatingActionButton(
                onClick = { showImportDialog = true },
                containerColor = Color.White,
                contentColor = iOSBlue
            ) {
                Icon(
                    Icons.Default.FileDownload,
                    contentDescription = "导入"
                )
            }
            
            // 导出按钮（只在有预设时显示）
            if (viewModel.customPresets.isNotEmpty()) {
                FloatingActionButton(
                    onClick = { 
                        exportedJson = viewModel.exportPresets()
                        showExportDialog = true
                    },
                    containerColor = Color.White,
                    contentColor = iOSBlue
                ) {
                    Icon(
                        Icons.Default.FileUpload,
                        contentDescription = "导出"
                    )
                }
            }
            
            // 新建按钮
            FloatingActionButton(
                onClick = { onEditPreset(null) },
                containerColor = iOSBlue,
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "新建"
                )
            }
        }
    }
}

@Composable
fun PresetCardItem(
    preset: LoopPreset,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = if (isPlaying) 4.dp else 2.dp,
        tonalElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 播放状态指示器
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (isPlaying) iOSBlue else iOSBlue.copy(alpha = 0.1f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isPlaying) Icons.Default.PlayArrow else Icons.Default.Favorite,
                    contentDescription = null,
                    tint = if (isPlaying) Color.White else iOSBlue,
                    modifier = Modifier.size(22.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // 预设信息
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = preset.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )
                    if (isPlaying) {
                        Text(
                            text = "播放中",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .background(iOSBlue, RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preset.description,
                    fontSize = 14.sp,
                    color = iOSTextSecondary,
                    maxLines = 1
                )
            }
            
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = null,
                tint = Color(0xFFC7C7CC),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SmallInfoChip(text: String) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = iOSTextSecondary,
        modifier = Modifier
            .background(
                Color.Black.copy(alpha = 0.04f),
                RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}

@Composable
fun PresetDetailDialog(
    preset: LoopPreset,
    isPlaying: Boolean,
    onDismiss: () -> Unit,
    onRun: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(
                    text = preset.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = preset.description,
                    fontSize = 14.sp,
                    color = iOSTextSecondary,
                    fontWeight = FontWeight.Normal
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 统计信息
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    InfoChip(
                        icon = Icons.Default.List,
                        text = "${preset.commands.size} 动作"
                    )
                    InfoChip(
                        icon = Icons.Default.Timer,
                        text = "${preset.commands.sumOf { it.time } / 1000}秒"
                    )
                }
                
                HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
                
                Text(
                    text = "动作列表",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                
                // 显示每个命令
                preset.commands.forEachIndexed { index, cmd ->
                    CustomCommandDetailRow(index = index + 1, command = cmd)
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit) {
                    Text("编辑", color = iOSBlue)
                }
                TextButton(onClick = onDelete) {
                    Text("删除", color = iOSRed)
                }
                Button(
                    onClick = onRun,
                    colors = ButtonDefaults.buttonColors(containerColor = iOSBlue),
                    enabled = !isPlaying
                ) {
                    Text(if (isPlaying) "播放中" else "开始循环")
                }
            }
        },
       
    )
}

// 从PresetsScreen复用
@Composable
fun CustomCommandDetailRow(
    index: Int,
    command: aya.strokenet.data.model.PresetCommand
) {
    val params = remember(command) { parseCustomCommandParams(command.command) }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "动作 $index",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = iOSTextPrimary
            )
            Text(
                text = "${command.time / 1000.0}秒",
                fontSize = 13.sp,
                color = iOSBlue,
                fontWeight = FontWeight.Medium
            )
        }
        
        // 参数网格
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            params["depth"]?.let { depth ->
                CustomParamChip(label = "深度", value = "$depth", modifier = Modifier.weight(1f))
            }
            params["extend"]?.let { extend ->
                CustomParamChip(label = "伸", value = "$extend", modifier = Modifier.weight(1f))
            }
            params["retract"]?.let { retract ->
                CustomParamChip(label = "缩", value = "$retract", modifier = Modifier.weight(1f))
            }
            params["strength"]?.let { strength ->
                CustomParamChip(label = "强度", value = "$strength", modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CustomParamChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(Color.White, RoundedCornerShape(8.dp))
            .padding(vertical = 6.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = iOSTextSecondary
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = iOSTextPrimary
        )
    }
}

private fun parseCustomCommandParams(command: String): Map<String, Int> {
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

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .background(
                iOSBlue.copy(alpha = 0.08f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = iOSBlue
        )
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = iOSBlue
        )
    }
}

/**
 * 检查指定服务是否正在运行
 */
private fun isServiceRunning(context: Context, serviceClass: Class<*>): Boolean {
    val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
    @Suppress("DEPRECATION")
    for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
        if (serviceClass.name == service.service.className) {
            return true
        }
    }
    return false
}
