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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import aya.strokenet.ble.DaxiuBleAdvertiser
import aya.strokenet.BleService
import aya.strokenet.HeatingTimerService
import aya.strokenet.LoopPresetService
import aya.strokenet.data.model.ControlParams
import aya.strokenet.ui.theme.*
import aya.strokenet.ui.components.GlassPanel
import aya.strokenet.ui.components.StatusIndicator

@Composable
fun ControlScreen(
    bleAdvertiser: DaxiuBleAdvertiser?,
    onCheckBluetooth: () -> Unit,
    viewModel: aya.strokenet.ui.viewmodel.ControlViewModel,
    modifier: Modifier = Modifier
) {
    // 使用ViewModel中的状态，页面切换时数据不会丢失
    var isBluetoothEnabled by remember { mutableStateOf(true) }
    
    val context = LocalContext.current

    // 统一发送所有参数的函数
    val sendAllParameters: () -> Unit = {
        if (!isBluetoothEnabled) {
            onCheckBluetooth()
        } else {
            // 1. 先停止预设循环播放（如果正在播放）
            context.stopService(Intent(context, LoopPresetService::class.java))
            
            // 2. 使用批量发送API
            val intent = Intent(context, BleService::class.java).apply {
                putExtra("action", BleService.ACTION_SEND_ALL)
                putExtra(BleService.EXTRA_DEPTH, viewModel.depth.toInt())
                putExtra(BleService.EXTRA_EXTEND, viewModel.extendSpeed.toInt())
                putExtra(BleService.EXTRA_RETRACT, viewModel.retractSpeed.toInt())
                putExtra(BleService.EXTRA_STRENGTH, viewModel.strength.toInt())
                // 如果正在加热，包含温度参数
                if (viewModel.isHeating) {
                    putExtra(BleService.EXTRA_TEMP, viewModel.temp.toInt())
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            
            viewModel.isRunning = true
        }
    }

    // 初始检查蓝牙状态
    LaunchedEffect(Unit) {
        isBluetoothEnabled = bleAdvertiser?.isBluetoothEnabled() ?: false
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
                    HeatingTimerService.BROADCAST_TIMER_STOPPED -> {
                        // 定时器停止，同步UI状态
                        viewModel.isHeating = false
                        viewModel.tempDuration = 0f
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(HeatingTimerService.BROADCAST_TIMER_STOPPED)
        }
        
        // Android 13+ 需要指定 RECEIVER_NOT_EXPORTED
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(
                context,
                bluetoothReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
        
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
            isRunning = viewModel.isRunning,
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
                value = viewModel.depth,
                range = ControlParams.DEPTH_MIN.toFloat()..ControlParams.DEPTH_MAX.toFloat(),
                onValueChange = { viewModel.depth = it },
                onValueChangeFinished = { sendAllParameters() }
            )

            Divider(
                color = Color.Black.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            AppleStyleSlider(
                label = "伸出速度",
                value = viewModel.extendSpeed,
                range = ControlParams.SPEED_MIN.toFloat()..ControlParams.SPEED_MAX.toFloat(),
                onValueChange = { viewModel.extendSpeed = it },
                onValueChangeFinished = { sendAllParameters() }
            )

            Divider(
                color = Color.Black.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            AppleStyleSlider(
                label = "缩回速度",
                value = viewModel.retractSpeed,
                range = ControlParams.SPEED_MIN.toFloat()..ControlParams.SPEED_MAX.toFloat(),
                onValueChange = { viewModel.retractSpeed = it },
                onValueChangeFinished = { sendAllParameters() }
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

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "震动强度",
                        fontSize = 15.sp,
                        color = iOSTextPrimary
                    )
                    Text(
                        text = "${viewModel.strength.toInt()}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSBlue
                    )
                }
                Slider(
                    value = viewModel.strength,
                    onValueChange = { viewModel.strength = it },
                    onValueChangeFinished = { sendAllParameters() },
                    valueRange = ControlParams.STRENGTH_MIN.toFloat()..ControlParams.STRENGTH_MAX.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = iOSBlue,
                        inactiveTrackColor = Color.Black.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Divider(
                color = Color.Black.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            AppleStyleSlider(
                label = "加热温度",
                value = viewModel.temp,
                range = ControlParams.TEMP_MIN.toFloat()..ControlParams.TEMP_MAX.toFloat(),
                unit = "°C",
                onValueChange = { viewModel.temp = it },
                onValueChangeFinished = {
                    // 如果正在加热，更新温度
                    if (viewModel.isHeating) {
                        sendAllParameters()
                    }
                }
            )

            Divider(
                color = Color.Black.copy(alpha = 0.05f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            // 加热时长控制
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "加热时长",
                        fontSize = 15.sp,
                        color = iOSTextPrimary
                    )
                    Text(
                        text = "${viewModel.tempDuration.toInt()}分钟",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (viewModel.tempDuration > 0) iOSBlue else iOSTextSecondary
                    )
                }
                Slider(
                    value = viewModel.tempDuration,
                    onValueChange = { viewModel.tempDuration = it },
                    onValueChangeFinished = {
                        // 滑块停止时处理
                        if (viewModel.tempDuration > 0) {
                            // 检查温度是否设置
                            if (viewModel.temp <= 0) {
                                android.widget.Toast.makeText(context, "请先调整温度", android.widget.Toast.LENGTH_SHORT).show()
                                viewModel.tempDuration = 0f
                                return@Slider
                            }
                            
                            // 启动加热定时器服务
                            viewModel.isHeating = true
                            val timerIntent = Intent(context, HeatingTimerService::class.java).apply {
                                putExtra("action", HeatingTimerService.ACTION_START_TIMER)
                                putExtra(HeatingTimerService.EXTRA_DURATION_MINUTES, viewModel.tempDuration.toInt())
                                putExtra(HeatingTimerService.EXTRA_TEMPERATURE, viewModel.temp.toInt())
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(timerIntent)
                            } else {
                                context.startService(timerIntent)
                            }
                            
                            android.widget.Toast.makeText(
                                context,
                                "加热已启动，${viewModel.tempDuration.toInt()}分钟后自动关闭",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            // 停止加热
                            viewModel.isHeating = false
                            val timerIntent = Intent(context, HeatingTimerService::class.java).apply {
                                putExtra("action", HeatingTimerService.ACTION_STOP_TIMER)
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(timerIntent)
                            } else {
                                context.startService(timerIntent)
                            }
                            
                            android.widget.Toast.makeText(context, "加热已停止", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    valueRange = ControlParams.TEMP_DURATION_MIN.toFloat()..ControlParams.TEMP_DURATION_MAX.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.White,
                        activeTrackColor = iOSBlue,
                        inactiveTrackColor = Color.Black.copy(alpha = 0.08f)
                    ),
                    modifier = Modifier.padding(top = 4.dp)
                )
                if (viewModel.tempDuration == 0f && !viewModel.isHeating) {
                    Text(
                        text = "💡 拉动滑块设置加热时长并启动",
                        fontSize = 12.sp,
                        color = iOSTextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
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
                    
                    // 检查蓝牙状态
                    if (!isBluetoothEnabled) {
                        onCheckBluetooth()
                        return@Button
                    }
                    
                    // 发送所有参数
                    sendAllParameters()
                    
                    val message = if (viewModel.isRunning) "更新参数" else {
                        if (viewModel.isHeating) "启动设备（推拉+震动+加热）" else "启动设备（推拉+震动）"
                    }
                    android.widget.Toast.makeText(context, message, android.widget.Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
            ) {
                Text(
                    if (viewModel.isRunning) "调节更新" else "启动设备",
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
                    
                    // 停止预设循环播放
                    context.stopService(Intent(context, LoopPresetService::class.java))
                    
                    // 发送全部停止命令并持续2秒
                    val serviceIntent = Intent(context, BleService::class.java).apply {
                        putExtra("action", BleService.ACTION_STOP_ALL)
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    
                    // 停止加热定时器服务
                    if (viewModel.isHeating) {
                        viewModel.isHeating = false
                        viewModel.tempDuration = 0f
                        context.stopService(Intent(context, HeatingTimerService::class.java))
                    }
                    
                    viewModel.isRunning = false
                    android.widget.Toast.makeText(
                        context,
                        "正在停止设备...",
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
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null
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
            onValueChangeFinished = onValueChangeFinished,
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
