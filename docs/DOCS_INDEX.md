# 📚 StrokeNet 2.0 文档索引

## 🎯 快速导航

### 🤖 给 LLM/AI Agent
**[LLM.md](LLM.md)** ⭐⭐⭐⭐⭐ - 项目全景、快速定位、开发决策

### 👤 给用户
[README.md](../README.md) → [MCP_QUICKSTART.md](MCP_QUICKSTART.md) → [MCP_PRESET_GUIDE.md](MCP_PRESET_GUIDE.md)

### 👨‍💻 给开发者
[LLM.md](LLM.md) → [UUID_ENCODING.md](UUID_ENCODING.md) → [SERVICE_INTEGRATION.md](SERVICE_INTEGRATION.md)

---

## 📖 核心文档（6个）

| 文档 | 说明 | 读者 | 优先级 |
|------|------|------|--------|
| **[LLM.md](LLM.md)** | **LLM 开发指南**（项目全景、快速定位、架构决策） | LLM/AI | ⭐⭐⭐⭐⭐ |
| [MCP_QUICKSTART.md](MCP_QUICKSTART.md) | MCP Server 5分钟快速开始 | 用户 | ⭐⭐⭐⭐⭐ |
| [MCP_PRESET_GUIDE.md](MCP_PRESET_GUIDE.md) | 预设管理完整指南（创建/编辑/导入导出） | 用户 | ⭐⭐⭐⭐⭐ |
| [UUID_ENCODING.md](UUID_ENCODING.md) | **BLE UUID 编码协议详解** | 开发者 | ⚠️ 必读 |
| [SERVICE_INTEGRATION.md](SERVICE_INTEGRATION.md) | BLE 前台服务集成说明 | 开发者 | ⭐⭐⭐⭐ |
| [RELEASE_BUILD_GUIDE.md](RELEASE_BUILD_GUIDE.md) | 正式版 APK 打包指南 | 开发者 | 📦 |

---

## 📝 扩展文档（2个）

| 文档 | 说明 | 状态 |
|------|------|------|
| [MCP_INTEGRATION_GUIDE.md](MCP_INTEGRATION_GUIDE.md) | MCP 详细集成文档（技术细节） | 可选 |
| [TERMUX_MCP_GUIDE.md](TERMUX_MCP_GUIDE.md) | Termux + Python MCP 配置（1.0 留档） | 可选 |

---

## 🗂️ 按用途分类

### 快速开始
- [README.md](../README.md) - 项目总览
- [MCP_QUICKSTART.md](MCP_QUICKSTART.md) - 5分钟上手

### 功能使用
- [MCP_PRESET_GUIDE.md](MCP_PRESET_GUIDE.md) - 预设管理
- [MCP_INTEGRATION_GUIDE.md](MCP_INTEGRATION_GUIDE.md) - MCP 详细功能

### 开发文档
- [LLM.md](LLM.md) - **LLM 开发指南** ⭐
- [UUID_ENCODING.md](UUID_ENCODING.md) - 协议实现 ⚠️
- [SERVICE_INTEGRATION.md](SERVICE_INTEGRATION.md) - 服务架构

### 发布部署
- [RELEASE_BUILD_GUIDE.md](RELEASE_BUILD_GUIDE.md) - 打包发布

### 可选方案
- [TERMUX_MCP_GUIDE.md](TERMUX_MCP_GUIDE.md) - Termux MCP (1.0)

---

## 📋 完整文档列表

### 活跃文档（8个）

```
docs/
├── LLM.md                        # LLM 开发指南 ⭐ 新增
├── README.md (主目录)            # 项目总览和快速开始
├── DOCS_INDEX.md                 # 本文档 - 文档导航
│
├── MCP_QUICKSTART.md             # MCP 5分钟快速开始 ⚡
├── MCP_PRESET_GUIDE.md           # 预设管理完整指南 🎨
├── MCP_INTEGRATION_GUIDE.md      # MCP 详细集成文档 🔧
│
├── UUID_ENCODING.md              # UUID 编码协议 ⚠️ 必读
├── SERVICE_INTEGRATION.md        # BLE 服务集成说明
└── RELEASE_BUILD_GUIDE.md        # 发布打包指南 📦
```

