package aya.strokenet.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import aya.strokenet.data.model.LoopPreset
import aya.strokenet.ui.components.GlassPanel
import aya.strokenet.ui.theme.*
import aya.strokenet.ui.viewmodel.PresetViewModel

@Composable
fun CustomPresetsScreen(
    onNavigateBack: () -> Unit,
    onEditPreset: (LoopPreset?) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PresetViewModel = viewModel()
) {
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }
    var presetToDelete by remember { mutableStateOf<LoopPreset?>(null) }
    
    // 删除确认对话框
    if (showDeleteDialog && presetToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text("删除预设", fontWeight = FontWeight.Bold)
            },
            text = {
                Text("确定要删除「${presetToDelete!!.name}」吗？此操作无法撤销。")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteCustomPreset(presetToDelete!!.id)
                        Toast.makeText(context, "已删除", Toast.LENGTH_SHORT).show()
                        showDeleteDialog = false
                        presetToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = iOSRed)
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("取消", color = iOSTextSecondary)
                }
            }
        )
    }
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(iOSBg)
    ) {
        // 顶部导航栏
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.95f))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    Icons.Default.ArrowBack,
                    contentDescription = "返回",
                    tint = iOSBlue
                )
            }
            Text(
                text = "自定义预设",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onEditPreset(null) }
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "新建",
                    tint = iOSBlue
                )
            }
        }
        
        // 内容区域
        if (viewModel.customPresets.isEmpty()) {
            // 空状态
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = iOSTextSecondary.copy(alpha = 0.5f)
                    )
                    Text(
                        text = "还没有自定义预设",
                        fontSize = 16.sp,
                        color = iOSTextSecondary
                    )
                    Button(
                        onClick = { onEditPreset(null) },
                        colors = ButtonDefaults.buttonColors(containerColor = iOSBlue)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("创建第一个预设")
                    }
                }
            }
        } else {
            // 预设列表
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "共 ${viewModel.customPresets.size} 个自定义预设",
                    fontSize = 14.sp,
                    color = iOSTextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                viewModel.customPresets.forEach { preset ->
                    CustomPresetCard(
                        preset = preset,
                        onEdit = { onEditPreset(preset) },
                        onDelete = {
                            presetToDelete = preset
                            showDeleteDialog = true
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun CustomPresetCard(
    preset: LoopPreset,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    GlassPanel(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = preset.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = iOSTextPrimary
                    )
                    Text(
                        text = preset.description,
                        fontSize = 14.sp,
                        color = iOSTextSecondary
                    )
                }
            }
            
            // 统计信息
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                InfoChip(
                    icon = Icons.Default.List,
                    text = "${preset.commands.size} 个动作"
                )
                InfoChip(
                    icon = Icons.Default.Timer,
                    text = "${preset.commands.sumOf { it.time } / 1000}秒/轮"
                )
            }
            
            HorizontalDivider(color = Color.Black.copy(alpha = 0.05f))
            
            // 操作按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = iOSBlue
                    )
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("编辑")
                }
                
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = iOSRed
                    )
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("删除")
                }
            }
        }
    }
}

@Composable
fun InfoChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier
            .background(
                Color.Black.copy(alpha = 0.03f),
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = iOSBlue
        )
        Text(
            text = text,
            fontSize = 13.sp,
            color = iOSTextPrimary
        )
    }
}
