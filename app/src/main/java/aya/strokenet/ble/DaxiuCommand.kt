package aya.strokenet.ble

import android.content.Context
import aya.strokenet.data.model.ControlParams
import aya.strokenet.utils.*
import kotlin.math.roundToInt

/**
 * 大秀BLE命令构造器
 * 基于官方APP逆向分析的协议实现
 * 
 * UUID格式: 710003XX-YYYY-ZZZZ-0000-WWWWWWWWWWCC
 * - 710003: 固定前缀（设备类型 = DaXiu）
 * - XX: 命令ID（2位十六进制，每次随机生成）
 * - YYYY: 命令类型（8800=推拉, 9000=温度, 1000=强度等）
 * - ZZZZ: 设备ID（4位十六进制，持久化存储）
 * - 0000-WWWWWWWWWW: 参数数据（12位十六进制）
 * - CC: 校验和（2位十六进制）
 */
object DaxiuCommand {
    
    // 命令模板（使用占位符 ** = 命令ID, #### = 设备ID）
    // UUID格式: 710003XX-YYYY-ZZZZ-0000-WWWWWWWWWW[CC]
    // 最后段: 10位参数 + 2位校验和(自动追加) = 12位
    private const val TEMPLATE_THRUST = "710003**-8800-####-0000-%s0000"  // 6位参数 + 4位0 = 10位
    private const val TEMPLATE_TEMP = "710003**-9000-####-0000-%s00000000"  // 2位参数 + 8位0 = 10位
    private const val TEMPLATE_STRENGTH = "710003**-8200-####-0100-640000%s02"  // 6位固定 + 2位参数 + 02 = 10位
    private const val TEMPLATE_STOP_THRUST = "710003**-8800-####-0000-0000000000"  // 伸缩停止：10位0
    private const val TEMPLATE_STOP_STRENGTH = "710003**-0200-####-0100-0000000002"  // 震动停止：特殊命令类型0200
    private const val TEMPLATE_STOP_ALL = "710003**-1F00-####-0000-0000000000"  // 全部停止：10位0
    private const val TEMPLATE_DISCOVER = "710003**-1F00-####-0000-0000000000"  // 设备发现：10位0
    
    /**
     * 构建控制命令（推拉+速度）
     * 
     * @param params 控制参数 (UI范围: 1-100)
     * @param context 上下文（用于获取设备ID）
     * @return 完整的UUID字符串（包含校验和）
     */
    fun buildControlCommand(params: ControlParams, context: Context): String {
        // 深度映射: 1-100 -> 14-72
        val mappedDepth = if (params.depth <= 0) {
            0
        } else {
            (((params.depth - 1) * 58) / 99.0f + 14).roundToInt()
        }
        
        // 速度映射: 1-100 -> 2-15 (官方原始逻辑: progress * 0.15f, progress 是 0-100)
        val mappedExtend = (params.extendSpeed * 0.15f).roundToInt().coerceAtLeast(2)
        val mappedRetract = (params.retractSpeed * 0.15f).roundToInt().coerceAtLeast(2)
        
        // 参数编码: 深度(2位) + 伸出速度(2位) + 缩回速度(2位)
        val paramsHex = String.format(
            "%s%s%s",
            mappedDepth.toTwoHexString(),
            mappedExtend.toTwoHexString(),
            mappedRetract.toTwoHexString()
        )
        
        val template = String.format(TEMPLATE_THRUST, paramsHex)
        return buildBleCommand(template, context)
    }
    
    /**
     * 构建温度命令
     * 
     * @param temp 温度值 (0-60, UI显示范围)
     * @param context 上下文
     * @return 完整的UUID字符串
     */
    fun buildTemperatureCommand(temp: Int, context: Context): String {
        // 温度映射: 0-60 -> 30-60
        val mappedTemp = ((temp * 30) / 60) + 30
        val tempHex = mappedTemp.coerceIn(30, 60).toTwoHexString()
        
        val template = String.format(TEMPLATE_TEMP, tempHex)
        return buildBleCommand(template, context)
    }
    
    /**
     * 构建强度命令
     * 
     * @param strength 强度值 (1-100, UI显示范围)
     * @param context 上下文
     * @return 完整的UUID字符串
     */
    fun buildStrengthCommand(strength: Int, context: Context): String {
        // 强度直接使用，范围1-100
        val strengthHex = strength.coerceIn(1, 100).toTwoHexString()
        
        val template = String.format(TEMPLATE_STRENGTH, strengthHex)
        return buildBleCommand(template, context)
    }
    
    /**
     * 构建停止命令（停止推拉）
     * 
     * @param context 上下文
     * @return 完整的UUID字符串
     */
    fun buildStopCommand(context: Context): String {
        return buildBleCommand(TEMPLATE_STOP_THRUST, context)
    }
    
    /**
     * 构建停止强度命令
     * 
     * @param context 上下文
     * @return 完整的UUID字符串
     */
    fun buildStopStrengthCommand(context: Context): String {
        return buildBleCommand(TEMPLATE_STOP_STRENGTH, context)
    }
    
    /**
     * 构建全部停止命令
     * 
     * @param context 上下文
     * @return 完整的UUID字符串
     */
    fun buildStopAllCommand(context: Context): String {
        return buildBleCommand(TEMPLATE_STOP_ALL, context)
    }
    
    /**
     * 构建设备发现命令
     * 
     * @param context 上下文
     * @return 完整的UUID字符串
     */
    fun buildDiscoverCommand(context: Context): String {
        return buildBleCommand(TEMPLATE_DISCOVER, context)
    }
}
