# 循环预设系统实现文档

## 概述
实现了完整的循环预设播放系统，支持官方预设和用户自定义预设。

## 系统架构

### 1. 数据模型 (`data/model/PresetCommand.kt`)
```kotlin
// 单个预设命令
data class PresetCommand(
    val command: String,  // UUID命令（包含占位符 ** 和 ####）
    val time: Int = 1000  // 执行延迟（毫秒）
)

// 循环预设
data class LoopPreset(
    val id: String,
    val name: String,
    val description: String,
    val pic: String? = null,
    val commands: List<PresetCommand>,
    val isCustom: Boolean = false
)

// 预设列表容器
data class PresetList(
    val official: List<LoopPreset>,
    val custom: List<LoopPreset>
)
```

### 2. 数据仓库 (`data/repository/PresetRepository.kt`)
- **官方预设加载**：从 `assets/presets.json` 读取
- **自定义预设管理**：使用 SharedPreferences 存储
- **CRUD操作**：添加、删除、更新自定义预设

### 3. ViewModel (`ui/viewmodel/PresetViewModel.kt`)
- 管理官方预设列表
- 管理自定义预设列表
- 跟踪当前播放状态（`playingPresetId`）
- 提供预设操作接口

### 4. 循环播放服务 (`LoopPresetService.kt`)
- **前台服务**：通知ID 1003
- **循环机制**：按命令列表顺序循环发送
- **延迟控制**：每个命令有独立的延迟时间
- **进度显示**：通知显示"第X/Y个命令"
- **广播通知**：停止时发送 `BROADCAST_LOOP_STOPPED`

### 5. UI界面 (`ui/screens/PresetsScreen.kt`)
- 官方预设列表展示
- 自定义预设列表展示
- 预设详情对话框
- 循环播放控制（开始/停止）
- 播放状态指示器

## 官方预设数据

### 预设列表（10个）
1. **九浅一深** - 经典节奏，循序渐进
2. **暗涌** - 波浪起伏，渐入佳境
3. **旧梦** - 温柔缠绵，回味无穷
4. **狂澜** - 激情澎湃，势不可挡
5. **觊觎** - 欲拒还迎，若即若离
6. **逡巡** - 徘徊流连，意犹未尽
7. **谵妄** - 迷离恍惚，如梦如幻
8. **噬嗑** - 层层递进，欲罢不能
9. **大热** - 火热激情，一浪高过一浪
10. **渊薮** - 深邃幽远，探索未知

### 命令格式
```json
{
  "command": "710003**-8800-####-0000-1908090000",
  "time": 4000
}
```
- `**`：设备ID占位符（运行时替换）
- `####`：命令ID占位符（运行时替换）
- `time`：延迟时间（毫秒）

## 工作流程

### 启动循环播放
1. 用户点击预设卡片
2. 显示预设详情对话框
3. 点击"开始循环"按钮
4. 检查蓝牙权限和状态
5. 启动 `LoopPresetService` 前台服务
6. 服务开始循环发送命令
7. 更新UI播放状态

### 循环播放机制
```
命令1 → 延迟time1 → 命令2 → 延迟time2 → ... → 命令N → 延迟timeN → 回到命令1
```

### 停止循环播放
1. 用户点击"停止循环"按钮
2. 调用 `stopService()`
3. 服务发送 `BROADCAST_LOOP_STOPPED` 广播
4. UI监听广播并更新状态

## 通知系统

### 通知ID分配
- **1001**：BLE命令通知（BleService）
- **1002**：加热定时器通知（HeatingTimerService）
- **1003**：循环预设通知（LoopPresetService）

### 循环预设通知内容
- **标题**：🔄 预设名称
- **内容**：正在播放第 X/Y 个命令
- **操作**：停止按钮

## 自定义预设功能

### 存储方式
- 使用 SharedPreferences
- Key: `"custom_presets"`
- 格式: JSON数组

### 功能接口
```kotlin
// 添加自定义预设
viewModel.addCustomPreset(preset)

// 删除自定义预设
viewModel.deleteCustomPreset(presetId)

// 更新自定义预设
viewModel.updateCustomPreset(preset)
```

## 权限要求
- 蓝牙权限（BLUETOOTH_ADVERTISE / BLUETOOTH_CONNECT）
- 位置权限（ACCESS_FINE_LOCATION）
- 通知权限（POST_NOTIFICATIONS，Android 13+）

## 文件清单

### 新增文件
- `app/src/main/assets/presets.json` - 官方预设数据
- `app/src/main/java/aya/strokenet/data/model/PresetCommand.kt` - 数据模型
- `app/src/main/java/aya/strokenet/data/repository/PresetRepository.kt` - 数据仓库
- `app/src/main/java/aya/strokenet/ui/viewmodel/PresetViewModel.kt` - ViewModel
- `app/src/main/java/aya/strokenet/LoopPresetService.kt` - 循环播放服务

### 修改文件
- `app/src/main/java/aya/strokenet/ui/screens/PresetsScreen.kt` - 完全重写
- `app/src/main/AndroidManifest.xml` - 注册LoopPresetService
- `app/build.gradle.kts` - 添加kotlinx.serialization依赖

## 依赖项
```kotlin
// Kotlinx Serialization
implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")

// ViewModel
implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
```

## 测试建议

### 功能测试
1. ✅ 预设列表加载
2. ✅ 预设详情显示
3. ✅ 循环播放启动
4. ✅ 循环播放停止
5. ✅ 通知显示和更新
6. ✅ 页面切换状态保持
7. ⏳ 自定义预设创建（待实现）
8. ⏳ 自定义预设编辑（待实现）
9. ⏳ 自定义预设删除（待实现）

### 边界测试
- 空命令列表处理
- 蓝牙权限缺失处理
- 蓝牙未开启处理
- JSON解析失败处理
- 服务异常停止处理

## 后续优化方向

### 1. 自定义预设编辑器
- 可视化命令编辑界面
- 拖拽排序命令
- 实时预览效果

### 2. 预设分享功能
- 导出预设为JSON文件
- 导入其他用户的预设
- 二维码分享

### 3. 高级功能
- 预设收藏/标记
- 预设搜索和过滤
- 播放历史记录
- 循环次数限制

### 4. 性能优化
- 预设图片缓存
- 懒加载预设列表
- 命令发送队列优化

## 已知问题
- 无

## 编译状态
✅ **BUILD SUCCESSFUL** - 所有功能正常编译通过

## 更新日期
2026-05-31
