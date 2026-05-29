package aya.strokenet

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import aya.strokenet.ui.theme.StrokeNetTheme
import aya.strokenet.ui.components.GlassBottomDock
import aya.strokenet.ui.components.GlassTopBar
import aya.strokenet.ui.theme.iOSBg

class MainActivity : ComponentActivity() {
    
    private lateinit var bleAdvertiser: BleAdvertiser
    
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            Toast.makeText(this, "权限已授予", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "需要蓝牙和位置权限才能使用", Toast.LENGTH_LONG).show()
        }
    }
    
    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            Toast.makeText(this, "蓝牙已启用", Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        try {
            bleAdvertiser = BleAdvertiser(this)
            
            // 检查并请求权限
            checkAndRequestPermissions()
            
            enableEdgeToEdge()
            setContent {
                StrokeNetTheme {
                    StrokeNetApp(
                        bleAdvertiser = bleAdvertiser,
                        onCheckBluetooth = { checkBluetooth() }
                    )
                }
            }
            
            // 处理 Intent 参数
            handleIntent(intent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "初始化失败: ${e.message}", Toast.LENGTH_LONG).show()
            
            // 即使初始化失败也显示 UI
            enableEdgeToEdge()
            setContent {
                StrokeNetTheme {
                    StrokeNetApp(
                        bleAdvertiser = null,
                        onCheckBluetooth = {}
                    )
                }
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val action = it.getStringExtra("action") ?: return
            
            // 启动前台服务来处理所有指令
            val serviceIntent = Intent(this, BleService::class.java).apply {
                putExtra("action", action)
                
                when (action) {
                    "start", "thrust" -> {
                        putExtra(BleService.EXTRA_DEPTH, it.getIntExtra("depth", 36))
                        putExtra(BleService.EXTRA_EXTEND, it.getIntExtra("extend", 8))
                        putExtra(BleService.EXTRA_RETRACT, it.getIntExtra("retract", 8))
                    }
                    "strength", "temp" -> {
                        putExtra(BleService.EXTRA_VALUE, it.getIntExtra("value", 50))
                    }
                }
            }
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
            
            val message = when (action) {
                "start" -> "启动推拉"
                "thrust" -> "调节参数"
                "strength" -> "设置强度"
                "temp" -> "设置温度"
                "stop" -> "停止运行"
                else -> "指令已发送"
            }
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        
        // 蓝牙权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        
        // 通知权限 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        val needRequest = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (needRequest.isNotEmpty()) {
            requestPermissionLauncher.launch(needRequest.toTypedArray())
        }
    }
    
    private fun checkBluetooth() {
        if (!bleAdvertiser.isBluetoothAvailable()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bleAdvertiser.stopAdvertising()
    }
}

@Composable
fun StrokeNetApp(
    bleAdvertiser: BleAdvertiser? = null,
    onCheckBluetooth: () -> Unit = {}
) {
    var currentScreen by rememberSaveable { mutableStateOf("control") }

    // 页面标题映射
    val screenTitle = when(currentScreen) {
        "control" -> "控制中心"
        "presets" -> "预设模式"
        "settings" -> "设置"
        else -> "StrokeNet"
    }

    Scaffold(
        containerColor = iOSBg, // iOS 浅灰背景
        topBar = {
            GlassTopBar(title = screenTitle)
        },
        bottomBar = {
            GlassBottomDock(
                currentRoute = currentScreen,
                onNavigate = { currentScreen = it }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                "control" -> {
                    aya.strokenet.ui.screens.ControlScreen(
                        bleAdvertiser = bleAdvertiser,
                        onCheckBluetooth = onCheckBluetooth
                    )
                }
                "presets" -> {
                    aya.strokenet.ui.screens.PresetsScreen(
                        bleAdvertiser = bleAdvertiser,
                        onCheckBluetooth = onCheckBluetooth
                    )
                }
                "settings" -> {
                    aya.strokenet.ui.screens.SettingsScreen()
                }
            }
        }
    }
}
