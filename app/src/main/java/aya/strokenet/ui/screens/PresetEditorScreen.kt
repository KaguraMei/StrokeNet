package aya.strokenet.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import aya.strokenet.data.model.LoopPreset
import aya.strokenet.data.model.PresetCommand
import aya.strokenet.ui.components.GlassPanel
import aya.strokenet.ui.theme.*
import aya.strokenet.ui.viewmodel.PresetViewModel
import java.util.UUID

@Composable
fun PresetEditorScreen(
    preset: LoopPreset?,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresetViewModel = viewModel()
) {
    val context = LocalContext.current
    val isEditMode = preset != null
    
    var name by remember { mutableStateOf(preset?.name ?: "") }
    var description by remember { mutableStateOf(preset?.description ?: "") }
    var commands by remember { 
        mutableStateOf<List<PresetCommand>>(preset?.commands ?: emptyList())
    }
    
    // 当前编辑的命令
    var editingIndex by remember { mutableStateOf(-1) }
    var showCommandEditor by remember { mutableStateOf(false) }
    var showExitConfirmDialog by remember { mutableStateOf(false) }
    
    // 拦截系统返回
    BackHandler {
        // 检查是否有未保存的更改
        val hasChanges = name.isNotBlank() || description.isNotBlank() || commands.isNotEmpty()
        if (hasChanges) {
            showExitConfirmDialog = true
        } else {
            onNavigateBack()
        }
    }
    
    // 退出确认对话框
    if (showExitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showExitConfirmDialog = false },
            title = { Text("放弃更改？", fontWeight = FontWeight.Bold) },
            text = { Text("您的更改尚未保存，确定要退出吗？") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitConfirmDialog = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSRed)
                ) {
                    Text("放弃")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirmDialog = false }) {
                    Text("继续编辑", color = iOSBlue)
                }
            }
        )
    }
    
    // 命令编辑器对话框
    if (showCommandEditor) {
        CommandEditorDialog(
            command = if (editingIndex >= 0 && editingIndex < commands.size) commands[editingIndex] else null,
            onDismiss = { showCommandEditor = false },
            onSave = { newCommand ->
                commands = if (editingIndex >= 0 && editingIndex < commands.size) {
                    commands.toMutableList().apply { set(editingIndex, newCommand) }
                } else {
                    commands + newCommand
                }
                showCommandEditor = false
                editingIndex = -1
            }
        )
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBg)
    ) {
        // 内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .padding(bottom = 80.dp), // 为悬浮按钮留空间
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 基本信息
            GlassPanel {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "基本信息",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("预设名称") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("预设描述") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 3
                    )
                }
            }
            
            // 动作列表
            GlassPanel {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "动作列表",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${commands.size} 个动作",
                            fontSize = 13.sp,
                            color = iOSTextSecondary
                        )
                    }
                    
                    if (commands.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "还没有添加动作",
                                fontSize = 14.sp,
                                color = iOSTextSecondary
                            )
                        }
                    } else {
                        commands.forEachIndexed { index, command ->
                            CommandItemRow(
                                index = index + 1,
                                command = command,
                                onEdit = {
                                    editingIndex = index
                                    showCommandEditor = true
                                },
                                onDelete = {
                                    commands = commands.filterIndexed { i, _ -> i != index }
                                }
                            )
                        }
                    }
                    
                    Button(
                        onClick = {
                            editingIndex = -1
                            showCommandEditor = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = iOSBlue.copy(alpha = 0.1f),
                            contentColor = iOSBlue
                        )
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("添加动作")
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        // 左下角悬浮按钮组
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.Start
        ) {
            // 取消按钮
            FloatingActionButton(
                onClick = onNavigateBack,
                containerColor = Color.White,
                contentColor = iOSTextSecondary
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "取消"
                )
            }
            
            // 保存按钮
            FloatingActionButton(
                onClick = {
                    // 验证输入
                    if (name.isBlank()) {
                        Toast.makeText(context, "请输入预设名称", Toast.LENGTH_SHORT).show()
                        return@FloatingActionButton
                    }
                    if (description.isBlank()) {
                        Toast.makeText(context, "请输入预设描述", Toast.LENGTH_SHORT).show()
                        return@FloatingActionButton
                    }
                    if (commands.isEmpty()) {
                        Toast.makeText(context, "请至少添加一个动作", Toast.LENGTH_SHORT).show()
                        return@FloatingActionButton
                    }
                    
                    // 保存预设
                    val newPreset = LoopPreset(
                        id = preset?.id ?: UUID.randomUUID().toString(),
                        name = name,
                        description = description,
                        commands = commands,
                        isCustom = true
                    )
                    
                    if (isEditMode) {
                        viewModel.updateCustomPreset(newPreset)
                        Toast.makeText(context, "预设已更新", Toast.LENGTH_SHORT).show()
                    } else {
                        viewModel.addCustomPreset(newPreset)
                        Toast.makeText(context, "预设已创建", Toast.LENGTH_SHORT).show()
                    }
                    
                    onNavigateBack()
                },
                containerColor = iOSBlue,
                contentColor = Color.White
            ) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = "保存"
                )
            }
        }
    }
}

