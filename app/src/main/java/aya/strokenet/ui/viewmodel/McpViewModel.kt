package aya.strokenet.ui.viewmodel

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import aya.strokenet.McpServerService

/**
 * MCP Server ViewModel
 */
class McpViewModel : ViewModel() {
    
    var serverStatus by mutableStateOf<ServerStatus>(ServerStatus.Stopped)
        private set
    
    var localUrl by mutableStateOf<String?>(null)
        private set
    
    var errorMessage by mutableStateOf<String?>(null)
        private set
    
    private var receiver: BroadcastReceiver? = null
    
    /**
     * 注册广播接收器
     */
    fun registerReceiver(context: Context) {
        // 如果已经注册过，先注销
        unregisterReceiver(context)
        
        receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val status = intent?.getStringExtra(McpServerService.EXTRA_STATUS)
                val localUrlExtra = intent?.getStringExtra(McpServerService.EXTRA_LOCAL_URL)
                val errorExtra = intent?.getStringExtra(McpServerService.EXTRA_ERROR)
                
                android.util.Log.d("McpViewModel", "Received broadcast: action=${intent?.action}, status=$status, localUrl=$localUrlExtra")
                
                when (status) {
                    "starting" -> {
                        serverStatus = ServerStatus.Starting
                        if (localUrlExtra != null) {
                            localUrl = localUrlExtra
                        }
                        errorMessage = null
                        android.util.Log.d("McpViewModel", "Updated to Starting, localUrl=$localUrl")
                    }
                    "running_local" -> {
                        serverStatus = ServerStatus.RunningLocal
                        localUrl = localUrlExtra
                        errorMessage = null
                        android.util.Log.d("McpViewModel", "Updated to RunningLocal: localUrl=$localUrl")
                    }
                    "stopped" -> {
                        serverStatus = ServerStatus.Stopped
                        localUrl = null
                        errorMessage = null
                        android.util.Log.d("McpViewModel", "Updated to Stopped")
                    }
                    "stopping" -> {
                        serverStatus = ServerStatus.Stopping
                        android.util.Log.d("McpViewModel", "Updated to Stopping")
                    }
                    "error" -> {
                        serverStatus = ServerStatus.Error
                        errorMessage = errorExtra
                        android.util.Log.d("McpViewModel", "Updated to Error: $errorMessage")
                    }
                    else -> {
                        android.util.Log.w("McpViewModel", "Unknown status received: $status")
                    }
                }
            }
        }
        
        val filter = IntentFilter(McpServerService.BROADCAST_STATUS_UPDATE)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("UnspecifiedRegisterReceiverFlag")
            context.registerReceiver(receiver, filter)
        }
        
        android.util.Log.d("McpViewModel", "Broadcast receiver registered with filter: ${McpServerService.BROADCAST_STATUS_UPDATE}")
        
        // 注册后延迟查询状态，确保接收器已完全注册
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            android.util.Log.d("McpViewModel", "Querying current status...")
            queryCurrentStatus(context)
        }, 200)
    }
    
    /**
     * 查询当前服务状态
     */
    fun queryCurrentStatus(context: Context) {
        val intent = Intent(context, McpServerService::class.java).apply {
            action = McpServerService.ACTION_QUERY_STATUS
        }
        // 仅查询状态，使用 startService 而不是 startForegroundService
        context.startService(intent)
        android.util.Log.d("McpViewModel", "Query status sent")
    }
    
    /**
     * 注销广播接收器
     */
    fun unregisterReceiver(context: Context) {
        receiver?.let {
            try {
                context.unregisterReceiver(it)
                android.util.Log.d("McpViewModel", "Broadcast receiver unregistered")
            } catch (e: Exception) {
                android.util.Log.w("McpViewModel", "Error unregistering receiver: ${e.message}")
            }
            receiver = null
        }
    }
    
    /**
     * 启动本地服务器
     */
    fun startServer(context: Context, port: Int = McpServerService.DEFAULT_PORT) {
        val intent = Intent(context, McpServerService::class.java).apply {
            action = McpServerService.ACTION_START_LOCAL
            putExtra(McpServerService.EXTRA_PORT, port)
        }
        context.startForegroundService(intent)
        serverStatus = ServerStatus.Starting
    }
    
    /**
     * 停止服务器
     */
    fun stopServer(context: Context) {
        val intent = Intent(context, McpServerService::class.java).apply {
            action = McpServerService.ACTION_STOP
        }
        context.startService(intent)
        serverStatus = ServerStatus.Stopping
    }
    
    override fun onCleared() {
        super.onCleared()
        receiver = null
    }
}

/**
 * 服务器状态
 */
sealed class ServerStatus {
    object Stopped : ServerStatus()
    object Starting : ServerStatus()
    object RunningLocal : ServerStatus()
    object Stopping : ServerStatus()
    object Error : ServerStatus()
}
