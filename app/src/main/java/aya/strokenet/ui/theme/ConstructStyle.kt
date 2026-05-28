package aya.strokenet.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// 构成主义风格配色
val ConstructBlue = Color(0xFF1332A6)   // 克莱因蓝/工程蓝
val ConstructYellow = Color(0xFFFFD600) // 工业黄
val ConstructBlack = Color(0xFF0F0F0F)  // 极黑
val ConstructWhite = Color(0xFFF0F4F8)  // 蓝图纸白
val ConstructGray = Color(0xFF9E9E9E)   // 注释灰
val ConstructGridLine = Color(0xFFD0D7E2) // 网格线颜色

/**
 * 蓝图几何风格背景
 */
@Composable
fun ConstructBackground(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .fillMaxSize()
            .background(ConstructWhite)
    ) {
        val width = size.width
        val height = size.height

        // 1. 绘制基准网格 (Grid)
        val gridSize = 40.dp.toPx()
        for (i in 0..(width / gridSize).toInt()) {
            val x = i * gridSize
            drawLine(
                color = ConstructGridLine,
                start = Offset(x, 0f),
                end = Offset(x, height),
                strokeWidth = 1f
            )
        }
        for (i in 0..(height / gridSize).toInt()) {
            val y = i * gridSize
            drawLine(
                color = ConstructGridLine,
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1f
            )
        }

        // 2. 绘制几何度量圆与准星
        val center = Offset(width / 2, height / 3)
        drawCircle(
            color = ConstructBlue,
            radius = 120.dp.toPx(),
            center = center,
            style = Stroke(
                width = 2f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
            )
        )
        drawCircle(
            color = ConstructBlue,
            radius = 80.dp.toPx(),
            center = center,
            style = Stroke(width = 1f)
        )

        // 准星十字
        drawLine(
            ConstructBlue,
            center.copy(x = center.x - 140.dp.toPx()),
            center.copy(x = center.x + 140.dp.toPx()),
            2f
        )
        drawLine(
            ConstructBlue,
            center.copy(y = center.y - 140.dp.toPx()),
            center.copy(y = center.y + 140.dp.toPx()),
            2f
        )

        // 3. 绘制右下角技术参数线
        drawLine(
            color = ConstructBlue,
            start = Offset(width - 150.dp.toPx(), height - 100.dp.toPx()),
            end = Offset(width, height - 100.dp.toPx()),
            strokeWidth = 3f
        )
    }
}

/**
 * 构成主义风格的模块容器（替代原本圆润的Card）
 */
@Composable
fun ConstructBlock(
    modifier: Modifier = Modifier,
    title: String? = null,
    label: String = "+ DATA",
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .border(2.dp, ConstructBlue)
            .background(ConstructWhite.copy(alpha = 0.85f))
            .padding(2.dp)
    ) {
        Box(
            modifier = Modifier
                .border(1.dp, ConstructBlue)
                .padding(16.dp)
        ) {
            content()
            
            // 装饰性坐标号角
            Text(
                text = "L1: $label",
                fontFamily = FontFamily.Monospace,
                fontSize = 8.sp,
                color = ConstructBlue,
                modifier = Modifier.align(Alignment.TopEnd)
            )
        }
    }
}
