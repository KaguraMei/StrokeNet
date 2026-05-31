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
 * 加热定时器前台服务
 * 独立通知显示倒计时，不干扰BLE命令通知
 */
class HeatingTimerService : Service() {
    
    companion object {
        private const val TAG = "HeatingTimerService"
        private const val NOTIFICATION_ID = 1002  // 不同于BleService的1001
        private const val CHANNEL_ID = "heating_timer_channel"
        
        const val ACTION_START_TIMER = "start_timer"
        const val ACTION_STOP_TIMER = "stop_timer"
        const val EXTRA_DURATION_MINUTES = "duration_minutes"
        const val EXTRA_TEMPERATURE = "temperature"
        
        // 广播Action
        const val BROADCAST_TIMER_STOPPED = "aya.strokenet.HEATING_TIMER_STOPPED"
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private var timeLeftSeconds = 0L
    private var temperature = 0
    private var isRunning = false
    
    private val timerRunnable = object : Runnable {
        override fun run() {
            if (isRunning && timeLeftSeconds > 0) {
                timeLeftSeconds--
                updateNotification()
                handler.postDelayed(this, 1000)
                
                // 时间到了
                if (timeLeftSeconds <= 0) {
                    stopHeating()
                }
            }
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "Heating timer service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("准备加热..."))
        
        intent?.let { handleIntent(it) }
        
        return START_NOT_STICKY
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.getStringExtra("action")) {
            ACTION_START_TIMER -> {
                val durationMinutes = intent.getIntExtra(EXTRA_DURATION_MINUTES, 0)
                temperature = intent.getIntExtra(EXTRA_TEMPERATURE, 0)
                
                if (durationMinutes > 0 && temperature > 0) {
                    startTimer(durationMinutes)
                } else {
                    Log.w(TAG, "Invalid timer parameters: duration=$durationMinutes, temp=$temperature")
                    stopSelf()
                }
            }
            ACTION_STOP_TIMER -> {
                stopTimer()
            }
        }
    }
    
    private fun startTimer(durationMinutes: Int) {
        timeLeftSeconds = (durationMinutes * 60).toLong()
        isRunning = true
        
        Log.d(TAG, "Timer started: ${durationMinutes}min at ${temperature}°C")
        updateNotification()
        handler.post(timerRunnable)
    }
    
    private fun stopTimer() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)
        
        // 发送广播通知UI
        sendBroadcast(Intent(BROADCAST_TIMER_STOPPED))
        
        Log.d(TAG, "Timer stopped manually")
        stopSelf()
    }
    
    private fun stopHeating() {
        isRunning = false
        handler.removeCallbacks(timerRunnable)
        
        // 发送温度0命令关闭加热
        sendTemperatureCommand(0)
        
        // 发送广播通知UI
        sendBroadcast(Intent(BROADCAST_TIMER_STOPPED))
        
        // 显示完成通知
        val notification = buildNotification("⏰ 加热完成，已自动关闭")
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
        
        Log.d(TAG, "Heating completed")
        
        // 延迟停止服务
        handler.postDelayed({ stopSelf() }, 3000)
    }
    
    private fun sendTemperatureCommand(temp: Int) {
        val intent = Intent(this, BleService::class.java).apply {
            putExtra("action", BleService.ACTION_TEMP)
            putExtra(BleService.EXTRA_VALUE, temp)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "加热定时器",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "显示加热倒计时"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun buildNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        // 停止按钮
        val stopIntent = Intent(this, HeatingTimerService::class.java).apply {
            putExtra("action", ACTION_STOP_TIMER)
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🔥 加热中 ${temperature}°C")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)  // 不可滑动删除
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)  // 不显示时间戳
        
        // 如果正在运行，添加停止按钮
        if (isRunning) {
            builder.addAction(
                android.R.drawable.ic_delete,
                "停止",
                stopPendingIntent
            )
        }
        
        return builder.build()
    }
    
    private fun updateNotification() {
        val minutes = timeLeftSeconds / 60
        val seconds = timeLeftSeconds % 60
        val timeText = String.format("⏱ 剩余 %02d:%02d", minutes, seconds)
        
        val notification = buildNotification(timeText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(timerRunnable)
        Log.d(TAG, "Service destroyed")
    }
}
