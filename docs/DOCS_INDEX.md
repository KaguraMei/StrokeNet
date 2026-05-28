# StrokeNet 文档索引

> 📚 所有项目文档的导航和说明

**最后更新**: 2026-05-29

---

## 🎯 快速导航

### 👨‍💻 给 AI Agent

**必读**: [`AI_AGENT_GUIDE.md`](AI_AGENT_GUIDE.md) ⭐⭐⭐⭐⭐

这是专为 AI Agent 设计的综合开发指南，整合了所有关键信息：
- 项目概述和当前状态
- 完整的技术架构
- 核心功能说明
- 开发指南和快速参考
- 下一步工作清单

**如果你是 AI Agent，从这里开始！**

### 👤 给人类开发者

**必读**: [`../README.md`](../README.md) ⭐⭐⭐⭐⭐

项目介绍、快速开始、功能特性、使用说明。

---

## 📖 文档分类

### 🚀 入门文档

| 文档 | 用途 | 推荐对象 | 优先级 |
|------|------|---------|--------|
| [`../README.md`](../README.md) | 项目介绍、快速开始 | 所有人 | ⭐⭐⭐⭐⭐ |
| [`QUICKSTART.md`](QUICKSTART.md) | 快速上手指南 | 新手 | ⭐⭐⭐⭐ |
| [`AI_AGENT_GUIDE.md`](AI_AGENT_GUIDE.md) | AI Agent 综合指南 | AI Agent | ⭐⭐⭐⭐⭐ |

### 🏗️ 架构文档

| 文档 | 用途 | 推荐对象 | 优先级 |
|------|------|---------|--------|
| [`ARCHITECTURE.md`](ARCHITECTURE.md) | 架构设计说明 | 开发者 | ⭐⭐⭐⭐ |
| [`PROJECT_GUIDE.md`](PROJECT_GUIDE.md) | 完整项目指南 | 开发者 | ⭐⭐⭐ |

### 🔧 开发文档

| 文档 | 用途 | 推荐对象 | 优先级 |
|------|------|---------|--------|
| [`SERVICE_INTEGRATION.md`](SERVICE_INTEGRATION.md) | BLE Service 集成说明 | 开发者 | ⭐⭐⭐⭐⭐ |
| [`UUID_ENCODING.md`](UUID_ENCODING.md) | UUID 编码配置说明 | 核心开发者 | ⭐⭐⭐⭐⭐ |
| [`BUILD_GUIDE.md`](BUILD_GUIDE.md) | 构建和部署指南 | 开发者 | ⭐⭐⭐ |

---

## 📋 文档详细说明

### 核心文档

#### [`AI_AGENT_GUIDE.md`](AI_AGENT_GUIDE.md)
**目标读者**: AI Agent  
**内容**:
- 项目概述和当前状态
- 完整技术架构
- BLE Service 集成
- 核心功能详解
- 文件结构说明
- 开发指南和常见任务
- 下一步工作清单
- 快速参考和命令速查

**何时阅读**: AI Agent 开始工作时的第一份文档

---

#### [`../README.md`](../README.md)
**目标读者**: 所有人  
**内容**:
- 项目简介和特性
- 快速开始（编译、安装、测试）
- 文档导航
- 项目结构
- 控制协议说明
- Intent 接口使用
- MCP 服务集成
- 前台服务特性

**何时阅读**: 第一次接触项目时

---

### 技术文档

#### [`SERVICE_INTEGRATION.md`](SERVICE_INTEGRATION.md)
**目标读者**: 开发者  
**内容**:
- BLE Service 架构设计
- 前台服务工作流程
- 重试机制说明
- 通知栏状态
- 未来功能规划（自定义动作循环）

**何时阅读**: 需要理解或修改 Service 层时

---

#### [`ARCHITECTURE.md`](ARCHITECTURE.md)
**目标读者**: 开发者  
**内容**:
- 技术栈
- 架构模式
- 模块划分
- 数据流
- 依赖关系

