package aya.strokenet.data.model

import kotlinx.serialization.Serializable

/**
 * 预设命令
 * @param command UUID命令字符串（包含占位符）
 * @param time 执行时间（毫秒）
 */
@Serializable
data class PresetCommand(
    val command: String,
    val time: Int = 1000
)

/**
 * 循环预设模式
 * @param id 预设ID
 * @param name 预设名称
 * @param description 描述
 * @param pic 图片URL
 * @param commands 命令列表（循环播放）
 * @param isCustom 是否为用户自定义
 */
@Serializable
data class LoopPreset(
    val id: String,
    val name: String,
    val description: String,
    val pic: String? = null,
    val commands: List<PresetCommand>,
    val isCustom: Boolean = false
)

/**
 * 预设列表容器
 */
@Serializable
data class PresetList(
    val official: List<LoopPreset> = emptyList(),
    val custom: List<LoopPreset> = emptyList()
)
