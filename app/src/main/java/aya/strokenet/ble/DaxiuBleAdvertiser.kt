package aya.strokenet.ble

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.AdvertisingSet
import android.bluetooth.le.AdvertisingSetCallback
import android.bluetooth.le.AdvertisingSetParameters
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 大秀设备BLE广播器
 * 基于官方APP完整逆向的实现
 * 
 * 使用两个独立的广播器实例：
 * - thrustAdvertiser: 专门处理推拉命令（8800）
 * - strengthAdvertiser: 专门处理震动命令（8200）
 * 
 * 这样可以避免调整参数时互相干扰，防止设备"抽搐"
 */
class DaxiuBleAdvertiser(private val context: Context) {
    
    private val bluetoothManager: BluetoothManager = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    
    // 两个独立的广播器实例
    private val thrustAdvertiser = SingleBleAdvertiser(context, "推拉")
    private val strengthAdvertiser = SingleBleAdvertiser(context, "震动")
    private val tempAdvertiser = SingleBleAdvertiser(context, "温度")
    
    // 用于循环播放预设的广播器
    private val loopAdvertiser = SingleBleAdvertiser(context, "循环")
    
    companion object {
        private const val TAG = "DaxiuBleAdvertiser"
    }

    /**
     * 发送推拉命令（不影响震动）
     */
    fun sendThrustCommand(uuid: String) {
        Log.d(TAG, "发送推拉命令: $uuid")
        thrustAdvertiser.broadcast(uuid)
    }
    
    /**
     * 发送震动命令（不影响推拉）
     */
    fun sendStrengthCommand(uuid: String) {
        Log.d(TAG, "发送震动命令: $uuid")
        strengthAdvertiser.broadcast(uuid)
    }
    
    /**
     * 发送温度命令（不影响推拉和震动）
     */
    fun sendTemperatureCommand(uuid: String) {
        Log.d(TAG, "发送温度命令: $uuid")
        tempAdvertiser.broadcast(uuid)
    }
    
    /**
     * 发送单个命令（自动识别类型）
     * 兼容旧代码
     */
    fun startSingleBroadcast(uuid: String) {
        Log.d(TAG, "========== 发送单个命令 ==========")
        Log.d(TAG, "命令UUID: $uuid")
        
        // 根据命令类型选择对应的广播器
        when {
            uuid.contains("-8800-") -> sendThrustCommand(uuid)  // 推拉
            uuid.contains("-8200-") || uuid.contains("-0200-") -> sendStrengthCommand(uuid)  // 震动
            uuid.contains("-9000-") -> sendTemperatureCommand(uuid)  // 温度
            uuid.contains("-1F00-") -> loopAdvertiser.broadcast(uuid)  // 停止/发现
            else -> {
                Log.w(TAG, "未知命令类型，使用循环广播器")
                loopAdvertiser.broadcast(uuid)
            }
        }
        
        Log.d(TAG, "====================================")
    }
    
    /**
     * 循环发送命令列表（UUID字符串列表）
     * 用于预设循环播放
     */
    fun startLoopBroadcast(uuids: List<String>?, delayMs: Long = 1000L) {
        if (uuids.isNullOrEmpty()) {
            Log.w(TAG, "命令列表为空")
            return
        }
        
        Log.d(TAG, "开始循环广播，命令数量: ${uuids.size}")
        loopAdvertiser.startLoop(uuids, delayMs)
    }
    
    /**
     * 单轮发送命令列表（UUID字符串列表）
     */
    fun startSingleRoundBroadcast(uuids: List<String>, delayMs: Long = 1000L) {
        if (uuids.isEmpty()) {
            Log.w(TAG, "命令列表为空")
            return
        }
        
        Log.d(TAG, "开始单轮广播，命令数量: ${uuids.size}")
        loopAdvertiser.startSingleRound(uuids, delayMs)
    }
    
    /**
     * 停止循环广播
     */
    fun stopBroadcastJob(stopUuid: String? = null) {
        Log.d(TAG, "========== 停止循环广播 ==========")
        loopAdvertiser.stop()
        
        stopUuid?.let {
            Log.d(TAG, "发送停止命令: $it")
            startSingleBroadcast(it)
        }
        
        Log.d(TAG, "====================================")
    }
    
    /**
     * 停止所有广播
     */
    fun stopCurrentBroadcast() {
        Log.d(TAG, "========== 停止所有广播 ==========")
        
        thrustAdvertiser.stop()
        strengthAdvertiser.stop()
        tempAdvertiser.stop()
        loopAdvertiser.stop()
        
        Log.d(TAG, "✓ 所有广播已停止")
        Log.d(TAG, "====================================")
    }
    
    /**
     * 完全关闭
     */
    fun shutdown() {
        try {
            stopCurrentBroadcast()
            thrustAdvertiser.shutdown()
            strengthAdvertiser.shutdown()
            tempAdvertiser.shutdown()
            loopAdvertiser.shutdown()
            Log.d(TAG, "广播系统已关闭")
        } catch (e: Exception) {
            Log.e(TAG, "关闭失败: ${e.message}", e)
        }
    }
    
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADMIN
            ) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * 获取当前设备ID（持久化）
     */
    fun getDeviceId(): String {
        return aya.strokenet.utils.getControlDeviceId(context)
    }
    
    /**
     * 发送设备发现广播
     */
    fun sendDiscoverBroadcast(uuid: String) {
        Log.d(TAG, "发送设备发现广播: $uuid")
        loopAdvertiser.broadcast(uuid)
    }
}
