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
    
    override fun onCreate() {
        super.onCreate()
        bleAdvertiser = BleAdvertiser(this)
        createNotificationChannel()
        Log.d(TAG, "Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 启动前台服务
        startForeground(NOTIFICATION_ID, buildNotification("准备发送指令..."))
        
        intent?.let { handleIntent(it) }
        
        return START_NOT_STICKY // 任务完成后不需要重启
    }
    
    private fun handleIntent(intent: Intent) {
        val action = intent.getStringExtra("action") ?: return
        
        Log.d(TAG, "Received action: $action")
        
        when (action) {
            ACTION_START, ACTION_THRUST -> {
                val depth = intent.getIntExtra(EXTRA_DEPTH, 36)
                val extend = intent.getIntExtra(EXTRA_EXTEND, 8)
                val retract = intent.getIntExtra(EXTRA_RETRACT, 8)
                
                sendWithRetry(
                    action = action,
                    depth = depth,
                    extendSpeed = extend,
                    retractSpeed = retract,
                    description = "推拉: 深度=$depth 伸=$extend 缩=$retract"
                )
            }
            
            ACTION_STRENGTH -> {
                val value = intent.getIntExtra(EXTRA_VALUE, 50)
                sendWithRetry(
                    action = action,
                    strength = value,
                    description = "强度: $value"
                )
            }
            
            ACTION_TEMP -> {
                val value = intent.getIntExtra(EXTRA_VALUE, 30)
                sendWithRetry(
                    action = action,
                    temp = value,
                    description = "温度: $value"
                )
            }
            
            ACTION_STOP -> {
                sendWithRetry(
                    action = action,
                    description = "停止"
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
        retryCount: Int = 0
    ) {
        updateNotification("发送中: $description")
        
        bleAdvertiser.advertise(
            action = action,
            depth = depth,
            extendSpeed = extendSpeed,
            retractSpeed = retractSpeed,
            strength = strength,
            temp = temp,
            onSuccess = {
                Log.d(TAG, "Command sent successfully: $action")
                updateNotification("✓ 已发送: $description")
                // 成功后延迟停止服务
                handler.postDelayed({ stopSelf() }, 2000)
            },
            onFailure = { errorCode ->
                if (retryCount < MAX_RETRY) {
                    Log.w(TAG, "Command failed (attempt ${retryCount + 1}/$MAX_RETRY), retrying...")
                    updateNotification("重试中 (${retryCount + 1}/$MAX_RETRY): $description")
                    
                    // 延迟后重试
                    handler.postDelayed({
                        sendWithRetry(
                            action, depth, extendSpeed, retractSpeed,
                            strength, temp, description, retryCount + 1
                        )
                    }, RETRY_DELAY_MS)
                } else {
                    Log.e(TAG, "Command failed after $MAX_RETRY retries: $errorCode")
                    updateNotification("✗ 发送失败: $description (错误码: $errorCode)")
                    handler.postDelayed({ stopSelf() }, 3000)
                }
            }
        )
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
        Log.d(TAG, "Service destroyed")
    }
}
