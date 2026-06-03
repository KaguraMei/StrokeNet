package aya.strokenet

import android.app.*
import android.content.Context
import android.content.Intent
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import aya.strokenet.mcp.createStrokeNetMcpServer
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Inet4Address
import java.net.NetworkInterface

/**
 * MCP Server 前台服务
 * 使用官方 Kotlin MCP SDK + Ktor
 */
class McpServerService : Service() {
    
    companion object {
        private const val TAG = "McpServerService"
        private const val NOTIFICATION_ID = 1002
        private const val CHANNEL_ID = "mcp_server_channel"
        const val DEFAULT_PORT = 8080
        
        const val ACTION_START_LOCAL = "start_local"
        const val ACTION_STOP = "stop"
        const val ACTION_QUERY_STATUS = "query_status"
        const val ACTION_NOTIFICATION_DELETED = "notification_deleted"
        
        const val EXTRA_PORT = "port"
        
        // Broadcast Actions
        const val BROADCAST_STATUS_UPDATE = "aya.strokenet.MCP_STATUS_UPDATE"
        const val EXTRA_STATUS = "status"
        const val EXTRA_LOCAL_URL = "local_url"
        const val EXTRA_ERROR = "error"
    }
    
    private var ktorServer: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private var currentPort = DEFAULT_PORT
    private var localUrl: String? = null
    private var currentStatus = "stopped"
    private var currentError: String? = null
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "MCP Server Service created")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        
        Log.d(TAG, "onStartCommand called with action: $action, current status: $currentStatus")
        
        // 如果服务重启（intent 为 null），恢复前台服务
        if (intent == null) {
            Log.d(TAG, "Service restarted by system, restoring foreground state")
            restoreForegroundService()
            return START_STICKY
        }
        
        when (action) {
            ACTION_QUERY_STATUS -> {
                // 仅查询状态，不改变前台服务状态
                // 如果服务已经在运行，保持前台状态；如果是stopped状态，不主动进入前台
                if (currentStatus != "stopped") {
                    val notification = buildNotification(getNotificationText())
                    startForeground(NOTIFICATION_ID, notification)
                }
                handleIntent(intent)
            }
            ACTION_START_LOCAL -> {
                // 启动服务时进入前台模式
                val initialNotification = buildNotification("MCP 服务正在启动...")
                startForeground(NOTIFICATION_ID, initialNotification)
                handleIntent(intent)
            }
            ACTION_STOP -> {
                handleIntent(intent)
            }
            ACTION_NOTIFICATION_DELETED -> {
                Log.w(TAG, "Notification was deleted by user, recreating...")
                restoreForegroundService()
            }
            else -> {
                if (currentStatus != "stopped") {
                    restoreForegroundService()
                }
            }
        }
        
        return START_STICKY
    }
    
    /**
     * 获取当前状态对应的通知文本
     */
    private fun getNotificationText(): String {
        return when (currentStatus) {
            "starting" -> "MCP 服务正在启动..."
            "running_local" -> {
                localUrl?.let { "✓ 服务运行中\n$it" } ?: "MCP 服务运行中"
            }
            "stopping" -> "正在停止服务..."
            "error" -> "✗ 服务错误\n${currentError ?: "未知错误"}"
            else -> "MCP 服务就绪"
        }
    }
    
    /**
     * 恢复前台服务（在服务重启或通知被删除时调用）
     */
    private fun restoreForegroundService() {
        val notificationText = getNotificationText()
        val notification = buildNotification(notificationText)
        startForeground(NOTIFICATION_ID, notification)
        
        Log.d(TAG, "Foreground service restored with status: $currentStatus")
    }
    
    private fun handleIntent(intent: Intent) {
        when (intent.action) {
            ACTION_START_LOCAL -> {
                broadcastStatus(status = "starting")
                val port = intent.getIntExtra(EXTRA_PORT, DEFAULT_PORT)
                startLocalServer(port)
            }
            ACTION_STOP -> {
                stopServer()
            }
            ACTION_QUERY_STATUS -> {
                Log.d(TAG, "Query status request received, current status: $currentStatus")
                broadcastStatus(
                    status = currentStatus,
                    localUrl = localUrl,
                    error = currentError
                )
            }
        }
    }
    
    /**
     * 启动本地局域网服务
     */
    private fun startLocalServer(port: Int) {
        serviceScope.launch {
            try {
                currentPort = port
                currentStatus = "starting"
                currentError = null
                
                updateNotification("正在启动 MCP 服务...")
                broadcastStatus(status = "starting")
                
                startKtorServer(port)
                
                val localIp = getLocalIpAddress()
                localUrl = "http://$localIp:$port/mcp"
                
                updateNotification("✓ 服务运行中\n$localUrl")
                broadcastStatus(
                    status = "running_local",
                    localUrl = localUrl
                )
                
                Log.d(TAG, "Local server started: $localUrl")
                
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start local server", e)
                val errorMsg = e.message ?: "未知错误"
                localUrl = null
                currentStatus = "error"
                currentError = errorMsg
                updateNotification("✗ 启动失败\n$errorMsg")
                broadcastStatus(
                    status = "error",
                    error = errorMsg
                )
            }
        }
    }
    
    /**
     * 启动 Ktor HTTP Server (使用官方 MCP SDK)
     */
    private suspend fun startKtorServer(port: Int) {
        try {
            // 如果已有实例，先关闭
            ktorServer?.let {
                Log.d(TAG, "Stopping existing Ktor server...")
                it.stop(500, 500)
                ktorServer = null
            }
            
            // 创建 MCP Server 实例
            val mcpServer = createStrokeNetMcpServer(this)
            
            ktorServer = embeddedServer(Netty, port = port) {
                // 安装 CORS (支持浏览器客户端如 MCP Inspector)
                install(CORS) {
                    anyHost()
                    allowMethod(HttpMethod.Options)
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Post)
                    allowMethod(HttpMethod.Delete)
                    allowNonSimpleContentTypes = true
                    allowHeader("Mcp-Session-Id")
                    allowHeader("Mcp-Protocol-Version")
                    exposeHeader("Mcp-Session-Id")
                    exposeHeader("Mcp-Protocol-Version")
                }
                
                routing {
                    // 健康检查端点
                    get("/") {
                        call.respondText("StrokeNet MCP Server Running", ContentType.Text.Plain)
                    }
                    
                    // 使用 SDK 的 Streamable HTTP 扩展挂载 MCP
                    mcpStreamableHttp(path = "/mcp") {
                        mcpServer
                    }
                }
            }.start(wait = false)
            
            Log.d(TAG, "Ktor server started on port $port with MCP SDK")
            
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("Address already in use") == true -> 
                    "端口 $port 已被占用，请稍后重试"
                e.message?.contains("bind") == true -> 
                    "无法绑定端口 $port"
                else -> "Ktor 启动失败: ${e.message}"
            }
            throw Exception(errorMsg, e)
        }
    }
    
    /**
     * 获取本地 IP 地址
     */
    private fun getLocalIpAddress(): String {
        try {
            // 优先使用 WiFi IP
            val wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            val wifiInfo = wifiManager.connectionInfo
            val ipAddress = wifiInfo.ipAddress
            
            if (ipAddress != 0) {
                return String.format(
                    "%d.%d.%d.%d",
                    ipAddress and 0xff,
                    (ipAddress shr 8) and 0xff,
                    (ipAddress shr 16) and 0xff,
                    (ipAddress shr 24) and 0xff
                )
            }
            
            // 回退到网络接口枚举
            NetworkInterface.getNetworkInterfaces()?.let { interfaces ->
                for (networkInterface in interfaces) {
                    networkInterface.inetAddresses?.let { addresses ->
                        for (address in addresses) {
                            if (!address.isLoopbackAddress && address is Inet4Address) {
                                return address.hostAddress ?: "127.0.0.1"
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get IP: ${e.message}", e)
        }
        
        return "127.0.0.1"
    }
    
    /**
     * 停止服务器
     */
    private fun stopServer() {
        serviceScope.launch {
            try {
                currentStatus = "stopping"
                currentError = null
                updateNotification("正在停止服务...")
                broadcastStatus(status = "stopping")
                
                Log.d(TAG, "Stopping server...")
                
                // 停止 Ktor 服务器
                ktorServer?.let {
                    Log.d(TAG, "Stopping Ktor server...")
                    withContext(Dispatchers.IO) {
                        try {
                            it.stop(1000, 2000)
                        } catch (e: Exception) {
                            Log.w(TAG, "Error stopping Ktor: ${e.message}")
                        }
                    }
                }
                ktorServer = null
                
                // 清理状态
                localUrl = null
                currentStatus = "stopped"
                currentError = null
                
                // 广播停止状态
                broadcastStatus(status = "stopped")
                
                Log.d(TAG, "Server stopped successfully")
                
                // 延迟关闭服务，确保广播已发送和UI已更新
                delay(500)
                
                // 停止前台服务并移除通知
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping server", e)
                // 即使出错也要清理状态
                currentStatus = "stopped"
                currentError = null
                localUrl = null
                broadcastStatus(status = "stopped")
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    stopForeground(STOP_FOREGROUND_REMOVE)
                } else {
                    @Suppress("DEPRECATION")
                    stopForeground(true)
                }
                stopSelf()
            }
        }
    }
    
    /**
     * 广播状态更新
     */
    private fun broadcastStatus(
        status: String,
        localUrl: String? = null,
        error: String? = null
    ) {
        // 更新内部状态
        currentStatus = status
        currentError = error
        
        val intent = Intent(BROADCAST_STATUS_UPDATE).apply {
            setPackage(packageName)
            putExtra(EXTRA_STATUS, status)
            localUrl?.let { putExtra(EXTRA_LOCAL_URL, it) }
            error?.let { putExtra(EXTRA_ERROR, it) }
        }
        sendBroadcast(intent)
        Log.d(TAG, "Broadcast sent: status=$status, localUrl=$localUrl, error=$error")
    }
    
    /**
     * 创建通知渠道
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "MCP Server 前台服务",
                NotificationManager.IMPORTANCE_LOW  // 使用 LOW 而不是 DEFAULT，减少打扰
            ).apply {
                description = "显示 MCP Server 运行状态，此通知无法手动关闭"
                setShowBadge(true)
                enableLights(false)  // 关闭指示灯
                enableVibration(false)  // 关闭振动
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                // 不设置 setBypassDnd，避免免打扰模式问题
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            
            Log.d(TAG, "Notification channel created: $CHANNEL_ID")
        }
    }
    
    /**
     * 构建通知
     */
    private fun buildNotification(contentText: String): Notification {
        // 创建点击通知后打开应用的 Intent
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // 创建停止服务的 Action
        val stopIntent = Intent(this, McpServerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        // 创建通知删除时的 PendingIntent（用于监听通知被删除）
        val deleteIntent = Intent(this, McpServerService::class.java).apply {
            action = ACTION_NOTIFICATION_DELETED
        }
        val deletePendingIntent = PendingIntent.getService(
            this,
            2,
            deleteIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MCP Server")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deletePendingIntent)  // ✅ 监听通知删除
            .setOngoing(true)  // 设置为持续通知，不可滑动删除
            .setAutoCancel(false)  // 点击后不自动消失
            .setPriority(NotificationCompat.PRIORITY_LOW)  // 低优先级，减少打扰
            .setStyle(NotificationCompat.BigTextStyle().bigText(contentText))
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(
                android.R.drawable.ic_media_pause,
                "停止",
                stopPendingIntent
            )
            .build()
    }
    
    /**
     * 更新通知
     */
    private fun updateNotification(contentText: String) {
        val notification = buildNotification(contentText)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
    
    override fun onBind(intent: Intent?): IBinder? = null
    
    override fun onDestroy() {
        super.onDestroy()
        
        Log.d(TAG, "Service onDestroy called, cleaning up...")
        
        // 确保发送停止状态
        if (currentStatus != "stopped") {
            currentStatus = "stopped"
            localUrl = null
            currentError = null
            broadcastStatus(status = "stopped")
        }
        
        // 清理资源
        try {
            ktorServer?.stop(500, 500)
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping Ktor in onDestroy: ${e.message}")
        }
        
        serviceScope.cancel()
        Log.d(TAG, "Service destroyed")
    }
}
