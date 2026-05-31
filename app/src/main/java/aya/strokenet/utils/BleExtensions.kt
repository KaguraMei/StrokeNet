package aya.strokenet.utils

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import kotlin.random.Random

/**
 * BLE相关扩展函数
 * 移植自官方实现的 DataExtKt
 */

/**
 * 将整数转换为2位十六进制字符串
 * @param uppercase 是否大写，默认true
 */
fun Int.toTwoHexString(uppercase: Boolean = true): String {
    return String.format(if (uppercase) "%02X" else "%02x", this)
}

/**
 * 获取持久化的设备ID（4位十六进制）
 * 首次调用时生成并保存，后续调用返回相同值
 */
fun getControlDeviceId(context: Context): String {
    val prefs = context.getSharedPreferences("ble_settings", Context.MODE_PRIVATE)
    var deviceId = prefs.getString("ble_device_id", null)
    
    if (deviceId.isNullOrEmpty()) {
        // 生成新的设备ID（0x0000 - 0xFFFF）
        deviceId = Random.nextInt(0, 65536).toString(16).padStart(4, '0').uppercase()
        prefs.edit().putString("ble_device_id", deviceId).apply()
    }
    
    return deviceId
}

/**
 * 生成随机命令ID（2位十六进制）
 * 每次调用都生成新值
 */
fun generateControlCommandId(): String {
    return Random.nextInt(0, 256).toTwoHexString()
}

/**
 * 计算UUID校验和
 * 将UUID所有字节相加对256取模
 * 
 * @param uuidWithoutHyphens UUID字符串（不含连字符）
 * @return 2位十六进制校验和
 */
fun makeCheckSum(uuidWithoutHyphens: String): String {
    var sum = 0
    var i = 0
    
    while (i < uuidWithoutHyphens.length) {
        val endIndex = minOf(i + 2, uuidWithoutHyphens.length)
        val hex = uuidWithoutHyphens.substring(i, endIndex)
        
        // 处理边界情况：单字符需要乘以16
        sum += if (hex.length == 1) {
            hex.toInt(16) * 16
        } else {
            hex.toInt(16)
        }
        
        i += 2
    }
    
    return (sum % 256).toTwoHexString()
}

/**
 * 检测是否为小米设备
 */
fun isXiaomiDevice(): Boolean {
    return Build.MANUFACTURER.equals("xiaomi", ignoreCase = true)
}

/**
 * 构建完整的BLE命令UUID
 * 
 * @param template 命令模板，包含占位符 ** (命令ID) 和 #### (设备ID)
 * @param context 上下文，用于获取设备ID
 * @return 完整的UUID字符串（包含校验和）
 */
fun buildBleCommand(template: String, context: Context): String {
    // 1. 替换设备ID占位符（持久化的4位十六进制）
    val deviceId = getControlDeviceId(context)
    val withDeviceId = template.replace("####", deviceId)
    
    // 2. 替换命令ID占位符（随机的2位十六进制）
    val commandId = generateControlCommandId()
    val filled = withDeviceId.replace("**", commandId)
    
    // 3. 计算并追加校验和
    val checksum = makeCheckSum(filled.replace("-", ""))
    return filled + checksum
}
