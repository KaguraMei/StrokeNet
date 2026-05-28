package aya.strokenet.data.model

/**
 * 预设模式数据类
 */
data class Preset(
    val id: String,
    val name: String,
    val description: String,
    val icon: String,  // 图标名称
    val params: ControlParams
) {
    companion object {
        // 内置预设
        fun getBuiltInPresets(): List<Preset> = listOf(
            Preset(
                id = "gentle",
                name = "轻柔模式",
                description = "温和舒适，适合初次使用",
                icon = "favorite",
                params = ControlParams.GENTLE
            ),
            Preset(
                id = "standard",
                name = "标准模式",
                description = "平衡的体验，适合日常使用",
                icon = "star",
                params = ControlParams.STANDARD
            ),
            Preset(
                id = "intense",
                name = "强力模式",
                description = "强劲刺激，适合进阶体验",
                icon = "bolt",
                params = ControlParams.INTENSE
            )
        )
    }
}
