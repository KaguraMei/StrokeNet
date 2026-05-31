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
import aya.strokenet.data.model.LoopPreset
import aya.strokenet.data.model.PresetCommand
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 循环预设播放服务
 * 按照预设的命令列表循环发送BLE命令
 */
class LoopPresetService : Service() {
    
    companion object {
        private const val TAG = "LoopPresetService"
        private const val NOTIFICATION_ID = 1003
        private const val CHANNEL_ID = "loop_preset_channel"
        
        const val ACTION_START_LOOP = "start_loop"
        const val ACTION_STOP_LOOP = "stop_loop"
        const val EXTRA_PRESET_JSON = "preset_json"
        
        // 广播Action
        const val BROADCAST_LOOP_STOPPED = "aya.strokenet.LOOP_PRESET_STOPPED"
    }
    
    private lateinit var bleAdvertiser: DaxiuBleAdvertiser
    private val handler = Handler(Looper.getMainLooper())
    private var currentPreset: LoopPreset? = null
    private var currentIndex = 0
    private var isLooping = false
    
    private val loopRunnable = object : Runnable {
        override fun run() {
            if (!isLooping || currentPreset == null) return
            
            val preset = currentPreset!!
            val command = preset.commands[currentIndex]
            
            // 发送当前命令（需要替换占位符并添加校验和）
            try {
                // 使用buildBleCommand处理占位符和校验和
                val finalCommand = aya.strokenet.utils.buildBleCommand(command.command, this@LoopPresetService)
                bleAdvertiser.startSingleBroadcast(finalCommand)
                Log.d(TAG, "Loop command sent: $finalCommand (original: ${command.command}), delay: ${command.time}ms")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send loop command", e)
            }
            
            // 移动到下一个命令
            currentIndex = (currentIndex + 1) % preset.commands.size
            
            // 更新通知
            updateNotification()
            
            // 调度下一个命令
            handler.postDelayed(this, command.time.toLong())
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        bleAdvertiser = DaxiuBleAdvertiser(this)
        createNotificationChannel()
        Log.d(TAG, "Loop preset service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, buildNotification("准备播放..."))
        
        intent?.let { handleIntent(it) }
        
        return START_NOT_STICKY
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.getStringExtra("action")) {
            ACTION_START_LOOP -> {
                val presetJson = intent.getStringExtra(EXTRA_PRESET_JSON)
                if (presetJson != null) {
                    try {
                        val preset = Json.decodeFromString<LoopPreset>(presetJson)
                        startLoop(preset)
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to parse preset JSON", e)
                        stopSelf()
                    }
                } else {
                    stopSelf()
                }
            }
            ACTION_STOP_LOOP -> {
                stopLoop()
            }
        }
    }
    
    private fun startLoop(preset: LoopPreset) {
        if (preset.commands.isEmpty()) {
            Log.w(TAG, "Preset has no commands")
            stopSelf()
            return
        }
        
        currentPreset = preset
        currentIndex = 0
        isLooping = true
        
        Log.d(TAG, "Loop started: ${preset.name}, ${preset.commands.size} commands")
        updateNotification()
        handler.post(loopRunnable)
    }
    
    private fun stopLoop() {
        isLooping = false
        handler.removeCallbacks(loopRunnable)
        
        // 发送广播通知UI
        sendBroadcast(Intent(BROADCAST_LOOP_STOPPED))
        
        Log.d(TAG, "Loop stopped")
        stopSelf()
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "循环预设播放",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "循环播放预设模式"
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
        val stopIntent = Intent(this, LoopPresetService::class.java).apply {
            putExtra("action", ACTION_STOP_LOOP)
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            0,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val title = currentPreset?.let { "🔄 ${it.name}" } ?: "循环播放"
        
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
        
        if (isLooping) {
            builder.addAction(
                android.R.drawable.ic_media_pause,
                "停止",
                stopPendingIntent
            )
        }
        
        return builder.build()
    }
    
    private fun updateNotification() {
        val preset = currentPreset ?: return
        val commandNum = "${currentIndex + 1}/${preset.commands.size}"
        val notification = buildNotification("正在播放第 $commandNum 个命令")
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(loopRunnable)
        bleAdvertiser.stopCurrentBroadcast()
        Log.d(TAG, "Service destroyed")
    }
}
