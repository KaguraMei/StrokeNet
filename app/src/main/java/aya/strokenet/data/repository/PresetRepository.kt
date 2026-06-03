package aya.strokenet.data.repository

import android.content.Context
import android.content.SharedPreferences
import aya.strokenet.data.model.LoopPreset
import aya.strokenet.data.model.PresetList
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

/**
 * 预设数据仓库
 * 负责加载官方预设和管理自定义预设
 */
class PresetRepository(private val context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("presets", Context.MODE_PRIVATE)
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    companion object {
        private const val KEY_CUSTOM_PRESETS = "custom_presets"
    }
    
    /**
     * 加载官方预设（从assets/presets.json）
     */
    fun loadOfficialPresets(): List<LoopPreset> {
        return try {
            val jsonString = context.assets.open("presets.json").bufferedReader().use { it.readText() }
            val presetList = json.decodeFromString<PresetList>(jsonString)
            presetList.official
        } catch (e: IOException) {
            e.printStackTrace()
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 加载自定义预设（从SharedPreferences）
     */
    fun loadCustomPresets(): List<LoopPreset> {
        return try {
            val jsonString = prefs.getString(KEY_CUSTOM_PRESETS, null) ?: return emptyList()
            json.decodeFromString<List<LoopPreset>>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * 保存自定义预设
     */
    fun saveCustomPresets(presets: List<LoopPreset>) {
        try {
            val jsonString = json.encodeToString(presets)
            prefs.edit().putString(KEY_CUSTOM_PRESETS, jsonString).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 添加自定义预设
     */
    fun addCustomPreset(preset: LoopPreset) {
        val currentPresets = loadCustomPresets().toMutableList()
        currentPresets.add(preset.copy(isCustom = true))
        saveCustomPresets(currentPresets)
    }
    
    /**
     * 删除自定义预设
     */
    fun deleteCustomPreset(presetId: String) {
        val currentPresets = loadCustomPresets().toMutableList()
        currentPresets.removeAll { it.id == presetId }
        saveCustomPresets(currentPresets)
    }
    
    /**
     * 更新自定义预设
     */
    fun updateCustomPreset(preset: LoopPreset) {
        val currentPresets = loadCustomPresets().toMutableList()
        val index = currentPresets.indexOfFirst { it.id == preset.id }
        if (index != -1) {
            currentPresets[index] = preset.copy(isCustom = true)
            saveCustomPresets(currentPresets)
        }
    }
    
    /**
     * 获取所有预设（官方+自定义）
     */
    fun getAllPresets(): List<LoopPreset> {
        return loadOfficialPresets() + loadCustomPresets()
    }
    
    /**
     * 导出自定义预设为JSON字符串
     */
    fun exportCustomPresetsJson(): String {
        val presets = loadCustomPresets()
        return json.encodeToString(presets)
    }
    
    /**
     * 从JSON字符串导入自定义预设
     * @param jsonString JSON字符串
     * @param replace 是否替换现有预设（true=替换，false=合并）
     * @return 导入的预设数量
     */
    fun importCustomPresetsJson(jsonString: String, replace: Boolean = false): Int {
        return try {
            val importedPresets = json.decodeFromString<List<LoopPreset>>(jsonString)
            
            val finalPresets = if (replace) {
                importedPresets
            } else {
                val currentPresets = loadCustomPresets()
                val currentIds = currentPresets.map { it.id }.toSet()
                
                // 合并：去重，导入的预设如果ID重复则跳过
                val newPresets = importedPresets.filter { it.id !in currentIds }
                currentPresets + newPresets
            }
            
            saveCustomPresets(finalPresets.map { it.copy(isCustom = true) })
            importedPresets.size
        } catch (e: Exception) {
            e.printStackTrace()
            0
        }
    }
    
    /**
     * 导出单个预设为JSON字符串
     */
    fun exportPresetJson(presetId: String): String? {
        val preset = loadCustomPresets().find { it.id == presetId }
        return preset?.let { json.encodeToString(it) }
    }
    
    /**
     * 从JSON字符串导入单个预设
     */
    fun importPresetJson(jsonString: String): Boolean {
        return try {
            val preset = json.decodeFromString<LoopPreset>(jsonString)
            addCustomPreset(preset)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
