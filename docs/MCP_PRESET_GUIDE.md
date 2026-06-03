# MCP 自定义循环预设指南

## 概述

StrokeNet MCP Server 现在支持完整的自定义循环预设功能，让你可以：

- 🎨 **创建自定义预设** - 设计专属的循环节奏
- 🔄 **运行循环预设** - 自动循环播放预设动作
- 📤 **导出分享** - 将预设导出为JSON分享给他人
- 📥 **导入使用** - 导入他人分享的预设

## 快速开始

### 1. 启动完整功能版MCP服务器

```bash
# 安装依赖
pip install fastmcp

# 启动服务器
python mcp/daxiu_mcp_full.py
```

### 2. 在Claude Desktop中配置

编辑 `~/AppData/Roaming/Claude/claude_desktop_config.json` (Windows):

```json
{
  "mcpServers": {
    "daxiu-full": {
      "command": "python",
      "args": ["C:/path/to/mcp/daxiu_mcp_full.py"],
      "env": {
        "MCP_TRANSPORT": "stdio"
      }
    }
  }
}
```

或使用HTTP模式（通过cloudflared）：

```json
{
  "mcpServers": {
    "daxiu-full": {
      "url": "https://xxx.trycloudflare.com/mcp",
      "transport": "http"
    }
  }
}
```

## MCP工具完整列表

### 基础控制工具

#### `daxiu_start`
启动推拉运动
- `depth`: 深度 0-72 (默认36)
- `extend_speed`: 伸出速度 0-15 (默认8)
- `retract_speed`: 缩回速度 0-15 (默认8)

#### `daxiu_thrust`
运行中调节参数（不停止）
- `depth`: 深度 0-72
- `extend_speed`: 伸出速度 0-15
- `retract_speed`: 缩回速度 0-15

#### `daxiu_strength`
设置震动强度
- `value`: 强度 0-100

#### `daxiu_temp`
设置加热温度
- `value`: 温度 0-60°C

#### `daxiu_stop`
停止所有运动

---

### 预设管理工具

#### `list_presets`
列出所有可用预设（官方+自定义）

#### `get_official_presets`
获取所有官方预设的详细信息

#### `get_custom_presets`
获取所有自定义预设的详细信息

#### `get_preset_detail`
获取指定预设的详细动作参数

参数：
- `preset_id`: 预设ID

#### `create_custom_preset`
创建自定义循环预设

参数：
- `name`: 预设名称
- `description`: 预设描述
- `actions`: 动作列表，每个动作包含：
  - `depth`: 深度 1-100
  - `extend_speed`: 伸出速度 1-100
  - `retract_speed`: 缩回速度 1-100
  - `strength`: 震动强度 1-100
  - `duration`: 持续时间（毫秒）

**示例：**
```python
create_custom_preset(
    name="温柔渐进",
    description="从轻柔到适中的渐进节奏",
    actions=[
        {
            "depth": 30,
            "extend_speed": 40,
            "retract_speed": 40,
            "strength": 30,
            "duration": 3000
        },
        {
            "depth": 45,
            "extend_speed": 50,
            "retract_speed": 50,
            "strength": 40,
            "duration": 2500
        },
        {
            "depth": 55,
            "extend_speed": 55,
            "retract_speed": 55,
            "strength": 45,
            "duration": 2000
        }
    ]
)
```

#### `update_custom_preset`
更新现有的自定义预设（只能更新自定义预设，不能更新官方预设）

参数：
- `preset_id`: 要更新的预设ID（必填）
- `name`: 新的预设名称（可选）
- `description`: 新的预设描述（可选）
- `actions`: 新的动作列表（可选，格式同 create_custom_preset）

**示例：**
```python
# 只更新名称和描述
update_custom_preset(
    preset_id="uuid-xxx",
    name="温柔渐进 v2",
    description="优化后的温柔节奏"
)

# 更新所有内容
update_custom_preset(
    preset_id="uuid-xxx",
    name="温柔渐进 v2",
    description="优化后的温柔节奏",
    actions=[...]
)
```

#### `run_preset`
运行指定的循环预设

参数：
- `preset_id`: 预设ID（从 list_presets 获取）

