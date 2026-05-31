package aya.strokenet.data.model

/**
 * 控制参数数据类
 */
data class ControlParams(
    val depth: Int = 0,           // 推拉深度 1-100 (UI显示)
    val extendSpeed: Int = 0,      // 伸出速度 1-100 (UI显示)
    val retractSpeed: Int = 0,     // 缩回速度 1-100 (UI显示)
    val strength: Int = 0,        // 入体端强度 1-100 (UI显示)
    val temp: Int = 0,            // 加热温度 0-60 (UI显示)
    val tempDuration: Int = 0      // 加热时长（分钟）0-10，0表示不加热
) {
    companion object {
        // 参数范围 (UI显示范围)
        const val DEPTH_MIN = 1
        const val DEPTH_MAX = 100
        const val SPEED_MIN = 1
        const val SPEED_MAX = 100
        const val STRENGTH_MIN = 1
        const val STRENGTH_MAX = 100
        const val TEMP_MIN = 0
        const val TEMP_MAX = 60
        const val TEMP_DURATION_MIN = 0
        const val TEMP_DURATION_MAX = 10  // 最大10分钟
        
        // 默认预设 (新范围: 1-100)
        val GENTLE = ControlParams(
            depth = 28,        // 约28% (原20/72)
            extendSpeed = 33,  // 约33% (原5/15)
            retractSpeed = 33, // 约33% (原5/15)
            strength = 30,
            temp = 25,
            tempDuration = 5
        )
        
        val STANDARD = ControlParams(
            depth = 50,        // 50% (原36/72)
            extendSpeed = 53,  // 约53% (原8/15)
            retractSpeed = 53, // 约53% (原8/15)
            strength = 50,
            temp = 35,
            tempDuration = 5
        )
        
        val INTENSE = ControlParams(
            depth = 83,        // 约83% (原60/72)
            extendSpeed = 80,  // 80% (原12/15)
            retractSpeed = 80, // 80% (原12/15)
            strength = 80,
            temp = 40,
            tempDuration = 5
        )
    }
    
    /**
     * 验证参数是否在有效范围内
     */
    fun isValid(): Boolean {
        return depth in DEPTH_MIN..DEPTH_MAX &&
                extendSpeed in SPEED_MIN..SPEED_MAX &&
                retractSpeed in SPEED_MIN..SPEED_MAX &&
                strength in STRENGTH_MIN..STRENGTH_MAX &&
                temp in TEMP_MIN..TEMP_MAX &&
                tempDuration in TEMP_DURATION_MIN..TEMP_DURATION_MAX
    }
    
    /**
     * 是否启用加热（加热时长大于0）
     */
    fun isHeatingEnabled(): Boolean = tempDuration > 0
}
