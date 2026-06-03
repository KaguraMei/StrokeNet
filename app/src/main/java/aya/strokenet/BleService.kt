package aya.strokenet

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import aya.strokenet.ble.DaxiuBleAdvertiser
import aya.strokenet.ble.DaxiuCommand

/**
 * 前台服务：发送 BLE 指令
 * 使用官方逆向的完整协议实现
 */
class BleService : Service() {
    
    companion object {
        private const val TAG = "BleService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ble_service_channel"
        
        // Broadcast Actions
        const val BROADCAST_DEVICE_ACTIVE = "aya.strokenet.DEVICE_ACTIVE"
        const val BROADCAST_DEVICE_STOPPED = "aya.strokenet.DEVICE_STOPPED"
        
        // Service Actions
        const val ACTION_START = "start"
        const val ACTION_THRUST = "thrust"
        const val ACTION_STRENGTH = "strength"
        const val ACTION_TEMP = "temp"
        const val ACTION_STOP = "stop"
        const val ACTION_STOP_THRUST = "stop_thrust"  // 新增：停止伸缩
        const val ACTION_STOP_STRENGTH = "stop_strength"  // 新增：停止震动
        const val ACTION_STOP_ALL = "stop_all"  // 新增：全部停止
        const val ACTION_SEND_ALL = "send_all"  // 发送所有参数
        
        // Intent Extras
        const val EXTRA_DEPTH = "depth"
        const val EXTRA_EXTEND = "extend"
        const val EXTRA_RETRACT = "retract"
        const val EXTRA_VALUE = "value"
        const val EXTRA_STRENGTH = "strength"  // 新增
        const val EXTRA_TEMP = "temp"  // 新增
    }
    
    private lateinit var bleAdvertiser: DaxiuBleAdvertiser
    private val handler = Handler(Looper.getMainLooper())
    private var isServiceActive = false // 跟踪服务是否处于活动状态
    
    // 维护当前设备状态
    private var currentDepth = 36
    private var currentExtend = 8
    private var currentRetract = 8
    private var currentStrength = 50
    private var currentTemp = 30
    private var isRunning = false
    
    override fun onCreate() {
        super.onCreate()
        bleAdvertiser = DaxiuBleAdvertiser(this)
        createNotificationChannel()
        Log.d(TAG, "Service created, Device ID: ${bleAdvertiser.getDeviceId()}")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 首次启动或重新启动前台服务
        if (!isServiceActive) {
            startForeground(NOTIFICATION_ID, buildNotification(buildFullStatusText()))
            isServiceActive = true
            Log.d(TAG, "Foreground service started")
        }
        
        intent?.let { handleIntent(it) }
        
        return START_STICKY // 改为 STICKY，保持服务运行
    }
    
    private fun handleIntent(intent: Intent) {
        val action = intent.getStringExtra("action") ?: return
        
        Log.d(TAG, "Received action: $action")
        
        when (action) {
            ACTION_SEND_ALL -> {
                // 批量发送所有参数
                val depth = intent.getIntExtra(EXTRA_DEPTH, 36)
                val extend = intent.getIntExtra(EXTRA_EXTEND, 8)
                val retract = intent.getIntExtra(EXTRA_RETRACT, 8)
                val strength = intent.getIntExtra(EXTRA_STRENGTH, 50)
                val temp = intent.getIntExtra(EXTRA_TEMP, 0)
                
                sendAllCommands(depth, extend, retract, strength, temp)
            }
            
            ACTION_START, ACTION_THRUST -> {
                val depth = intent.getIntExtra(EXTRA_DEPTH, 36)
                val extend = intent.getIntExtra(EXTRA_EXTEND, 8)
                val retract = intent.getIntExtra(EXTRA_RETRACT, 8)
                
                val params = aya.strokenet.data.model.ControlParams(
                    depth = depth,
                    extendSpeed = extend,
                    retractSpeed = retract
                )
                
                val uuid = aya.strokenet.ble.DaxiuCommand.buildControlCommand(params, this)
                sendCommand(uuid, "推拉: 深度=$depth 伸=$extend 缩=$retract")
            }
            
            ACTION_STRENGTH -> {
                val value = intent.getIntExtra(EXTRA_VALUE, 50)
                val uuid = aya.strokenet.ble.DaxiuCommand.buildStrengthCommand(value, this)
                sendCommand(uuid, "强度: $value")
            }
            
            ACTION_TEMP -> {
                val value = intent.getIntExtra(EXTRA_VALUE, 30)
                val uuid = aya.strokenet.ble.DaxiuCommand.buildTemperatureCommand(value, this)
                sendCommand(uuid, "温度: $value")
            }
            
            ACTION_STOP -> {
                val uuid = aya.strokenet.ble.DaxiuCommand.buildStopCommand(this)
                sendStopCommand(uuid, "停止伸缩")
            }
            
            ACTION_STOP_THRUST -> {
                val uuid = aya.strokenet.ble.DaxiuCommand.buildStopCommand(this)
                sendStopCommand(uuid, "停止伸缩")
            }
            
            ACTION_STOP_STRENGTH -> {
                val uuid = aya.strokenet.ble.DaxiuCommand.buildStopStrengthCommand(this)
                sendStopCommand(uuid, "停止震动")
            }
            
            ACTION_STOP_ALL -> {
                val uuid = aya.strokenet.ble.DaxiuCommand.buildStopAllCommand(this)
                sendStopCommand(uuid, "全部停止")
            }
        }
    }
    
