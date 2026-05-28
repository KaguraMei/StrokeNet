package aya.strokenet.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aya.strokenet.ui.theme.*
import aya.strokenet.ui.components.GlassPanel

/**
 * iOS 风格设置页面
 */
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 关于应用
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
                        Icons.Default.Info,
                        contentDescription = null,
                        tint = iOSBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "应用信息",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
            }

            SettingItem(label = "应用名称", value = "StrokeNet")
            Divider(color = Color.Black.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))
            SettingItem(label = "版本号", value = "1.0.0")
            Divider(color = Color.Black.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))
            SettingItem(label = "构建日期", value = "2026.05.28")
        }

        // 技术规格
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
                        Icons.Default.DataObject,
                        contentDescription = null,
                        tint = iOSBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "技术规格",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
            }

            SettingItem(label = "包名", value = "aya.strokenet")
            Divider(color = Color.Black.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))
            SettingItem(label = "协议", value = "BLE 广播")
            Divider(color = Color.Black.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))
            SettingItem(label = "最低系统", value = "Android 12 (API 31)")
            Divider(color = Color.Black.copy(alpha = 0.05f), modifier = Modifier.padding(vertical = 8.dp))
            SettingItem(label = "许可证", value = "MIT License")
        }

        // Intent API 示例
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
                        Icons.Default.IntegrationInstructions,
                        contentDescription = null,
                        tint = iOSBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "外部控制接口",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = iOSTextPrimary
                )
            }

            Text(
                text = "启动命令示例：",
                fontSize = 13.sp,
                color = iOSTextSecondary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Text(
                    text = "am start -n aya.strokenet/.MainActivity --es action start --ei depth 36 --ei extend 8 --ei retract 8",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = iOSTextPrimary,
                    lineHeight = 16.sp
                )
            }
        }

        // 权限说明
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(iOSRed.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
                .border(1.dp, iOSRed.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Warning,
                        contentDescription = null,
                        tint = iOSRed,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "所需权限",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )
                }
                Text(
                    text = "• 蓝牙广播权限 (BLUETOOTH_ADVERTISE)\n• 蓝牙连接权限 (BLUETOOTH_CONNECT)\n• 精确位置权限 (ACCESS_FINE_LOCATION)",
                    fontSize = 13.sp,
                    color = iOSTextSecondary,
                    lineHeight = 20.sp
                )
                Text(
                    text = "请在系统设置中手动授予这些权限",
                    fontSize = 12.sp,
                    color = iOSTextSecondary,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun SettingItem(
    label: String,
    value: String
) {
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
            text = value,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = iOSTextSecondary
        )
    }
}
