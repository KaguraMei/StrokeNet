package aya.strokenet.ui.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ControlScreen 的 ViewModel
 * 用于在页面切换时保持状态
 */
class ControlViewModel : ViewModel() {
    
    // 推拉参数
    private val _depth = MutableStateFlow(36f)
    val depth: StateFlow<Float> = _depth.asStateFlow()
    
    private val _extendSpeed = MutableStateFlow(8f)
    val extendSpeed: StateFlow<Float> = _extendSpeed.asStateFlow()
    
    private val _retractSpeed = MutableStateFlow(8f)
    val retractSpeed: StateFlow<Float> = _retractSpeed.asStateFlow()
    
    // 增强参数
    private val _strength = MutableStateFlow(50f)
    val strength: StateFlow<Float> = _strength.asStateFlow()
    
    private val _temp = MutableStateFlow(30f)
    val temp: StateFlow<Float> = _temp.asStateFlow()
    
    // 运行状态
    private val _isRunning = MutableStateFlow(false)
    val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()
    
    // 更新方法
    fun updateDepth(value: Float) {
        _depth.value = value
    }
    
    fun updateExtendSpeed(value: Float) {
        _extendSpeed.value = value
    }
    
    fun updateRetractSpeed(value: Float) {
        _retractSpeed.value = value
    }
    
    fun updateStrength(value: Float) {
        _strength.value = value
    }
    
    fun updateTemp(value: Float) {
        _temp.value = value
    }
    
    fun setRunning(running: Boolean) {
        _isRunning.value = running
    }
}