    /**
     * 批量发送所有参数（推拉+震动+温度）
     * 使用独立广播器，互不干扰
     */
    private fun sendAllCommands(depth: Int, extend: Int, retract: Int, strength: Int, temp: Int) {
        val summary = buildString {
            append("深度:$depth 伸:$extend 缩:$retract 强度:$strength")
        }
        
        updateNotification("发送中: $summary")
        
        // 广播设备激活状态
        sendBroadcast(Intent(BROADCAST_DEVICE_ACTIVE))
        
        try {
            // 1. 立即发送推拉命令（使用推拉广播器）
            val params = aya.strokenet.data.model.ControlParams(
                depth = depth,
                extendSpeed = extend,
                retractSpeed = retract
            )
            val thrustUuid = aya.strokenet.ble.DaxiuCommand.buildControlCommand(params, this)
            bleAdvertiser.sendThrustCommand(thrustUuid)
            Log.d(TAG, "Thrust command sent: $thrustUuid")
            
            // 2. 立即发送震动强度（使用震动广播器，不会影响推拉）
            val strengthUuid = aya.strokenet.ble.DaxiuCommand.buildStrengthCommand(strength, this)
            bleAdvertiser.sendStrengthCommand(strengthUuid)
            Log.d(TAG, "Strength command sent: $strengthUuid")
            
            // 3. 如果有温度，发送温度命令（使用温度广播器）
            if (temp > 0) {
                val tempUuid = aya.strokenet.ble.DaxiuCommand.buildTemperatureCommand(temp, this)
                bleAdvertiser.sendTemperatureCommand(tempUuid)
                Log.d(TAG, "Temperature command sent: $tempUuid")
            }
            
            // 延迟更新通知为成功
            handler.postDelayed({
                updateNotification("✓ 已发送: $summary")
            }, 500)
            
            // 延迟停止服务
            handler.postDelayed({ stopSelf() }, 2000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Batch command failed: ${e.message}", e)
            updateNotification("✗ 发送失败: $summary")
            handler.postDelayed({ stopSelf() }, 3000)
        }
    }
    
    /**
     * 发送停止命令（持续2秒确保设备收到）
     */
    private fun sendStopCommand(uuid: String, description: String) {
        updateNotification("🚨 $description...")
        
        // 广播设备停止状态
        sendBroadcast(Intent(BROADCAST_DEVICE_STOPPED))
        
        try {
            Log.d(TAG, "========== 发送停止命令 ==========")
            Log.d(TAG, "停止命令: $uuid")
            Log.d(TAG, "描述: $description")
            
            // 先停止所有正在运行的广播
            bleAdvertiser.stopCurrentBroadcast()
            Log.d(TAG, "✓ 已停止所有广播")
            
            // 短暂延迟后发送停止命令
            handler.postDelayed({
                bleAdvertiser.startSingleBroadcast(uuid)
                Log.d(TAG, "✓ 停止命令已发送")
                
                // 持续2秒，确保设备收到
                handler.postDelayed({
                    updateNotification("✓ $description 完成")
                    Log.d(TAG, "持续2秒后停止广播")
                    
                    // 再次停止广播
                    bleAdvertiser.stopCurrentBroadcast()
                    Log.d(TAG, "========================================")
                    
                    // 延迟停止服务
                    handler.postDelayed({ stopSelf() }, 1000)
                }, 2000)
            }, 100)
            
        } catch (e: Exception) {
            Log.e(TAG, "停止命令失败: ${e.message}", e)
            updateNotification("✗ $description 失败")
            handler.postDelayed({ stopSelf() }, 3000)
        }
    }
    
    /**
     * 发送 BLE 命令
     */
    private fun sendCommand(uuid: String, description: String) {
        updateNotification("发送中: $description")
        
        // 广播设备激活状态
        sendBroadcast(Intent(BROADCAST_DEVICE_ACTIVE))
        
        try {
            bleAdvertiser.startSingleBroadcast(uuid)
            Log.d(TAG, "Command sent: $uuid")
            
            // 不立即更新为成功，等待一小段时间让多个命令都能显示
            handler.postDelayed({
                updateNotification("✓ 已发送: $description")
            }, 300)
            
            // 延迟停止服务（给足够时间显示通知）
            handler.postDelayed({ stopSelf() }, 3000)
            
        } catch (e: Exception) {
            Log.e(TAG, "Command failed: ${e.message}", e)
            updateNotification("✗ 发送失败: $description")
            handler.postDelayed({ stopSelf() }, 3000)
        }
    }
    
    /**
     * 构建完整状态文本
     */
    private fun buildFullStatusText(): String {
        val status = if (isRunning) "运行中" else "待机"
        return "$status | 深度:$currentDepth 伸:$currentExtend 缩:$currentRetract | 强度:$currentStrength 温度:${currentTemp}°C"
    }
    
    /**
     * 创建通知渠道（Android 8.0+）
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "BLE 控制服务",
                NotificationManager.IMPORTANCE_DEFAULT // 改为 DEFAULT 以确保通知显示
            ).apply {
                description = "发送 BLE 控制指令"
                setShowBadge(true)
                enableLights(true)
                enableVibration(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }
    
    /**
     * 构建通知
     */
    private fun buildNotification(contentText: String = "服务运行中"): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("StrokeNet 控制")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth) // 使用蓝牙图标
            .setContentIntent(pendingIntent)
            .setOngoing(true) // 前台服务期间不可滑动删除
            .setPriority(NotificationCompat.PRIORITY_DEFAULT) // 改为 DEFAULT
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }
    
    /**
     * 更新通知内容
     */
    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        bleAdvertiser.stopCurrentBroadcast()
        handler.removeCallbacksAndMessages(null)
        isServiceActive = false
        Log.d(TAG, "Service destroyed")
    }
}