预设会循环播放，使用 `stop_all()` 或 `stop_preset()` 停止。

#### `stop_preset`
停止当前运行的循环预设

#### `delete_custom_preset`
删除自定义预设（官方预设无法删除）

参数：
- `preset_id`: 预设ID

#### `export_custom_presets`
导出所有自定义预设为JSON格式

#### `import_custom_presets`
从JSON导入预设

参数：
- `json`: JSON字符串
- `replace`: 是否替换现有预设（默认false，合并模式）

---

### 快速预设模板

#### `preset_quick_gentle`
快速创建「轻柔模式」预设

#### `preset_quick_standard`
快速创建「标准模式」预设

#### `preset_quick_intense`
快速创建「强力模式」预设

#### `preset_quick_wave`
快速创建「波浪模式」预设 - 渐强渐弱的节奏

## 使用示例

### 示例1: 查看和运行预设

```
用户: "有哪些可用的预设？"

Claude: 我来查看所有可用的预设
[调用 list_presets()]
✓ 共 15 个预设:
- 轻柔模式 (ID: gentle-mode) 官方
- 标准模式 (ID: standard-mode) 官方
- 我的自定义预设 (ID: uuid-xxx) 自定义
...

用户: "运行轻柔模式"

Claude: [调用 run_preset(preset_id="gentle-mode")]
✓ 正在运行预设: 轻柔模式
提示: 使用 stop_all 或 stop_preset 停止
```

### 示例2: 创建自定义预设

```
用户: "创建一个九浅一深的预设，前面轻柔9次，最后一次深入"

Claude: 我来创建一个九浅一深的节奏预设
[调用 create_custom_preset(
    name="九浅一深",
    description="经典节奏，9次浅入，1次深入",
    actions=[
        {
            "depth": 30,
            "extend_speed": 45,
            "retract_speed": 45,
            "strength": 35,
            "duration": 800
        },  # 浅入动作
        # ... 重复9次相似配置
        {
            "depth": 75,
            "extend_speed": 60,
            "retract_speed": 60,
            "strength": 55,
            "duration": 2000
        }  # 深入动作
    ]
)]
✓ 预设已创建: 九浅一深
ID: uuid-xxx
动作数: 10
```

### 示例3: 修改现有预设

```
用户: "把九浅一深的预设改得更温柔一些"

Claude: 我来更新这个预设，降低强度和速度
[调用 update_custom_preset(
    preset_id="uuid-xxx",
    name="九浅一深（温柔版）",
    description="更温柔的九浅一深节奏",
    actions=[
        {
            "depth": 25,
            "extend_speed": 35,
            "retract_speed": 35,
            "strength": 25,
            "duration": 1000
        },
        # ... 调整后的动作
    ]
)]
✓ 预设已更新: 九浅一深（温柔版）
```

### 示例4: 导出和分享预设

```
用户: "导出我的所有自定义预设"

Claude: [调用 export_custom_presets()]
✓ 自定义预设JSON:

[
  {
    "id": "uuid-xxx",
    "name": "九浅一深",
    ...
  }
]
```

### 示例5: 导入他人分享的预设

```
用户: "导入这个预设：[粘贴JSON]"

Claude: [调用 import_custom_presets(json="...", replace=false)]
✓ 已导入 1 个预设（合并模式）
```

### 示例6: 查看预设详情

```
用户: "看看轻柔模式的具体参数"

Claude: [调用 get_preset_detail(preset_id="gentle-mode")]

【轻柔模式】
ID: gentle-mode
描述: 温柔舒适的节奏
类型: 官方
动作数: 3
总时长: 7秒/轮

动作列表:
动作 1:
  深度: 30
  伸速度: 40
  缩速度: 40
  强度: 30
  持续时间: 2000ms (2.0秒)
...
```

## 预设设计技巧

### 参数理解

1. **depth (深度)**
   - 1-20: 极浅，挑逗
   - 21-40: 浅入，温柔
   - 41-60: 中等深度
   - 61-80: 深入
   - 81-100: 全深度

2. **speed (速度)**
   - 1-30: 缓慢，温柔
   - 31-60: 中速，标准
   - 61-100: 快速，激烈

