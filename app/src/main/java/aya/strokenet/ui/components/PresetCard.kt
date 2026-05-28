package aya.strokenet.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import aya.strokenet.data.model.Preset
import aya.strokenet.ui.theme.*

/**
 * 构成主义风格预设模式卡片组件
 */
@Composable
fun PresetCard(
    preset: Preset,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .border(2.dp, ConstructBlack)
            .background(ConstructWhite)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 图标块转换为几何标记
        Box(
            modifier = Modifier
                .width(48.dp)
                .height(84.dp)
                .background(ConstructYellow),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = preset.name.take(1), // 取首字母大写
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Black,
                fontSize = 24.sp
            )
        }
        
        // 右边框分隔线
        Box(
            modifier = Modifier
                .width(2.dp)
                .height(84.dp)
                .background(ConstructBlack)
        )
        
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(12.dp)
        ) {
            Text(
                text = preset.name.uppercase(),
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = ConstructBlue
            )
            Text(
                text = preset.description,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = ConstructGray
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                ConstructChip("D:${preset.params.depth}")
                ConstructChip("S:${preset.params.extendSpeed}")
                ConstructChip("P:${preset.params.strength}")
            }
        }
    }
}

@Composable
private fun ConstructChip(text: String) {
    Box(
        modifier = Modifier
            .border(1.dp, ConstructBlack)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
