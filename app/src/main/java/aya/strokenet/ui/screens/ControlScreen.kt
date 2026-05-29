package aya.strokenet.ui.screens

import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import aya.strokenet.BleAdvertiser
import aya.strokenet.BleService
import aya.strokenet.data.model.ControlParams
import aya.strokenet.ui.theme.*
import aya.strokenet.ui.components.GlassPanel
import aya.strokenet.ui.components.StatusIndicator
import aya.strokenet.ui.viewmodel.ControlViewModel

@Composable
fun ControlScreen(
    bleAdvertiser: BleAdvertiser?,
    onCheckBluetooth: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ControlViewModel = viewModel()
) {
    // 从 ViewModel 收集状态
    val depth by viewModel.depth.collectAsState()
    val extendSpeed by viewModel.extendSpeed.collectAsState()
    val retractSpeed by viewModel.retractSpeed.collectAsState()
    val strength by viewModel.strength.collectAsState()
    val temp by viewModel.temp.collectAsState()
    val isRunning by viewModel.isRunning.collectAsState()
    
    var isBluetoothEnabled by remember { mutableStateOf(true) }
    
    val context = LocalContext.current
    
    // 用于防抖的 Handler
    val handler = remember { android.os.Handler(android.os.Looper.getMainLooper()) }
    var strengthRunnable by remember { mutableStateOf<Runnable?>(null) }
    var tempRunnable by remember { mutableStateOf<Runnable?>(null) }

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
                onValueChange = { viewModel.updateDepth(it) }
            )

            Divider(
                color = Color.Black.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            AppleStyleSlider(
                label = "伸出速度",
                value = extendSpeed,
                range = ControlParams.SPEED_MIN.toFloat()..ControlParams.SPEED_MAX.toFloat(),
                onValueChange = { viewModel.updateExtendSpeed(it) }
            )

            Divider(
                color = Color.Black.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            AppleStyleSlider(
                label = "缩回速度",
                value = retractSpeed,
                range = ControlParams.SPEED_MIN.toFloat()..ControlParams.SPEED_MAX.toFloat(),
                onValueChange = { viewModel.updateRetractSpeed(it) }
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
                    viewModel.updateStrength(it)
                    
                    // 取消之前的延迟任务
                    strengthRunnable?.let { runnable -> handler.removeCallbacks(runnable) }
                    
                    // 创建新的延迟任务（500ms 后发送）
                    val newRunnable = Runnable {
                        val serviceIntent = Intent(context, BleService::class.java).apply {
                            putExtra("action", "strength")
                            putExtra(BleService.EXTRA_VALUE, it.toInt())
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                    strengthRunnable = newRunnable
                    handler.postDelayed(newRunnable, 500) // 500ms 防抖
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
                    viewModel.updateTemp(it)
                    
                    // 取消之前的延迟任务
                    tempRunnable?.let { runnable -> handler.removeCallbacks(runnable) }
                    
                    // 创建新的延迟任务（500ms 后发送）
                    val newRunnable = Runnable {
                        val serviceIntent = Intent(context, BleService::class.java).apply {
                            putExtra("action", "temp")
                            putExtra(BleService.EXTRA_VALUE, it.toInt())
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            context.startForegroundService(serviceIntent)
                        } else {
                            context.startService(serviceIntent)
                        }
                    }
                    tempRunnable = newRunnable
                    handler.postDelayed(newRunnable, 500) // 500ms 防抖
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
                    
                    // 通过 Service 发送指令
                    val action = if (isRunning) "thrust" else "start"
                    val serviceIntent = Intent(context, BleService::class.java).apply {
                        putExtra("action", action)
                        putExtra(BleService.EXTRA_DEPTH, depth.toInt())
                        putExtra(BleService.EXTRA_EXTEND, extendSpeed.toInt())
                        putExtra(BleService.EXTRA_RETRACT, retractSpeed.toInt())
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    
                    viewModel.setRunning(true)
                    android.widget.Toast.makeText(
                        context,
                        if (action == "start") "启动设备" else "更新参数",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
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
                    
                    // 通过 Service 发送停止指令
                    val serviceIntent = Intent(context, BleService::class.java).apply {
                        putExtra("action", "stop")
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    
                    viewModel.setRunning(false)
                    android.widget.Toast.makeText(
                        context,
                        "停止设备",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
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
    
    // 清理 Handler 回调
    DisposableEffect(Unit) {
        onDispose {
            strengthRunnable?.let { handler.removeCallbacks(it) }
            tempRunnable?.let { handler.removeCallbacks(it) }
        }
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
