package aya.strokenet.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
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
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBg)
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "取消",
                    tint = iOSBlue
                )
            }
            Text(
                text = if (isEditMode) "编辑预设" else "新建预设",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(
                onClick = {
                    // 验证输入
                    if (name.isBlank()) {
                        Toast.makeText(context, "请输入预设名称", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    if (description.isBlank()) {
                        Toast.makeText(context, "请输入预设描述", Toast.LENGTH_SHORT).show()
                        return@TextButton
                    }
                    if (commands.isEmpty()) {
                        Toast.makeText(context, "请至少添加一个动作", Toast.LENGTH_SHORT).show()
                        return@TextButton
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
                }
            ) {
                Text("保存", color = iOSBlue, fontWeight = FontWeight.SemiBold)
            }
        }
        
        // 内容区域
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
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
    }
}

@Composable
fun CommandItemRow(
    index: Int,
    command: PresetCommand,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.03f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "动作 $index",
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = iOSTextPrimary
            )
            Text(
                text = "持续 ${command.time / 1000.0}秒",
                fontSize = 13.sp,
                color = iOSTextSecondary
            )
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Default.Edit,
                    contentDescription = "编辑",
                    tint = iOSBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "删除",
                    tint = iOSRed,
                    modifier = Modifier.size(20.dp)
                )
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
    var depth by remember { mutableIntStateOf(command?.let { parseCommandParams(it.command)["depth"] } ?: 50) }
    var extendSpeed by remember { mutableIntStateOf(command?.let { parseCommandParams(it.command)["extend"] } ?: 50) }
    var retractSpeed by remember { mutableIntStateOf(command?.let { parseCommandParams(it.command)["retract"] } ?: 50) }
    var strength by remember { mutableIntStateOf(command?.let { parseCommandParams(it.command)["strength"] } ?: 50) }
    var duration by remember { mutableIntStateOf(command?.time ?: 2000) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (command == null) "添加动作" else "编辑动作",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 深度 (1-100)
                SliderWithLabel(
                    label = "深度",
                    value = depth,
                    onValueChange = { depth = it },
                    valueRange = 1f..100f,
                    unit = ""
                )
                
                // 伸速度 (1-100)
                SliderWithLabel(
                    label = "伸速度",
                    value = extendSpeed,
                    onValueChange = { extendSpeed = it },
                    valueRange = 1f..100f,
                    unit = ""
                )
                
                // 缩速度 (1-100)
                SliderWithLabel(
                    label = "缩速度",
                    value = retractSpeed,
                    onValueChange = { retractSpeed = it },
                    valueRange = 1f..100f,
                    unit = ""
                )
                
                // 强度 (1-100)
                SliderWithLabel(
                    label = "强度",
                    value = strength,
                    onValueChange = { strength = it },
                    valueRange = 1f..100f,
                    unit = ""
                )
                
                // 持续时间
                SliderWithLabel(
                    label = "持续时间",
                    value = duration / 1000,
                    onValueChange = { duration = it * 1000 },
                    valueRange = 0.5f..10f,
                    unit = "秒"
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    // 构建命令字符串
                    val commandStr = buildCommandString(depth, extendSpeed, retractSpeed, strength)
                    onSave(PresetCommand(commandStr, duration))
                },
                colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消", color = iOSTextSecondary)
            }
        }
    )
}

@Composable
fun SliderWithLabel(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    unit: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label,
                fontSize = 14.sp,
                color = iOSTextPrimary
            )
            Text(
                text = "$value$unit",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = iOSBlue
            )
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            valueRange = valueRange,
            colors = SliderDefaults.colors(
                thumbColor = iOSBlue,
                activeTrackColor = iOSBlue
            )
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