@Composable
fun CommandItemRow(
    index: Int,
    command: PresetCommand,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 序号标签
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iOSBlue.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$index",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = iOSBlue
                    )
                }
                
                Column {
                    Text(
                        text = "动作 $index",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )
                    Text(
                        text = "${command.time / 1000.0}秒",
                        fontSize = 13.sp,
                        color = iOSTextSecondary
                    )
                }
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "编辑",
                        tint = iOSBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "删除",
                        tint = iOSRed,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun CommandEditorDialog(
    command: PresetCommand?,
    onDismiss: () -> Unit,
    onSave: (PresetCommand) -> Unit
) {
    // 状态管理（将滑块状态统一为 Float，体验更顺滑）
    var depth by remember { mutableFloatStateOf((command?.let { parseCommandParams(it.command)["depth"] } ?: 50).toFloat()) }
    var extendSpeed by remember { mutableFloatStateOf((command?.let { parseCommandParams(it.command)["extend"] } ?: 50).toFloat()) }
    var retractSpeed by remember { mutableFloatStateOf((command?.let { parseCommandParams(it.command)["retract"] } ?: 50).toFloat()) }
    var strength by remember { mutableFloatStateOf((command?.let { parseCommandParams(it.command)["strength"] } ?: 50).toFloat()) }
    var duration by remember { mutableFloatStateOf((command?.time?.toFloat() ?: 2000f) / 1000f) }
    
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            tonalElevation = 4.dp
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // 头部标题
                Text(
                    text = if (command == null) "添加动作" else "编辑动作",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = iOSTextPrimary,
                    modifier = Modifier.padding(bottom = 20.dp)
                )
                
                // 滚动内容区
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // 分组 1：运动控制
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "运动控制",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = iOSTextSecondary
                        )
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF2F2F7))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ModernSliderItem(
                                label = "深度",
                                icon = Icons.Rounded.Height,
                                value = depth,
                                onValueChange = { depth = it },
                                valueRange = 1f..100f
                            )
                            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                            ModernSliderItem(
                                label = "伸出速度",
                                icon = Icons.Rounded.FastForward,
                                value = extendSpeed,
                                onValueChange = { extendSpeed = it },
                                valueRange = 1f..100f
                            )
                            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                            ModernSliderItem(
                                label = "缩回速度",
                                icon = Icons.Rounded.FastRewind,
                                value = retractSpeed,
                                onValueChange = { retractSpeed = it },
                                valueRange = 1f..100f
                            )
                        }
                    }
                    
                    // 分组 2：进阶设置
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            "进阶设置",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = iOSTextSecondary
                        )
                        Column(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFF2F2F7))
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            ModernSliderItem(
                                label = "强度",
                                icon = Icons.Rounded.Bolt,
                                value = strength,
                                onValueChange = { strength = it },
                                valueRange = 1f..100f
                            )
                            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
                            ModernSliderItem(
                                label = "持续时间",
                                icon = Icons.Rounded.Timer,
                                value = duration,
                                onValueChange = { duration = it },
                                valueRange = 0.5f..10f,
                                unit = "秒",
                                format = "%.1f"
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // 底部操作按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = iOSTextPrimary
                        ),
                        border = null
                    ) {
                        Text("取消", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                    
                    Button(
                        onClick = {
                            val commandStr = buildCommandString(
                                depth.toInt(),
                                extendSpeed.toInt(),
                                retractSpeed.toInt(),
                                strength.toInt()
                            )
                            onSave(PresetCommand(commandStr, (duration * 1000).toInt()))
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
                    ) {
                        Text("保存", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

/**
 * 精致版 Slider 组件
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModernSliderItem(
    label: String,
    icon: ImageVector,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String = "",
    format: String = "%.0f"
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // 顶部：图标 + 标题 + 数值展示
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = String.format(format, value) + if (unit.isNotEmpty()) " $unit" else "",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = iOSBlue
            )
        }
        
        // 提取颜色配置，确保 Track 和 Slider 使用同一套颜色
        val sliderColors = SliderDefaults.colors(
            thumbColor = Color.White,
            activeTrackColor = iOSBlue,
            inactiveTrackColor = Color.Black.copy(alpha = 0.1f)
        )

        // 底部：滑块
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp),
            colors = sliderColors,
            // 【核心修复区域】：重写 Track，干掉 Material 3 默认的竖线和间隙
            track = { sliderState ->
                SliderDefaults.Track(
                    sliderState = sliderState,
                    colors = sliderColors,
                    modifier = Modifier.height(8.dp), 
                    thumbTrackGapSize = 0.dp, // 消除滑块与轨道之间的缝隙
                    drawStopIndicator = null  // 消除轨道两端/中间默认自带的竖线刻度
                )
            }
        )
    }
}

/**
 * 构建命令字符串
 * 格式: 710003**-8800-####-0000-DDEERRSS0000
 */
private fun buildCommandString(depth: Int, extend: Int, retract: Int, strength: Int): String {
    val depthHex = depth.toString(16).padStart(2, '0').uppercase()
    val extendHex = extend.toString(16).padStart(2, '0').uppercase()
    val retractHex = retract.toString(16).padStart(2, '0').uppercase()
    val strengthHex = strength.toString(16).padStart(2, '0').uppercase()
    
    return "710003**-8800-####-0000-$depthHex$extendHex$retractHex${strengthHex}0000"
}

/**
 * 解析命令参数（复用PresetsScreen的函数）
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
