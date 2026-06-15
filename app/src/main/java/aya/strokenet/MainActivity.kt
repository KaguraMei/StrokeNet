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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import aya.strokenet.ui.theme.StrokeNetTheme
import aya.strokenet.ui.components.GlassBottomDock
import aya.strokenet.ui.components.GlassTopBar
import aya.strokenet.ui.theme.iOSBg
import aya.strokenet.ui.viewmodel.ControlViewModel
import aya.strokenet.data.repository.PresetRepository
import aya.strokenet.data.model.LoopPreset
import aya.strokenet.ble.DaxiuBleAdvertiser
import org.json.JSONObject

class MainActivity : ComponentActivity() {
    
    private lateinit var bleAdvertiser: DaxiuBleAdvertiser
    private lateinit var presetRepository: PresetRepository
    
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
            bleAdvertiser = DaxiuBleAdvertiser(this)
            
            // 初始化 PresetRepository 并预加载所有预设
            presetRepository = PresetRepository(this)
            preloadPresets()
            
            // 检查并请求权限
            checkAndRequestPermissions()
            
            // 检查电池优化豁免（MCP Server 后台保活）
            checkBatteryOptimization()
            
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
    
    /**
     * 预加载所有预设（官方 + 自定义）
     */
    private fun preloadPresets() {
        try {
            val officialCount = presetRepository.loadOfficialPresets().size
            val customCount = presetRepository.loadCustomPresets().size
            android.util.Log.i("MainActivity", "预设已加载: 官方=$officialCount, 自定义=$customCount")
        } catch (e: Exception) {
            android.util.Log.e("MainActivity", "预设加载失败", e)
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        intent?.let {
            val action = it.getStringExtra("action") ?: return
            
            when (action) {
                // BLE控制指令
                "start", "thrust", "strength", "temp", "stop" -> {
                    handleBleAction(it, action)
                }
                // 预设管理指令
                "export_presets", "import_presets", "get_presets", 
                "create_preset", "delete_preset", "run_preset" -> {
                    handlePresetAction(it, action)
                }
            }
        }
    }
    
    private fun handleBleAction(intent: Intent, action: String) {
        // 启动前台服务来处理所有BLE指令
        val serviceIntent = Intent(this, BleService::class.java).apply {
            putExtra("action", action)
            
            when (action) {
                "start", "thrust" -> {
                    putExtra(BleService.EXTRA_DEPTH, intent.getIntExtra("depth", 36))
                    putExtra(BleService.EXTRA_EXTEND, intent.getIntExtra("extend", 8))
                    putExtra(BleService.EXTRA_RETRACT, intent.getIntExtra("retract", 8))
                }
                "strength", "temp" -> {
                    putExtra(BleService.EXTRA_VALUE, intent.getIntExtra("value", 50))
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
    
    private fun handlePresetAction(intent: Intent, action: String) {
        val repository = PresetRepository(this)
        
        try {
            when (action) {
                "export_presets" -> {
                    val json = repository.exportCustomPresetsJson()
                    // 通过广播返回结果
                    sendBroadcast(Intent("aya.strokenet.PRESET_RESULT").apply {
                        putExtra("action", "export_presets")
                        putExtra("result", json)
                        putExtra("success", true)
                    })
                    Toast.makeText(this, "预设已导出", Toast.LENGTH_SHORT).show()
                }
                
                "import_presets" -> {
                    val json = intent.getStringExtra("json") ?: return
                    val replace = intent.getBooleanExtra("replace", false)
                    val count = repository.importCustomPresetsJson(json, replace)
                    
                    sendBroadcast(Intent("aya.strokenet.PRESET_RESULT").apply {
                        putExtra("action", "import_presets")
                        putExtra("count", count)
                        putExtra("success", count > 0)
                    })
                    
                    if (count > 0) {
                        Toast.makeText(this, "导入 $count 个预设", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "导入失败", Toast.LENGTH_SHORT).show()
                    }
                }
                
                "get_presets" -> {
                    val allPresets = repository.getAllPresets()
                    val jsonArray = org.json.JSONArray()
                    allPresets.forEach { preset ->
                        jsonArray.put(JSONObject().apply {
                            put("id", preset.id)
                            put("name", preset.name)
                            put("description", preset.description)
                            put("isCustom", preset.isCustom)
                            put("commandCount", preset.commands.size)
                        })
                    }
                    
                    sendBroadcast(Intent("aya.strokenet.PRESET_RESULT").apply {
                        putExtra("action", "get_presets")
                        putExtra("result", jsonArray.toString())
                        putExtra("success", true)
                    })
                }
                
                "create_preset" -> {
                    val json = intent.getStringExtra("json") ?: return
                    val success = repository.importPresetJson(json)
                    
                    sendBroadcast(Intent("aya.strokenet.PRESET_RESULT").apply {
                        putExtra("action", "create_preset")
                        putExtra("success", success)
                    })
                    
                    Toast.makeText(this, if (success) "预设已创建" else "创建失败", Toast.LENGTH_SHORT).show()
                }
                
                "delete_preset" -> {
                    val presetId = intent.getStringExtra("preset_id") ?: return
                    repository.deleteCustomPreset(presetId)
                    
                    sendBroadcast(Intent("aya.strokenet.PRESET_RESULT").apply {
                        putExtra("action", "delete_preset")
                        putExtra("success", true)
                    })
                    
                    Toast.makeText(this, "预设已删除", Toast.LENGTH_SHORT).show()
                }
                
                "run_preset" -> {
                    val presetId = intent.getStringExtra("preset_id") ?: return
                    val preset = repository.getAllPresets().find { it.id == presetId }
                    
                    if (preset != null) {
                        // 启动循环预设服务
                        val serviceIntent = Intent(this, LoopPresetService::class.java).apply {
                            putExtra("preset_json", kotlinx.serialization.json.Json.encodeToString(
                                aya.strokenet.data.model.LoopPreset.serializer(), preset))
                        }
                        
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            startForegroundService(serviceIntent)
                        } else {
                            startService(serviceIntent)
                        }
                        
                        Toast.makeText(this, "运行预设: ${preset.name}", Toast.LENGTH_SHORT).show()
                        
                        sendBroadcast(Intent("aya.strokenet.PRESET_RESULT").apply {
                            putExtra("action", "run_preset")
                            putExtra("success", true)
                        })
                    } else {
                        Toast.makeText(this, "预设不存在", Toast.LENGTH_SHORT).show()
                        
                        sendBroadcast(Intent("aya.strokenet.PRESET_RESULT").apply {
                            putExtra("action", "run_preset")
                            putExtra("success", false)
                            putExtra("error", "预设不存在")
                        })
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "操作失败: ${e.message}", Toast.LENGTH_SHORT).show()
            
            sendBroadcast(Intent("aya.strokenet.PRESET_RESULT").apply {
                putExtra("action", action)
                putExtra("success", false)
                putExtra("error", e.message)
            })
        }
    }
    
    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        
        // 蓝牙权限
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_ADVERTISE)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            permissions.add(Manifest.permission.BLUETOOTH)
            permissions.add(Manifest.permission.BLUETOOTH_ADMIN)
        }
        
        // 位置权限
        permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
        
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
        if (!bleAdvertiser.isBluetoothEnabled()) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        }
    }
    
    /**
     * 检查并申请电池优化豁免
     * 核心原因：防止 Android 在退到后台时强行切断 MCP Server 的网络连接
     */
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as android.os.PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = android.net.Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    Toast.makeText(
                        this,
                        "为了让 MCP Server 在后台稳定运行，请允许应用无限制后台运行",
                        Toast.LENGTH_LONG
                    ).show()
                } catch (e: Exception) {
                    android.util.Log.e("MainActivity", "无法打开电池优化设置", e)
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        bleAdvertiser.shutdown()
    }
}

@Composable
fun StrokeNetApp(
    bleAdvertiser: DaxiuBleAdvertiser? = null,
    onCheckBluetooth: () -> Unit = {}
) {
    var currentScreen by rememberSaveable { mutableStateOf("control") }
    var customPresetEditTarget by remember { mutableStateOf<LoopPreset?>(null) }
    var showPresetEditorScreen by remember { mutableStateOf(false) }
    
    // 获取ViewModel，生命周期跟随Activity
    val controlViewModel: ControlViewModel = viewModel()

    // 页面标题映射
    val screenTitle = when {
        showPresetEditorScreen -> if (customPresetEditTarget == null) "新建预设" else "编辑预设"
        else -> when(currentScreen) {
            "control" -> "控制中心"
            "presets" -> "预设模式"
            "custom" -> "自定义循环"
            "mcp" -> "MCP Server"
            "settings" -> "设置"
            else -> "StrokeNet"
        }
    }

    Scaffold(
        containerColor = iOSBg,
        topBar = {
            GlassTopBar(title = screenTitle)
        },
        bottomBar = {
            // 编辑页面不显示底部导航
            if (!showPresetEditorScreen) {
                GlassBottomDock(
                    currentRoute = currentScreen,
                    onNavigate = { currentScreen = it }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                showPresetEditorScreen -> {
                    aya.strokenet.ui.screens.PresetEditorScreen(
                        preset = customPresetEditTarget,
                        onNavigateBack = {
                            showPresetEditorScreen = false
                            customPresetEditTarget = null
                        }
                    )
                }
                else -> {
                    when (currentScreen) {
                        "control" -> {
                            aya.strokenet.ui.screens.ControlScreen(
                                bleAdvertiser = bleAdvertiser,
                                onCheckBluetooth = onCheckBluetooth,
                                viewModel = controlViewModel
                            )
                        }
                        "presets" -> {
                            aya.strokenet.ui.screens.PresetsScreen(
                                bleAdvertiser = bleAdvertiser,
                                onCheckBluetooth = onCheckBluetooth
                            )
                        }
                        "custom" -> {
                            aya.strokenet.ui.screens.CustomPresetsScreen(
                                onNavigateBack = {}, // 不需要返回，因为是Tab页面
                                onEditPreset = { preset ->
                                    customPresetEditTarget = preset
                                    showPresetEditorScreen = true
                                }
                            )
                        }
                        "mcp" -> {
                            aya.strokenet.ui.screens.McpScreen()
                        }
                        "settings" -> {
                            aya.strokenet.ui.screens.SettingsScreen()
                        }
                    }
                }
            }
            
            // 全局悬浮停止按钮（右下角）
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .padding(bottom = if (!showPresetEditorScreen) 80.dp else 0.dp) // 避免遮挡底部导航
            ) {
                aya.strokenet.ui.components.GlobalStopButton()
            }
        }
    }
}