**何时阅读**: 需要理解整体架构时

---

#### [`UUID_ENCODING.md`](UUID_ENCODING.md)
**目标读者**: 核心开发者  
**内容**:
- UUID 编码原理
- 逆向方法
- 参数映射
- 编码示例
- 调整步骤

**何时阅读**: ⚠️ 必读！需要调整 BLE 广播编码时

---

#### [`BUILD_GUIDE.md`](BUILD_GUIDE.md)
**目标读者**: 开发者  
**内容**:
- 环境配置
- 编译步骤
- 部署方法
- 常见问题

**何时阅读**: 第一次编译项目时

---

### 参考文档

#### [`PROJECT_GUIDE.md`](PROJECT_GUIDE.md)
**目标读者**: 开发者  
**内容**: 完整的项目指南

**何时阅读**: 需要详细了解项目时（建议优先阅读 AI_AGENT_GUIDE.md）

---

#### [`QUICKSTART.md`](QUICKSTART.md)
**目标读者**: 新手  
**内容**: 快速上手指南

**何时阅读**: 第一次使用时

---

## 🗺️ 阅读路径推荐

### 路径 1: AI Agent 开发

```
1. AI_AGENT_GUIDE.md          (综合指南)
2. SERVICE_INTEGRATION.md     (Service 架构)
3. UUID_ENCODING.md           (UUID 编码)
4. 开始开发
```

### 路径 2: 人类开发者入门

```
1. ../README.md               (项目介绍)
2. QUICKSTART.md              (快速开始)
3. BUILD_GUIDE.md             (编译部署)
4. ARCHITECTURE.md            (架构理解)
5. SERVICE_INTEGRATION.md     (Service 层)
6. UUID_ENCODING.md           (核心功能)
```

### 路径 3: 快速修改

```
1. AI_AGENT_GUIDE.md          (快速参考)
2. 相关具体文档
```

---

## 📊 文档状态

| 文档 | 状态 | 最后更新 |
|------|------|---------|
| `AI_AGENT_GUIDE.md` | ✅ 最新 | 2026-05-29 |
| `README.md` | ✅ 最新 | 2026-05-29 |
| `SERVICE_INTEGRATION.md` | ✅ 最新 | 2026-05-29 |
| `UUID_ENCODING.md` | ✅ 最新 | 2026-05-28 |
| `ARCHITECTURE.md` | ✅ 最新 | 2026-05-29 |
| `PROJECT_GUIDE.md` | ⚠️ 需更新 | - |
| `BUILD_GUIDE.md` | ✅ 有效 | - |
| `QUICKSTART.md` | ✅ 有效 | - |

---

## 🔄 文档维护

### 更新原则

1. **重大变更必须更新文档**
2. **优先更新 AI_AGENT_GUIDE.md**
3. **保持文档同步**
4. **标注更新日期**

### 需要更新的情况

- ✅ 添加新功能
- ✅ 修改架构
- ✅ 修复重要问题
- ✅ 调整 UUID 编码
- ✅ 重构代码

### 文档优先级

1. **AI_AGENT_GUIDE.md** - 最高优先级
2. **README.md** - 高优先级
3. **SERVICE_INTEGRATION.md** - 高优先级
4. **UUID_ENCODING.md** - 高优先级
5. 其他文档 - 按需更新

---

**维护**: 请在更新文档后同步更新本索引  
**版本**: 2.0.0  
**最后更新**: 2026-05-29

---

*本索引由 AI Agent 生成和维护*

---

## 📋 文档详细说明

### 核心文档

#### [`AI_AGENT_GUIDE.md`](AI_AGENT_GUIDE.md)
**目标读者**: AI Agent  
**内容**:
- 项目概述和当前状态
- 完整技术架构
- 设计系统（iOS 玻璃拟态）
- 核心功能详解
- 文件结构说明
- 开发指南和常见任务
- 已知问题和解决方案
- 下一步工作清单
- 快速参考和命令速查

