package aya.strokenet.data.model

/**
 * 控制参数数据类
 */
data class ControlParams(
    val depth: Int = 36,           // 推拉深度 0-72
    val extendSpeed: Int = 8,      // 伸出速度 0-15
    val retractSpeed: Int = 8,     // 缩回速度 0-15
    val strength: Int = 50,        // 入体端强度 0-100
    val temp: Int = 30             // 加热温度 0-60
) {
    companion object {
        // 参数范围
        const val DEPTH_MIN = 0
        const val DEPTH_MAX = 72
        const val SPEED_MIN = 0
        const val SPEED_MAX = 15
        const val STRENGTH_MIN = 0
        const val STRENGTH_MAX = 100
        const val TEMP_MIN = 0
        const val TEMP_MAX = 60
        
        // 默认预设
        val GENTLE = ControlParams(
            depth = 20,
            extendSpeed = 5,
            retractSpeed = 5,
            strength = 30,
            temp = 25
        )
        
        val STANDARD = ControlParams(
            depth = 36,
            extendSpeed = 8,
            retractSpeed = 8,
            strength = 50,
            temp = 35
        )
        
        val INTENSE = ControlParams(
            depth = 60,
            extendSpeed = 12,
            retractSpeed = 12,
            strength = 80,
            temp = 40
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
                temp in TEMP_MIN..TEMP_MAX
    }
}
