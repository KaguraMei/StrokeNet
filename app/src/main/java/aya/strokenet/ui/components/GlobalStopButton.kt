package aya.strokenet.ui.components

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aya.strokenet.BleService
import aya.strokenet.LoopPresetService
import aya.strokenet.ui.theme.iOSRed

/**
 * 全局悬浮停止按钮
 * 当设备活动时显示，点击停止所有运动
 */
@Composable
fun GlobalStopButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var isDeviceActive by remember { mutableStateOf(false) }
    
    // 监听设备状态广播
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                when (intent?.action) {
                    BleService.BROADCAST_DEVICE_ACTIVE -> {
                        isDeviceActive = true
                    }
                    BleService.BROADCAST_DEVICE_STOPPED,
                    LoopPresetService.BROADCAST_LOOP_STOPPED -> {
                        isDeviceActive = false
                    }
                }
            }
        }
        
        val filter = IntentFilter().apply {
            addAction(BleService.BROADCAST_DEVICE_ACTIVE)
            addAction(BleService.BROADCAST_DEVICE_STOPPED)
            addAction(LoopPresetService.BROADCAST_LOOP_STOPPED)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
    
    // 呼吸动画
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    
    AnimatedVisibility(
        visible = isDeviceActive,
        enter = fadeIn() + scaleIn(),
        exit = fadeOut() + scaleOut()
    ) {
        Box(modifier = modifier) {
            FloatingActionButton(
                onClick = {
                    // 停止所有运动
                    val stopIntent = Intent(context, BleService::class.java).apply {
                        putExtra("action", BleService.ACTION_STOP_ALL)
                    }
                    
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(stopIntent)
                    } else {
                        context.startService(stopIntent)
                    }
                    
                    // 停止循环预设服务
                    context.stopService(Intent(context, LoopPresetService::class.java))
                    
                    isDeviceActive = false
                },
                containerColor = iOSRed,
                contentColor = Color.White,
                modifier = Modifier
                    .size((56 * scale).dp)
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Stop,
                    contentDescription = "停止",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
