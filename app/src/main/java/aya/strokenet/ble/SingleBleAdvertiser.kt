package aya.strokenet.ble

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
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import kotlinx.coroutines.*
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * 单个BLE广播器
 * 每个实例独立管理一个广播通道，互不干扰
 */
class SingleBleAdvertiser(
    private val context: Context,
    private val name: String = "未命名"
) {
    
    private val bluetoothManager: BluetoothManager = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser
    
    private val currentAdvertisingSet = AtomicReference<AdvertisingSet?>(null)
    private val isLoopStopped = AtomicBoolean(true)
    private var broadcastJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val TAG = "SingleBleAdvertiser"
        private const val BROADCAST_SWITCH_DELAY_MS = 100L
    }
    
    private val advertiseSetCallback = object : AdvertisingSetCallback() {
        override fun onAdvertisingSetStarted(set: AdvertisingSet?, txPower: Int, status: Int) {
            if (status == 0) {
                currentAdvertisingSet.set(set)
                Log.d(TAG, "[$name] 广播启动成功: TX功率=$txPower")
            } else {
                currentAdvertisingSet.set(null)
                handleAdvertiseFailure(status)
            }
        }
        override fun onAdvertisingSetStopped(set: AdvertisingSet?) {
            currentAdvertisingSet.set(null)
            Log.d(TAG, "[$name] 广播已停止")
        }
    }
    
    /**
     * 发送单个命令
     */
    fun broadcast(uuid: String) {
        Log.d(TAG, "[$name] 发送命令: $uuid")
        
        // 取消旧的广播作业
        isLoopStopped.set(true)
        broadcastJob?.cancel()
        broadcastJob = null
        
        broadcastJob = coroutineScope.launch {
            try {
                delay(BROADCAST_SWITCH_DELAY_MS)
                startNewBroadcast(uuid)
            } catch (e: Exception) {
                Log.e(TAG, "[$name] 广播失败: ${e.message}", e)
            }
        }
    }
    
    /**
     * 循环发送命令列表
     */
    fun startLoop(uuids: List<String>, delayMs: Long) {
        stop()
        isLoopStopped.set(false)
        
        Log.d(TAG, "[$name] 开始循环广播，命令数量: ${uuids.size}")
        
        broadcastJob = coroutineScope.launch {
            try {
                executeBroadcastLoop(uuids, isLoop = true, delayMs)
            } catch (e: CancellationException) {
                Log.d(TAG, "[$name] 循环被取消")
            } catch (e: Exception) {
                Log.e(TAG, "[$name] 循环异常: ${e.message}", e)
            }
        }
    }
    
    /**
     * 单轮发送命令列表
     */
    fun startSingleRound(uuids: List<String>, delayMs: Long) {
        stop()
        isLoopStopped.set(false)
        
        Log.d(TAG, "[$name] 开始单轮广播，命令数量: ${uuids.size}")
        
        broadcastJob = coroutineScope.launch {
            try {
                executeBroadcastLoop(uuids, isLoop = false, delayMs)
            } catch (e: CancellationException) {
                Log.d(TAG, "[$name] 单轮被取消")
            } catch (e: Exception) {
                Log.e(TAG, "[$name] 单轮异常: ${e.message}", e)
            }
        }
    }
    
    /**
     * 停止广播
     */
    fun stop() {
        isLoopStopped.set(true)
        broadcastJob?.cancel()
        broadcastJob = null
        stopCurrentBroadcast()
    }
    
    /**
     * 关闭广播器
     */
    fun shutdown() {
        stop()
        coroutineScope.cancel()
    }
    
    // ========== 内部实现 ==========
    
    private suspend fun executeBroadcastLoop(uuids: List<String>, isLoop: Boolean, delayMs: Long) {
        var currentIndex = 0
        
        do {
            if (isLoopStopped.get()) {
                Log.d(TAG, "[$name] 循环已停止")
                break
            }
            
            val uuid = uuids[currentIndex]
            
            try {
                startNewBroadcast(uuid)
                delay(delayMs)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "[$name] 广播命令失败: ${e.message}", e)
            }
            
            currentIndex = (currentIndex + 1) % uuids.size
            
            // 单轮模式：执行完所有命令后退出
            if (!isLoop && currentIndex == 0) {
                Log.d(TAG, "[$name] 单轮广播完成")
                break
            }
            
        } while (true)
    }
    
    @SuppressLint("MissingPermission")
    private suspend fun startNewBroadcast(commandUuid: String) {
        // UUID验证
        if (!validateUuid(commandUuid)) return
        
        withContext(Dispatchers.Main) {
            try {
                // 停止之前的广播
                stopCurrentBroadcast()
                
                // 短暂延迟，确保之前的广播完全停止
                delay(BROADCAST_SWITCH_DELAY_MS)
                
                val uuid = UUID.fromString(commandUuid)
                val advertiseData = buildAdvertiseData(uuid)
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    // Android 8.0+ 使用 AdvertisingSet API
                    val parameters = buildAdvertisingSetParameters()
                    
                    advertiser?.startAdvertisingSet(
                        parameters,
                        advertiseData,
                        null,
                        null,
                        null,
                        advertiseSetCallback
                    )
                } else {
                    // Android 5.0-7.1 使用旧版 API
                    val settings = buildAdvertiseSettings()
                    
                    advertiser?.startAdvertising(
                        settings,
                        advertiseData,
                        object : AdvertiseCallback() {
                            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                                Log.d(TAG, "[$name] 广播启动成功")
                            }

                            override fun onStartFailure(errorCode: Int) {
                                handleAdvertiseFailure(errorCode)
                            }
                        }
                    )
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "[$name] 启动广播失败: ${e.message}", e)
                throw e
            }
        }
    }
    
    @SuppressLint("MissingPermission")
    private fun stopCurrentBroadcast() {
        if (bluetoothAdapter?.isEnabled != true) return
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                currentAdvertisingSet.getAndSet(null)?.let {
                    advertiser?.stopAdvertisingSet(advertiseSetCallback)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "[$name] 停止广播失败: ${e.message}", e)
        }
    }
    
    private fun validateUuid(commandUuid: String): Boolean {
        // 检查占位符
        if (commandUuid.contains("**") || commandUuid.contains("####")) {
            Log.e(TAG, "[$name] ❌ UUID未完成替换！")
            return false
        }
        
        // 检查长度
        if (commandUuid.length != 36) {
            Log.e(TAG, "[$name] ❌ UUID长度错误！应该是36字符，实际是${commandUuid.length}字符")
            return false
        }
        
        // 检查格式
        val parts = commandUuid.split("-")
        if (parts.size != 5 || parts[0].length != 8 || parts[1].length != 4 || 
            parts[2].length != 4 || parts[3].length != 4 || parts[4].length != 12) {
            Log.e(TAG, "[$name] ❌ UUID格式错误！")
            return false
        }
        
        return true
    }
    
    private fun buildAdvertiseData(uuid: UUID): AdvertiseData {
        return AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(uuid))
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()
    }
    
    private fun buildAdvertisingSetParameters(): AdvertisingSetParameters {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            AdvertisingSetParameters.Builder()
                .setLegacyMode(true)
                .setInterval(AdvertisingSetParameters.INTERVAL_LOW)
                .setTxPowerLevel(AdvertisingSetParameters.TX_POWER_MEDIUM)
                .setConnectable(false)
                .setScannable(false)
                .build()
        } else {
            throw IllegalStateException("AdvertisingSetParameters requires API 26+")
        }
    }
    
    private fun buildAdvertiseSettings(): AdvertiseSettings {
        return AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(false)
            .setTimeout(0)
            .build()
    }
    
    private fun handleAdvertiseFailure(errorCode: Int) {
        val message = when (errorCode) {
            AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "广播数据过大"
            AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "广播器数量过多"
            AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "BLE广播内部错误"
            AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "设备不支持BLE广播"
            else -> "未知错误，错误码: $errorCode"
        }
        Log.e(TAG, "[$name] 广播失败: $message")
    }
}
