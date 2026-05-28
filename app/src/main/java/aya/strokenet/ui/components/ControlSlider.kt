package aya.strokenet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aya.strokenet.ui.theme.*

/**
 * 构成主义风格控制滑块组件
 */
@Composable
fun ControlSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueText: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    description: String? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, ConstructBlue)
            .background(ConstructWhite)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column {
                Text(
                    text = label.uppercase(),
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Serif,
                    fontSize = 18.sp,
                    color = ConstructBlack
                )
                if (description != null) {
                    Text(
                        text = "//$description",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 10.sp,
                        color = ConstructGray
                    )
                }
            }
            
            // 数据显示块（模仿技术参数标签）
            Box(
                modifier = Modifier
                    .background(ConstructBlue)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "VAL_$valueText",
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = ConstructYellow,
                    fontSize = 14.sp
                )
            }
        }
        
        // 工业风滑块
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            enabled = enabled,
            colors = SliderDefaults.colors(
                thumbColor = ConstructYellow,
                activeTrackColor = ConstructBlack,
                inactiveTrackColor = ConstructGridLine
            )
        )
        
        // 底部刻度装饰线
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "+ ${valueRange.start.toInt()}",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
            Text(
                "- - - - - - - - - -",
                fontSize = 8.sp,
                color = ConstructGridLine
            )
            Text(
                "+ ${valueRange.endInclusive.toInt()}",
                fontSize = 8.sp,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
