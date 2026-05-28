package aya.strokenet.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aya.strokenet.BleAdvertiser
import aya.strokenet.data.model.Preset
import aya.strokenet.ui.theme.*
import aya.strokenet.ui.components.GlassPanel

@Composable
fun PresetsScreen(
    bleAdvertiser: BleAdvertiser?,
    onCheckBluetooth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val presets = remember { Preset.getBuiltInPresets() }
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var selectedPreset by remember { mutableStateOf<Preset?>(null) }

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
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = selectedPreset!!.description,
                        fontSize = 14.sp,
                        color = iOSTextSecondary
                    )
                    
                    Divider(color = Color.Black.copy(alpha = 0.1f))
                    
                    Text(
                        text = "参数详情",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    
                    ParamRow("推拉深度", "${selectedPreset!!.params.depth}", "0-72")
                    ParamRow("伸出速度", "${selectedPreset!!.params.extendSpeed}", "0-15")
                    ParamRow("缩回速度", "${selectedPreset!!.params.retractSpeed}", "0-15")
                    ParamRow("震动强度", "${selectedPreset!!.params.strength}", "0-100")
                    ParamRow("加热温度", "${selectedPreset!!.params.temp}°C", "0-60")
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
                        if (bleAdvertiser?.isBluetoothAvailable() == false) {
                            onCheckBluetooth()
                            return@Button
                        }
                        
                        // 发送启动指令
                        selectedPreset?.let { preset ->
                            bleAdvertiser?.advertise(
                                action = "start",
                                depth = preset.params.depth,
                                extendSpeed = preset.params.extendSpeed,
                                retractSpeed = preset.params.retractSpeed,
                                onSuccess = {
                                    Toast.makeText(
                                        context,
                                        "已启动「${preset.name}」模式",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                onFailure = { errorCode ->
                                    val message = when (errorCode) {
                                        -3 -> "权限不足，请在设置中授予蓝牙权限"
                                        -4 -> "蓝牙未启用"
                                        else -> "启动失败: $errorCode"
                                    }
                                    Toast.makeText(
                                        context,
                                        message,
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            )
                            
                            // 设置强度
                            bleAdvertiser?.advertise(
                                action = "strength",
                                strength = preset.params.strength
                            )
                            
                            // 设置温度
                            bleAdvertiser?.advertise(
                                action = "temp",
                                temp = preset.params.temp
                            )
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
                ) {
                    Text("启动")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消", color = iOSTextSecondary)
                }
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "选择一个预设模式快速开始设备，系统将自动应用配置的推拉深度、速度及温度。",
            fontSize = 14.sp,
            color = iOSTextSecondary,
            lineHeight = 20.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GlassPanel(modifier = Modifier.padding(0.dp)) {
            presets.forEachIndexed { index, preset ->
                AppleStylePresetRow(
                    preset = preset,
                    onClick = {
                        selectedPreset = preset
                        showDialog = true
                    }
                )

                // 除了最后一个，都画分割线
                if (index < presets.size - 1) {
                    Divider(
                        color = Color.Black.copy(alpha = 0.05f),
                        modifier = Modifier.padding(start = 48.dp, top = 12.dp, bottom = 12.dp)
                    )
                }
            }
        }
        
        // 添加停止按钮
        Button(
            onClick = {
                // 检查权限
                if (bleAdvertiser?.hasBluetoothPermissions() == false) {
                    Toast.makeText(
                        context,
                        "请在设置中授予蓝牙权限",
                        Toast.LENGTH_LONG
                    ).show()
                    return@Button
                }
                
                bleAdvertiser?.advertise(
                    action = "stop",
                    onSuccess = {
                        Toast.makeText(
                            context,
                            "已停止设备",
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    onFailure = { errorCode ->
                        val message = when (errorCode) {
                            -3 -> "权限不足，请在设置中授予蓝牙权限"
                            -4 -> "蓝牙未启用"
                            else -> "停止失败: $errorCode"
                        }
                        Toast.makeText(
                            context,
                            message,
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = iOSRed)
        ) {
            Text(
                "停止设备",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun AppleStylePresetRow(preset: Preset, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iOSBlue.copy(alpha = 0.1f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            // 根据预设名称选择图标
            val icon = when {
                preset.name.contains("温柔") -> Icons.Default.Favorite
                preset.name.contains("标准") -> Icons.Default.Star
                preset.name.contains("强劲") -> Icons.Default.FlashOn
                preset.name.contains("持久") -> Icons.Default.Schedule
                else -> Icons.Default.FiberManualRecord
            }
            Icon(
                icon,
                contentDescription = null,
                tint = iOSBlue,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = preset.name,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
                color = iOSTextPrimary
            )
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
