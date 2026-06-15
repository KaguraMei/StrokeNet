package aya.strokenet.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
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
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    val versionName = remember { getVersionName(context) }
    val versionCode = remember { getVersionCode(context) }
    
    // 检查电池优化状态
    var isBatteryOptimizationIgnored by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isBatteryOptimizationIgnored = checkBatteryOptimization(context)
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // MCP 后台稳定性设置
        GlassPanel {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            if (isBatteryOptimizationIgnored) iOSGreen.copy(alpha = 0.1f) else iOSYellow.copy(alpha = 0.1f),
                            RoundedCornerShape(8.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isBatteryOptimizationIgnored) Icons.Default.BatteryChargingFull else Icons.Default.BatteryAlert,
                        contentDescription = null,
                        tint = if (isBatteryOptimizationIgnored) iOSGreen else iOSYellow,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        "MCP 后台保活",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSTextPrimary
                    )
                    Text(
                        if (isBatteryOptimizationIgnored) "已优化" else "需要配置",
                        fontSize = 13.sp,
                        color = if (isBatteryOptimizationIgnored) iOSGreen else iOSYellow
                    )
                }
            }

            Text(
                text = "为了确保 MCP Server 在后台稳定运行，建议进行以下设置：",
                fontSize = 14.sp,
                color = iOSTextPrimary,
                lineHeight = 20.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            
            // 电池优化按钮
            Button(
                onClick = {
                    requestBatteryOptimization(context)
                    // 延迟刷新状态
                    android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                        isBatteryOptimizationIgnored = checkBatteryOptimization(context)
                    }, 1000)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isBatteryOptimizationIgnored) iOSGreen else iOSBlue
                )
            ) {
                Icon(
                    if (isBatteryOptimizationIgnored) Icons.Default.CheckCircle else Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (isBatteryOptimizationIgnored) "电池优化已关闭" else "关闭电池优化",
                    fontSize = 15.sp
                )
            }
            
            // 多任务上锁提示
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(iOSBlue.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "📌 多任务上锁（推荐）",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = iOSBlue
                    )
                    Text(
                        text = "打开多任务界面，长按 StrokeNet 卡片，点击锁定图标，防止系统清理。",
                        fontSize = 12.sp,
                        color = iOSTextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }
        }
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
            SettingItem(label = "版本号", value = "$versionName (Build $versionCode)")
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

/**
 * 检查是否已忽略电池优化
 */
private fun checkBatteryOptimization(context: Context): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }
    return true // Android 6.0 以下不需要此权限
}

/**
 * 请求忽略电池优化
 */
private fun requestBatteryOptimization(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:${context.packageName}")
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("SettingsScreen", "Failed to request battery optimization", e)
                // 如果打开失败，尝试打开通用电池优化设置页面
                try {
                    val fallbackIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    context.startActivity(fallbackIntent)
                } catch (e2: Exception) {
                    android.util.Log.e("SettingsScreen", "Failed to open battery settings", e2)
                }
            }
        }
    }
}

/**
 * 获取应用版本名称
 */
private fun getVersionName(context: Context): String {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        packageInfo.versionName ?: "未知"
    } catch (e: Exception) {
        "未知"
    }
}

/**
 * 获取应用版本号
 */
private fun getVersionCode(context: Context): Long {
    return try {
        val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo.versionCode.toLong()
        }
    } catch (e: Exception) {
        0L
    }
}
