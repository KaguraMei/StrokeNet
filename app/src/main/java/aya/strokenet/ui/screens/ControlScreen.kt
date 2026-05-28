package aya.strokenet.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
import aya.strokenet.BleAdvertiser
import aya.strokenet.data.model.ControlParams
import aya.strokenet.ui.theme.*
import aya.strokenet.ui.components.GlassPanel
import aya.strokenet.ui.components.StatusIndicator

@Composable
fun ControlScreen(
    bleAdvertiser: BleAdvertiser?,
    onCheckBluetooth: () -> Unit,
    modifier: Modifier = Modifier
) {
    var depth by remember { mutableStateOf(36f) }
    var extendSpeed by remember { mutableStateOf(8f) }
    var retractSpeed by remember { mutableStateOf(8f) }
    var strength by remember { mutableStateOf(50f) }
    var temp by remember { mutableStateOf(30f) }
    var isRunning by remember { mutableStateOf(false) }
    var isBluetoothEnabled by remember { mutableStateOf(true) }
    
    val context = LocalContext.current

    // 初始检查蓝牙状态
    LaunchedEffect(Unit) {
        isBluetoothEnabled = bleAdvertiser?.isBluetoothAvailable() ?: false
    }
    
    // 监听蓝牙状态变化
    DisposableEffect(context) {
        val bluetoothReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BluetoothAdapter.ACTION_STATE_CHANGED -> {
                        val state = intent.getIntExtra(
                            BluetoothAdapter.EXTRA_STATE,
                            BluetoothAdapter.ERROR
                        )
                        isBluetoothEnabled = when (state) {
                            BluetoothAdapter.STATE_ON -> true
                            BluetoothAdapter.STATE_OFF -> false
                            else -> isBluetoothEnabled
                        }
                    }
                }
            }
        }
        
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(bluetoothReceiver, filter)
        
        onDispose {
            context.unregisterReceiver(bluetoothReceiver)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 状态指示器
        StatusIndicator(
            isRunning = isRunning,
            isBluetoothEnabled = isBluetoothEnabled
        )

        // 推拉控制面板
        GlassPanel {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iOSBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.SwapVert,
                        contentDescription = null,
                        tint = iOSBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "推拉参数",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
            }

            AppleStyleSlider(
                label = "推拉深度",
                value = depth,
                range = ControlParams.DEPTH_MIN.toFloat()..ControlParams.DEPTH_MAX.toFloat(),
                onValueChange = { depth = it }
            )

            Divider(
                color = Color.Black.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            AppleStyleSlider(
                label = "伸出速度",
                value = extendSpeed,
                range = ControlParams.SPEED_MIN.toFloat()..ControlParams.SPEED_MAX.toFloat(),
                onValueChange = { extendSpeed = it }
            )

            Divider(
                color = Color.Black.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            AppleStyleSlider(
                label = "缩回速度",
                value = retractSpeed,
                range = ControlParams.SPEED_MIN.toFloat()..ControlParams.SPEED_MAX.toFloat(),
                onValueChange = { retractSpeed = it }
            )
        }

        // 其他参数面板
        GlassPanel {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(iOSBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Tune,
                        contentDescription = null,
                        tint = iOSBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "增强设置",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
            }

            AppleStyleSlider(
                label = "震动强度",
                value = strength,
                range = ControlParams.STRENGTH_MIN.toFloat()..ControlParams.STRENGTH_MAX.toFloat(),
                onValueChange = {
                    strength = it
                    bleAdvertiser?.advertise(action = "strength", strength = it.toInt())
                }
            )

            Divider(
                color = Color.Black.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            AppleStyleSlider(
                label = "加热温度",
                value = temp,
                range = ControlParams.TEMP_MIN.toFloat()..ControlParams.TEMP_MAX.toFloat(),
                unit = "°C",
                onValueChange = {
                    temp = it
                    bleAdvertiser?.advertise(action = "temp", temp = it.toInt())
                }
            )
        }

        // iOS风格大按钮区域
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    // 检查权限
                    if (bleAdvertiser?.hasBluetoothPermissions() == false) {
                        android.widget.Toast.makeText(
                            context,
                            "请在设置中授予蓝牙权限",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        return@Button
                    }
                    
                    // 检查蓝牙状态，如果未启用则弹出系统对话框
                    if (!isBluetoothEnabled) {
                        onCheckBluetooth()
                        return@Button
                    }
                    
                    if (isRunning) {
                        bleAdvertiser?.advertise(
                            action = "thrust",
                            depth = depth.toInt(),
                            extendSpeed = extendSpeed.toInt(),
                            retractSpeed = retractSpeed.toInt(),
                            onSuccess = {
                                android.widget.Toast.makeText(
                                    context,
                                    "参数已更新",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = { errorCode ->
                                val message = when (errorCode) {
                                    -3 -> "权限不足，请在设置中授予蓝牙权限"
                                    -4 -> "蓝牙未启用"
                                    else -> "发送失败: $errorCode"
                                }
                                android.widget.Toast.makeText(
                                    context,
                                    message,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    } else {
                        bleAdvertiser?.advertise(
                            action = "start",
                            depth = depth.toInt(),
                            extendSpeed = extendSpeed.toInt(),
                            retractSpeed = retractSpeed.toInt(),
                            onSuccess = {
                                isRunning = true
                                android.widget.Toast.makeText(
                                    context,
                                    "设备已启动",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            onFailure = { errorCode ->
                                val message = when (errorCode) {
                                    -3 -> "权限不足，请在设置中授予蓝牙权限"
                                    -4 -> "蓝牙未启用"
                                    else -> "启动失败: $errorCode"
                                }
                                android.widget.Toast.makeText(
                                    context,
                                    message,
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
            ) {
                Text(
                    if (isRunning) "调节更新" else "启动设备",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = {
                    // 检查蓝牙状态
                    if (!isBluetoothEnabled) {
                        onCheckBluetooth()
                        return@Button
                    }
                    
                    bleAdvertiser?.advertise(
                        action = "stop",
                        onSuccess = {
                            isRunning = false
                            android.widget.Toast.makeText(
                                context,
                                "设备已停止",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        },
                        onFailure = { errorCode ->
                            val message = when (errorCode) {
                                -3 -> "权限不足，请在设置中授予蓝牙权限"
                                -4 -> "蓝牙未启用"
                                else -> "停止失败: $errorCode"
                            }
                            android.widget.Toast.makeText(
                                context,
                                message,
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    )
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iOSRed)
            ) {
                Text(
                    "紧急停止",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // 底部留白
    }
}

/**
 * 苹果美学滑块：细条、纯净外观
 */
@Composable
fun AppleStyleSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String = "",
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                fontSize = 15.sp,
                color = iOSTextPrimary
            )
            Text(
                text = "${value.toInt()}$unit",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = iOSBlue
            )
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = iOSBlue,
                inactiveTrackColor = Color.Black.copy(alpha = 0.08f)
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
