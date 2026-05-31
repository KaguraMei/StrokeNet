package aya.strokenet.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * 控制页面的ViewModel
 * 生命周期跟随Activity，页面切换时数据不会丢失
 */
class ControlViewModel : ViewModel() {
    // 推拉参数 (默认值改为0)
    var depth by mutableFloatStateOf(0f)
    var extendSpeed by mutableFloatStateOf(0f)
    var retractSpeed by mutableFloatStateOf(0f)
    
    // 增强参数 (默认值改为0)
    var strength by mutableFloatStateOf(0f)
    var temp by mutableFloatStateOf(0f)
    var tempDuration by mutableFloatStateOf(0f)
    
    // 状态
    var isRunning by mutableStateOf(false)
    var isHeating by mutableStateOf(false)
}
