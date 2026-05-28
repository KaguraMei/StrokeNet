package aya.strokenet

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.UUID

class BleAdvertiser(private val context: Context) {
    
    private val bluetoothManager: BluetoothManager = 
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser
    
    private var currentCallback: AdvertiseCallback? = null
    
    companion object {
        private const val TAG = "BleAdvertiser"
        
        // UUID 基础模板: 710003f8-1f00-bbbf-0000-000000000005
        // 根据逆向结果，需要在特定字节位置编码参数
        private const val UUID_TEMPLATE = "710003f8-1f00-bbbf-%04x-%012x"
    }
    
    /**
     * 构造控制 UUID
     * 根据逆向的协议，将参数编码到 UUID 的特定位置
     */
    private fun buildControlUuid(
        action: String,
        depth: Int = 0,
        extendSpeed: Int = 0,
        retractSpeed: Int = 0,
        strength: Int = 0,
        temp: Int = 0
    ): UUID {
        // 这里需要根据实际逆向的字节位置来编码
        // 示例编码方式（需要根据实际抓包结果调整）
        val param1 = when (action) {
            "start", "thrust" -> {
                // 推拉参数编码: depth(0-72), extend(0-15), retract(0-15)
                ((depth and 0xFF) shl 8) or ((extendSpeed and 0x0F) shl 4) or (retractSpeed and 0x0F)
            }
            "strength" -> {
                // 强度参数编码
                0x1000 or (strength and 0xFF)
            }
            "temp" -> {
                // 温度参数编码
                0x2000 or (temp and 0xFF)
            }
            "stop" -> {
                // 停止指令
                0x0000
            }
            else -> 0x0000
        }
        
        val param2 = when (action) {
            "start" -> 0x000000000001L
            "thrust" -> 0x000000000002L
            "strength" -> 0x000000000003L
            "temp" -> 0x000000000004L
            "stop" -> 0x000000000005L
            else -> 0x000000000000L
        }
        
        val uuidString = String.format(UUID_TEMPLATE, param1, param2)
        Log.d(TAG, "Generated UUID: $uuidString for action: $action")
        return UUID.fromString(uuidString)
    }
    
    /**
     * 发送 BLE 广播
     */
    fun advertise(
        action: String,
        depth: Int = 0,
        extendSpeed: Int = 0,
        retractSpeed: Int = 0,
        strength: Int = 0,
        temp: Int = 0,
        onSuccess: () -> Unit = {},
        onFailure: (Int) -> Unit = {}
    ) {
        // 检查权限
        if (!hasBluetoothPermissions()) {
            Log.e(TAG, "Missing Bluetooth permissions")
            onFailure(-3) // -3 表示权限不足
            return
        }
        
        // 检查蓝牙是否启用
        if (!isBluetoothAvailable()) {
            Log.e(TAG, "Bluetooth is not enabled")
            onFailure(-4) // -4 表示蓝牙未启用
            return
        }
        
        if (advertiser == null) {
            Log.e(TAG, "BLE Advertiser not available")
            onFailure(-1)
            return
        }
        
        // 停止之前的广播
        stopAdvertising()
        
        val uuid = buildControlUuid(action, depth, extendSpeed, retractSpeed, strength, temp)
        
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(false)
            .setTimeout(500) // 500ms 后自动停止
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .build()
        
        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(uuid))
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .build()
        
        val callback = object : AdvertiseCallback() {
            override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
                super.onStartSuccess(settingsInEffect)
                Log.d(TAG, "Advertising started successfully")
                onSuccess()
            }
            
            override fun onStartFailure(errorCode: Int) {
                super.onStartFailure(errorCode)
                Log.e(TAG, "Advertising failed with error code: $errorCode")
                onFailure(errorCode)
            }
        }
        
        currentCallback = callback
        
        try {
            advertiser.startAdvertising(settings, data, callback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
            onFailure(-2)
        }
    }
    
    /**
     * 停止广播
     */
    fun stopAdvertising() {
        currentCallback?.let { callback ->
            try {
                advertiser?.stopAdvertising(callback)
                Log.d(TAG, "Advertising stopped")
            } catch (e: SecurityException) {
                Log.e(TAG, "Failed to stop advertising: ${e.message}")
            }
            currentCallback = null
        }
    }
    
    /**
     * 检查是否有必要的蓝牙权限
     */
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            // Android 11 及以下
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
    
    /**
     * 检查蓝牙是否可用
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled
    }
}
