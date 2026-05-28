package aya.strokenet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aya.strokenet.ui.theme.*

@Composable
fun StatusIndicator(
    isRunning: Boolean,
    isBluetoothEnabled: Boolean
) {
    GlassPanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 运行状态点
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(
                        if (isRunning) iOSGreen else Color.LightGray,
                        CircleShape
                    )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isRunning) "设备运行中" else "设备已待机",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Icon(
                        imageVector = if (isBluetoothEnabled) Icons.Default.CheckCircle else Icons.Default.Error,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = if (isBluetoothEnabled) iOSBlue else Color.Red
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isBluetoothEnabled) "蓝牙已启用" else "蓝牙未启用",
                        fontSize = 13.sp,
                        color = iOSTextSecondary
                    )
                }
            }
        }
    }
}