**何时阅读**: AI Agent 开始工作时的第一份文档

---

#### [`../README.md`](../README.md)
**目标读者**: 所有人  
**内容**:
- 项目简介和特性
- 快速开始（编译、安装、测试）
- 文档导航
- 设计风格介绍
- 项目结构
- 控制协议说明
- Intent 接口使用
- MCP 服务集成

**何时阅读**: 第一次接触项目时

---

### 设计文档

#### [`DESIGN_IOS_GLASSMORPHISM.md`](DESIGN_IOS_GLASSMORPHISM.md)
**目标读者**: UI 开发者、设计师  
**内容**:
- 设计理念和目标
- 核心颜色系统
- 玻璃拟态组件详解
- 页面布局规范
- 视觉层级系统
- 圆角、间距、阴影规范
- 与构成主义设计的对比
- 适配说明
- 设计原则
- 使用示例

**何时阅读**: 需要修改 UI 或添加新页面时

---

#### [`DESIGN_CONSTRUCTIVISM.md`](DESIGN_CONSTRUCTIVISM.md)
**目标读者**: 了解历史  
**状态**: ⚠️ 已废弃  
**内容**: 旧的构成主义设计系统

**何时阅读**: 仅作为历史参考，不建议使用

---

### 技术文档

#### [`ARCHITECTURE.md`](ARCHITECTURE.md)
**目标读者**: 开发者  
**内容**:
- 技术栈
- 架构模式
- 模块划分
- 数据流
- 依赖关系

**何时阅读**: 需要理解整体架构时

---

#### [`UUID_ENCODING.md`](UUID_ENCODING.md)
**目标读者**: 核心开发者  
**内容**:
- UUID 编码原理
- 逆向方法
- 参数映射
- 编码示例
- 调整步骤

**何时阅读**: ⚠️ 必读！需要调整 BLE 广播编码时

---

#### [`BUILD_GUIDE.md`](BUILD_GUIDE.md)
**目标读者**: 开发者  
**内容**:
- 环境配置
- 编译步骤
- 部署方法
- 常见问题

**何时阅读**: 第一次编译项目时

---

### 参考文档

#### [`PROJECT_GUIDE.md`](PROJECT_GUIDE.md)
**目标读者**: 开发者  
**内容**: 完整的项目指南（较旧，部分内容已过时）

**何时阅读**: 需要详细了解项目时（建议优先阅读 AI_AGENT_GUIDE.md）

---

#### [`UI_REDESIGN_SUMMARY.md`](UI_REDESIGN_SUMMARY.md)
**目标读者**: 了解历史  
**内容**:
- UI 重构目标
- 已完成的工作
- 设计对比
- 文件变更清单
- 技术细节
- 修复的问题

**何时阅读**: 想了解 UI 重构过程时

---

#### [`CRASH_FIX.md`](CRASH_FIX.md)
**目标读者**: 问题排查  
**内容**:
- 已修复的问题
- 修复步骤
- 常见崩溃原因
- 检查清单

**何时阅读**: 遇到崩溃或编译错误时

---

#### [`DEBUG_TIPS.md`](DEBUG_TIPS.md)
**目标读者**: 开发者  
**内容**: 调试技巧和工具使用

**何时阅读**: 需要调试时

---

#### [`QUICKSTART.md`](QUICKSTART.md)
**目标读者**: 新手  
**内容**: 快速上手指南

**何时阅读**: 第一次使用时

---

## �️ 阅读路径推荐

### 路径 1: AI Agent 开发

```
1. AI_AGENT_GUIDE.md          (综合指南)
2. DESIGN_IOS_GLASSMORPHISM.md (设计系统)
3. UUID_ENCODING.md            (UUID 编码)
4. 开始开发
```

### 路径 2: 人类开发者入门