### 可选文档（1个）

```
docs/
└── TERMUX_MCP_GUIDE.md           # Termux MCP 配置（1.0 留档）
```

### 已删除文档

```
docs/
├── AI_AGENT_GUIDE.md             # → 替换为 LLM.md
├── LOOP_PRESET_IMPLEMENTATION.md # → 合并到 MCP_PRESET_GUIDE.md
├── CUSTOM_PRESET_FEATURE.md      # → 合并到 MCP_PRESET_GUIDE.md
├── MCP_SDK_MIGRATION.md          # → 删除（工作存档）
├── MCP_SIMPLIFICATION.md         # → 删除（工作存档）
└── mcp_config_examples.json      # → 合并到各文档
```

---

## 🔍 如何查找文档

### 按需求查找

| 我想... | 查看文档 |
|--------|---------|
| 了解项目（LLM） | [LLM.md](LLM.md) ⭐ |
| 了解项目（人类） | [README.md](../README.md) |
| 快速启动 MCP | [MCP_QUICKSTART.md](MCP_QUICKSTART.md) ⚡ |
| 管理预设 | [MCP_PRESET_GUIDE.md](MCP_PRESET_GUIDE.md) 🎨 |
| 理解 BLE 协议 | [UUID_ENCODING.md](UUID_ENCODING.md) ⚠️ |
| 集成 MCP 功能 | [MCP_INTEGRATION_GUIDE.md](MCP_INTEGRATION_GUIDE.md) |
| 打包发布 APK | [RELEASE_BUILD_GUIDE.md](RELEASE_BUILD_GUIDE.md) 📦 |
| 了解服务架构 | [SERVICE_INTEGRATION.md](SERVICE_INTEGRATION.md) |
| 使用 Termux MCP | [TERMUX_MCP_GUIDE.md](TERMUX_MCP_GUIDE.md) (1.0) |

### 按角色查找

| 角色 | 推荐文档 |
|------|---------|
| **LLM/AI Agent** | LLM.md ⭐ |
| **普通用户** | README → MCP_QUICKSTART → MCP_PRESET_GUIDE |
| **Android 开发者** | LLM.md → UUID_ENCODING → SERVICE_INTEGRATION |
| **MCP 开发者** | LLM.md → MCP_QUICKSTART → MCP_INTEGRATION_GUIDE |
| **高级用户（Termux）** | TERMUX_MCP_GUIDE |

---

## � 文档优化记录

### 2.0 版本优化（2026-06-03）

#### 新增
- ✅ **[LLM.md](LLM.md)** - LLM 专用开发指南，替代 AI_AGENT_GUIDE.md

#### 整合
- ✅ LOOP_PRESET_IMPLEMENTATION.md → 合并到 MCP_PRESET_GUIDE.md
- ✅ CUSTOM_PRESET_FEATURE.md → 合并到 MCP_PRESET_GUIDE.md
- ✅ mcp_config_examples.json → 分散到各文档

#### 删除
- ✅ AI_AGENT_GUIDE.md → 替换为 LLM.md（更新到 2.0）
- ✅ MCP_SDK_MIGRATION.md → 删除（工作存档）
- ✅ MCP_SIMPLIFICATION.md → 删除（工作存档）

#### 优化结果
- **文档数量**: 13个 → 9个（活跃）
- **重复内容**: 显著减少
- **查找效率**: 更加清晰
- **维护成本**: 大幅降低

---

## 🔗 相关链接

### 外部资源
- [MCP 协议规范](https://spec.modelcontextprotocol.io/)
- [Kotlin MCP SDK](https://github.com/modelcontextprotocol/kotlin-sdk)
- [Android Developer](https://developer.android.com)
- [Ktor 文档](https://ktor.io/)

### 启发来源
- [Cachito 大秀炮机教程](https://claude.ai/public/artifacts/921eda06-e567-4cde-85af-8cde831a608f)  
  ⚠️ 注意：该教程的 UUID 编码逻辑不正确，请以本项目 [UUID_ENCODING.md](UUID_ENCODING.md) 为准

---

**文档版本**: 2.0  
**项目版本**: 2.1.0  
**最后更新**: 2026-06-03


