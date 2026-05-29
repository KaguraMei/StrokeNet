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

/**
 * 前台服务：发送 BLE 指令并重试（失败时最多重试2次）
 * 不做持续循环，只确保指令送达
 */
class BleService : Service() {
    
    companion object {
        private const val TAG = "BleService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "ble_service_channel"
        private const val MAX_RETRY = 2 // 失败时最多重试2次
        private const val RETRY_DELAY_MS = 300L // 重试间隔 300ms
        
        // Service Actions
        const val ACTION_START = "start"
        const val ACTION_THRUST = "thrust"
        const val ACTION_STRENGTH = "strength"
        const val ACTION_TEMP = "temp"
        const val ACTION_STOP = "stop"
        
        // Intent Extras
        const val EXTRA_DEPTH = "depth"
        const val EXTRA_EXTEND = "extend"
        const val EXTRA_RETRACT = "retract"
        const val EXTRA_VALUE = "value"
    }
    
    private lateinit var bleAdvertiser: BleAdvertiser
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
        bleAdvertiser = BleAdvertiser(this)
        createNotificationChannel()
        Log.d(TAG, "Service created")
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
        
        // 特殊处理停止指令
        if (action == ACTION_STOP) {
            isRunning = false
            sendWithRetry(
                action = action,
                description = "停止",
                shouldStopService = true // 停止后关闭服务
            )
            return
        }
        
        when (action) {
            ACTION_START, ACTION_THRUST -> {
                val depth = intent.getIntExtra(EXTRA_DEPTH, 36)
                val extend = intent.getIntExtra(EXTRA_EXTEND, 8)
                val retract = intent.getIntExtra(EXTRA_RETRACT, 8)
                
                // 更新状态
                currentDepth = depth
                currentExtend = extend
                currentRetract = retract
                if (action == ACTION_START) {
                    isRunning = true
                }
                
                sendWithRetry(
                    action = action,
                    depth = depth,
                    extendSpeed = extend,
                    retractSpeed = retract,
                    description = if (action == ACTION_START) "启动设备" else "调节推拉"
                )
            }
            
            ACTION_STRENGTH -> {
                val value = intent.getIntExtra(EXTRA_VALUE, 50)
                currentStrength = value
                
                sendWithRetry(
                    action = action,
                    strength = value,
                    description = "调节强度"
                )
            }
            
            ACTION_TEMP -> {
                val value = intent.getIntExtra(EXTRA_VALUE, 30)
                currentTemp = value
                
                sendWithRetry(
                    action = action,
                    temp = value,
                    description = "调节温度"
                )
            }
        }
    }
    
    /**
     * 发送 BLE 指令，失败时重试最多2次
     */
    private fun sendWithRetry(
        action: String,
        depth: Int = 0,
        extendSpeed: Int = 0,
        retractSpeed: Int = 0,
        strength: Int = 0,
        temp: Int = 0,
        description: String,
        retryCount: Int = 0,
        shouldStopService: Boolean = false // 是否在完成后停止服务
    ) {
        updateNotification("⏳ 发送中: $description")
        
        bleAdvertiser.advertise(
            action = action,
            depth = depth,
            extendSpeed = extendSpeed,
            retractSpeed = retractSpeed,
            strength = strength,
            temp = temp,
            onSuccess = {
                Log.d(TAG, "Command sent successfully: $action")
                
                // 成功后显示完整状态
                if (shouldStopService) {
                    updateNotification("✓ 已停止")
                    handler.postDelayed({ 
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf() 
                    }, 2000)
                } else {
                    updateNotification(buildFullStatusText())
                }
            },
            onFailure = { errorCode ->
                if (retryCount < MAX_RETRY) {
                    Log.w(TAG, "Command failed (attempt ${retryCount + 1}/$MAX_RETRY), retrying...")
                    updateNotification("🔄 重试中 (${retryCount + 1}/$MAX_RETRY): $description")
                    
                    // 延迟后重试
                    handler.postDelayed({
                        sendWithRetry(
                            action, depth, extendSpeed, retractSpeed,
                            strength, temp, description, retryCount + 1, shouldStopService
                        )
                    }, RETRY_DELAY_MS)
                } else {
                    Log.e(TAG, "Command failed after $MAX_RETRY retries: $errorCode")
                    updateNotification("✗ 失败: $description (错误: $errorCode)")
                    
                    // 失败后也保持服务运行，除非是停止指令
                    if (shouldStopService) {
                        handler.postDelayed({ 
                            stopForeground(STOP_FOREGROUND_REMOVE)
                            stopSelf() 
                        }, 3000)
                    }
                }
            }
        )
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
        bleAdvertiser.stopAdvertising()
        handler.removeCallbacksAndMessages(null)
        isServiceActive = false
        Log.d(TAG, "Service destroyed")
    }
}