3. **strength (强度)**
   - 1-30: 轻微震动
   - 31-60: 中等震动
   - 61-100: 强烈震动

4. **duration (时长)**
   - 500-1000ms: 快节奏
   - 1000-3000ms: 标准节奏
   - 3000-5000ms: 慢节奏

### 预设模式建议

#### 渐进式
从轻到重逐渐增强：
```python
[
    {"depth": 30, "speed": 40, "duration": 2000},
    {"depth": 50, "speed": 50, "duration": 2000},
    {"depth": 70, "speed": 60, "duration": 2000}
]
```

#### 波浪式
强弱交替：
```python
[
    {"depth": 40, "speed": 50, "duration": 1500},
    {"depth": 70, "speed": 70, "duration": 1500},
    {"depth": 40, "speed": 50, "duration": 1500}
]
```

#### 节奏式
有规律的节奏变化：
```python
[
    {"depth": 30, "speed": 45, "duration": 800},  # 短促
    {"depth": 30, "speed": 45, "duration": 800},
    {"depth": 30, "speed": 45, "duration": 800},
    {"depth": 70, "speed": 60, "duration": 2000}  # 深入
]
```

## 在APP中使用

### 手动创建预设

1. 打开APP
2. 导航到「预设」页面
3. 点击「自定义预设」
4. 点击「+」创建新预设
5. 添加动作，设置参数
6. 保存

### 导入导出预设

1. **导出**：
   - 进入「自定义预设」页面
   - 点击「导出」图标
   - JSON会显示在对话框中
   - 点击「复制」将JSON复制到剪贴板

2. **导入**：
   - 进入「自定义预设」页面
   - 点击「导入」图标
   - 粘贴JSON数据
   - 点击「导入」

## JSON格式参考

### 单个预设格式

```json
{
  "id": "uuid-xxx",
  "name": "我的预设",
  "description": "自定义描述",
  "commands": [
    {
      "command": "710003**-8800-####-0000-322828280000",
      "time": 2000
    }
  ],
  "isCustom": true
}
```

### 多个预设格式

```json
[
  {
    "id": "preset-1",
    "name": "预设1",
    ...
  },
  {
    "id": "preset-2",
    "name": "预设2",
    ...
  }
]
```

## 常见问题

### Q: 预设运行后如何停止？
A: 调用 `stop_all()` 或 `stop_preset()` 停止运行的预设。

### Q: 可以修改官方预设吗？
A: 官方预设不能修改，但可以：
1. 使用 `get_preset_detail()` 查看官方预设的参数
2. 基于这些参数创建自己的自定义版本
3. 使用 `create_custom_preset()` 保存为自定义预设

### Q: 如何修改自定义预设？
A: 使用 `update_custom_preset()` 工具，可以选择性更新名称、描述或动作列表。

### Q: 导入的预设ID重复怎么办？
A: 
- 合并模式（`replace=false`）：重复ID的预设会被跳过
- 替换模式（`replace=true`）：会删除所有现有预设，只保留导入的

### Q: 预设存储在哪里？
A: 
- **官方预设**：存储在 APP 的 `assets/presets.json` 文件中，启动时自动加载
- **自定义预设**：存储在 SharedPreferences 中，持久保存，不会丢失

### Q: 应用启动时会加载预设吗？
A: 是的，应用启动时会自动加载所有官方预设和自定义预设到内存中，确保快速访问。

### Q: 命令格式是什么？
A: 命令格式为 `710003**-8800-####-0000-DDEERRSS0000`，其中：
- DD: 深度（十六进制）
- EE: 伸出速度（十六进制）
- RR: 缩回速度（十六进制）
- SS: 强度（十六进制）

## 安全建议

1. **循序渐进**：第一次使用时从轻柔模式开始
2. **注意强度**：不要长时间使用高强度设置
3. **及时停止**：如有不适立即停止
4. **合理时长**：单次使用不宜过长

## 技术支持

如有问题或建议，请查看：
- [MCP集成指南](./MCP_INTEGRATION_GUIDE.md)
- [MCP快速开始](./MCP_QUICKSTART.md)
- [项目文档索引](./DOCS_INDEX.md)
