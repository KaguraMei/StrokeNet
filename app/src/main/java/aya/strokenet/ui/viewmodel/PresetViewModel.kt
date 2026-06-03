package aya.strokenet.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import aya.strokenet.data.model.LoopPreset
import aya.strokenet.data.repository.PresetRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 预设页面的ViewModel
 */
class PresetViewModel(application: Application) : AndroidViewModel(application) {
    
    private val repository = PresetRepository(application)
    
    // 官方预设列表
    var officialPresets by mutableStateOf<List<LoopPreset>>(emptyList())
        private set
    
    // 自定义预设列表
    var customPresets by mutableStateOf<List<LoopPreset>>(emptyList())
        private set
    
    // 是否正在加载
    var isLoading by mutableStateOf(false)
        private set
    
    // 当前正在播放的预设ID
    var playingPresetId by mutableStateOf<String?>(null)
    
    init {
        // 同步加载预设到内存（SharedPreferences读取很快）
        try {
            officialPresets = repository.loadOfficialPresets()
            customPresets = repository.loadCustomPresets()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // 异步刷新确保最新
        loadPresets()
    }
    
    /**
     * 加载所有预设
     */
    fun loadPresets() {
        viewModelScope.launch {
            isLoading = true
            try {
                withContext(Dispatchers.IO) {
                    officialPresets = repository.loadOfficialPresets()
                    customPresets = repository.loadCustomPresets()
                }
            } finally {
                isLoading = false
            }
        }
    }
    
    /**
     * 添加自定义预设
     */
    fun addCustomPreset(preset: LoopPreset) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.addCustomPreset(preset)
            customPresets = repository.loadCustomPresets()
        }
    }
    
    /**
     * 删除自定义预设
     */
    fun deleteCustomPreset(presetId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.deleteCustomPreset(presetId)
            customPresets = repository.loadCustomPresets()
        }
    }
    
    /**
     * 更新自定义预设
     */
    fun updateCustomPreset(preset: LoopPreset) {
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateCustomPreset(preset)
            customPresets = repository.loadCustomPresets()
        }
    }
    
    /**
     * 获取所有预设
     */
    fun getAllPresets(): List<LoopPreset> {
        return officialPresets + customPresets
    }
    
    /**
     * 导出预设为JSON
     */
    fun exportPresets(): String {
        return repository.exportCustomPresetsJson()
    }
    
    /**
     * 导入预设JSON
     * @return 导入的预设数量
     */
    fun importPresets(json: String, replace: Boolean = false): Int {
        val count = repository.importCustomPresetsJson(json, replace)
        if (count > 0) {
            loadPresets()
        }
        return count
    }
}