```
1. ../README.md                (项目介绍)
2. QUICKSTART.md               (快速开始)
3. BUILD_GUIDE.md              (编译部署)
4. ARCHITECTURE.md             (架构理解)
5. DESIGN_IOS_GLASSMORPHISM.md (UI 开发)
6. UUID_ENCODING.md            (核心功能)
```

### 路径 3: UI 设计师

```
1. ../README.md                (项目介绍)
2. DESIGN_IOS_GLASSMORPHISM.md (设计系统)
3. UI_REDESIGN_SUMMARY.md      (设计演变)
```

### 路径 4: 问题排查

```
1. CRASH_FIX.md                (已知问题)
2. DEBUG_TIPS.md               (调试技巧)
3. AI_AGENT_GUIDE.md           (快速参考)
```

---

## 📊 文档状态

| 文档 | 状态 | 最后更新 | 维护者 |
|------|------|---------|--------|
| `AI_AGENT_GUIDE.md` | ✅ 最新 | 2026-05-28 | AI Agent |
| `README.md` | ✅ 最新 | 2026-05-28 | AI Agent |
| `DESIGN_IOS_GLASSMORPHISM.md` | ✅ 最新 | 2026-05-28 | AI Agent |
| `UUID_ENCODING.md` | ✅ 最新 | 2026-05-28 | - |
| `UI_REDESIGN_SUMMARY.md` | ✅ 最新 | 2026-05-28 | AI Agent |
| `CRASH_FIX.md` | ✅ 最新 | 2026-05-28 | AI Agent |
| `ARCHITECTURE.md` | ⚠️ 部分过时 | - | - |
| `PROJECT_GUIDE.md` | ⚠️ 部分过时 | - | - |
| `BUILD_GUIDE.md` | ✅ 有效 | - | - |
| `DEBUG_TIPS.md` | ✅ 有效 | - | - |
| `QUICKSTART.md` | ✅ 有效 | - | - |
| `DESIGN_CONSTRUCTIVISM.md` | ❌ 已废弃 | - | - |

---

## 🔄 文档维护

### 更新原则

1. **重大变更必须更新文档**
2. **优先更新 AI_AGENT_GUIDE.md**
3. **保持文档同步**
4. **标注更新日期**

### 需要更新的情况

- ✅ 添加新功能
- ✅ 修改架构
- ✅ 更改设计系统
- ✅ 修复重要问题
- ✅ 调整 UUID 编码
- ✅ 重构代码

### 文档优先级

1. **AI_AGENT_GUIDE.md** - 最高优先级
2. **README.md** - 高优先级
3. **DESIGN_IOS_GLASSMORPHISM.md** - 高优先级
4. **UUID_ENCODING.md** - 高优先级
5. 其他文档 - 按需更新

---

## 📞 获取帮助

### 文档问题

如果文档有错误、过时或不清楚：
1. 检查文档状态（见上方表格）
2. 查看相关的其他文档
3. 更新文档内容

### 技术问题

1. 先查看 `CRASH_FIX.md`
2. 再查看 `DEBUG_TIPS.md`
3. 最后查看 `AI_AGENT_GUIDE.md` 的问题排查章节

---

## 🎯 文档改进建议

### 待完善的文档

- [ ] 更新 `ARCHITECTURE.md` 以反映当前架构
- [ ] 更新 `PROJECT_GUIDE.md` 以反映 iOS 设计
- [ ] 添加更多调试技巧到 `DEBUG_TIPS.md`
- [ ] 添加性能优化文档
- [ ] 添加测试文档

### 可以删除的文档

- `DESIGN_CONSTRUCTIVISM.md` - 已废弃的设计系统
- 或保留作为历史参考

---

**维护**: 请在更新文档后同步更新本索引  
**版本**: 1.0.0  
**最后更新**: 2026-05-28

---

*本索引由 AI Agent 生成和维护*
