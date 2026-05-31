package aya.strokenet.ui.screens

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import aya.strokenet.ble.DaxiuBleAdvertiser
import aya.strokenet.BleService
import aya.strokenet.LoopPresetService
import aya.strokenet.data.model.LoopPreset
import aya.strokenet.data.model.PresetCommand
import aya.strokenet.ui.theme.*
import aya.strokenet.ui.components.GlassPanel
import aya.strokenet.ui.viewmodel.PresetViewModel
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun PresetsScreen(
    bleAdvertiser: DaxiuBleAdvertiser?,
    onCheckBluetooth: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: PresetViewModel = viewModel()
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf<LoopPreset?>(null) }
    
    // 监听循环播放停止广播
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == LoopPresetService.BROADCAST_LOOP_STOPPED) {
                    viewModel.playingPresetId = null
                }
            }
        }
        
        val filter = IntentFilter(LoopPresetService.BROADCAST_LOOP_STOPPED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // 预设详情对话框
    if (showDialog && selectedPreset != null) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = {
                Text(
                    text = selectedPreset!!.name,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = selectedPreset!!.description,
                        fontSize = 14.sp,
                        color = iOSTextSecondary
                    )
                    
                    HorizontalDivider(color = Color.Black.copy(alpha = 0.1f))
                    
                    Text(
                        text = "循环节奏",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    Text(
                        text = "共 ${selectedPreset!!.commands.size} 个动作，总时长 ${selectedPreset!!.commands.sumOf { it.time } / 1000}秒/轮",
                        fontSize = 13.sp,
                        color = iOSTextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // 显示每个命令的详细参数
                    selectedPreset!!.commands.forEachIndexed { index, cmd ->
                        CommandDetailRow(
                            index = index + 1,
                            command = cmd
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        
                        // 检查权限
                        if (bleAdvertiser?.hasBluetoothPermissions() == false) {
                            Toast.makeText(
                                context,
                                "请在设置中授予蓝牙权限",
                                Toast.LENGTH_LONG
                            ).show()
                            return@Button
                        }
                        
                        // 检查蓝牙状态
                        if (bleAdvertiser?.isBluetoothEnabled() == false) {
                            onCheckBluetooth()
                            return@Button
                        }
                        
                        // 启动循环播放服务
                        selectedPreset?.let { preset ->
                            val presetJson = Json.encodeToString(preset)
                            val intent = Intent(context, LoopPresetService::class.java).apply {
                                putExtra("action", LoopPresetService.ACTION_START_LOOP)
                                putExtra(LoopPresetService.EXTRA_PRESET_JSON, presetJson)
                            }
                            
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                            
                            viewModel.playingPresetId = preset.id
                            
                            Toast.makeText(
                                context,
                                "正在循环播放「${preset.name}」",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
                ) {
                    Text("开始循环")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消", color = iOSTextSecondary)
                }
            }
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = if (viewModel.playingPresetId != null) 80.dp else 0.dp), // 为悬浮按钮留空间
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // 加载状态
        if (viewModel.isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Column
        }
        
        Text(
            text = "选择一个预设模式开始循环播放，系统将自动按照预设的节奏循环发送命令。",
            fontSize = 14.sp,
            color = iOSTextSecondary,
            lineHeight = 20.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // 官方预设
        if (viewModel.officialPresets.isNotEmpty()) {
            Text(
                text = "官方预设",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = iOSTextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )
            
            GlassPanel(modifier = Modifier.padding(0.dp)) {
                viewModel.officialPresets.forEachIndexed { index, preset ->
                    PresetRow(
                        preset = preset,
                        isPlaying = viewModel.playingPresetId == preset.id,
                        onClick = {
                            selectedPreset = preset
                            showDialog = true
                        }
                    )

                    if (index < viewModel.officialPresets.size - 1) {
                        HorizontalDivider(
                            color = Color.Black.copy(alpha = 0.05f),
                            modifier = Modifier.padding(start = 48.dp, top = 12.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }
        
        // 自定义预设
        if (viewModel.customPresets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "自定义预设",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSTextPrimary
                )
                TextButton(
                    onClick = { /* TODO: 导航到自定义预设管理页面 */ }
                ) {
                    Text("管理", color = iOSBlue)
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = iOSBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            
            GlassPanel(modifier = Modifier.padding(0.dp)) {
                viewModel.customPresets.forEachIndexed { index, preset ->
                    PresetRow(
                        preset = preset,
                        isPlaying = viewModel.playingPresetId == preset.id,
                        onClick = {
                            selectedPreset = preset
                            showDialog = true
                        }
                    )

                    if (index < viewModel.customPresets.size - 1) {
                        HorizontalDivider(
                            color = Color.Black.copy(alpha = 0.05f),
                            modifier = Modifier.padding(start = 48.dp, top = 12.dp, bottom = 12.dp)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
        
        // 悬浮急停按钮（可拖动）
        if (viewModel.playingPresetId != null) {
            var offsetX by remember { mutableFloatStateOf(0f) }
            var offsetY by remember { mutableFloatStateOf(0f) }
            
            FloatingActionButton(
                onClick = {
                    // 停止循环播放服务
                    context.stopService(Intent(context, LoopPresetService::class.java))
                    viewModel.playingPresetId = null
                    
                    // 发送全部停止命令
                    val serviceIntent = Intent(context, BleService::class.java).apply {
                        putExtra("action", BleService.ACTION_STOP_ALL)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    
                    Toast.makeText(
                        context,
                        "正在停止设备...",
                        Toast.LENGTH_SHORT
                    ).show()
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .offset { androidx.compose.ui.unit.IntOffset(offsetX.toInt(), offsetY.toInt()) }
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        }
                    },
                containerColor = iOSRed,
                contentColor = Color.White
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Stop,
                        contentDescription = "紧急停止",
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        "紧急停止",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun PresetRow(
    preset: LoopPreset,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (isPlaying) iOSBlue else iOSBlue.copy(alpha = 0.1f),
                    RoundedCornerShape(10.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (isPlaying) Icons.Default.PlayArrow else Icons.Default.FiberManualRecord,
                contentDescription = null,
                tint = if (isPlaying) Color.White else iOSBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = preset.name,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Medium,
                    color = iOSTextPrimary
                )
                if (preset.isCustom) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "自定义",
                        fontSize = 11.sp,
                        color = Color.White,
                        modifier = Modifier
                            .background(iOSBlue, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Text(
                text = preset.description,
                fontSize = 13.sp,
                color = iOSTextSecondary
            )
        }
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = Color(0xFFC7C7CC)
        )
    }
}

/**
 * 解析命令字符串中的参数
 * 格式: 710003**-8800-####-0000-DDEERRSSTTTT
 * DD=深度, EE=伸速度, RR=缩速度, SS=强度, TT=温度
 */
private fun parseCommandParams(command: String): Map<String, Int> {
    val params = mutableMapOf<String, Int>()
    
    try {
        // 提取最后的参数部分 (去掉UUID前缀)
        val parts = command.split("-")
        if (parts.size >= 5) {
            val paramHex = parts[4] // 例如: "1908090000"
            
            if (paramHex.length >= 8) {
                // 解析各个参数 (每2位十六进制)
                val depth = paramHex.substring(0, 2).toIntOrNull(16) ?: 0
                val extendSpeed = paramHex.substring(2, 4).toIntOrNull(16) ?: 0
                val retractSpeed = paramHex.substring(4, 6).toIntOrNull(16) ?: 0
                val strength = paramHex.substring(6, 8).toIntOrNull(16) ?: 0
                
                params["depth"] = depth
                params["extend"] = extendSpeed
                params["retract"] = retractSpeed
                params["strength"] = strength
                
                // 如果有温度参数
                if (paramHex.length >= 10) {
                    val temp = paramHex.substring(8, 10).toIntOrNull(16) ?: 0
                    if (temp > 0) {
                        params["temp"] = temp
                    }
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    
    return params
}

@Composable
fun CommandDetailRow(
    index: Int,
    command: PresetCommand
) {
    val params = remember(command) { parseCommandParams(command.command) }
    
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
                ParamChip(
                    label = "深度",
                    value = "$depth",
                    modifier = Modifier.weight(1f)
                )
            }
            params["extend"]?.let { extend ->
                ParamChip(
                    label = "伸",
                    value = "$extend",
                    modifier = Modifier.weight(1f)
                )
            }
            params["retract"]?.let { retract ->
                ParamChip(
                    label = "缩",
                    value = "$retract",
                    modifier = Modifier.weight(1f)
                )
            }
            params["strength"]?.let { strength ->
                ParamChip(
                    label = "强度",
                    value = "$strength",
                    modifier = Modifier.weight(1f)
                )
            }
        }
        
        params["temp"]?.let { temp ->
            ParamChip(
                label = "温度",
                value = "$temp°C",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ParamChip(
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

@Composable
private fun ParamRow(label: String, value: String, range: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                fontSize = 14.sp,
                color = iOSTextPrimary
            )
            Text(
                text = "范围: $range",
                fontSize = 11.sp,
                color = iOSTextSecondary
            )
        }
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = iOSBlue
        )
    }
}
