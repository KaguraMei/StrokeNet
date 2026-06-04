欢迎光临罗德岛工程部！我是本舰的首席工程师可露希尔~ 给你带来全新升级的 **StrokeNet 2.0**！嘿嘿，这可是花了我不少心血的好东西哦！

> 罗德岛工程部特供：基于 BLE 广播协议的 Android 控制终端 | 内置官方 MCP SDK | 全新 AI 原生控制！

[![Android](https://img.shields.io/badge/Android-7%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-blue.svg)](https://kotlinlang.org)
[![MCP SDK](https://img.shields.io/badge/MCP_SDK-0.6.0-brightgreen.svg)](https://github.com/modelcontextprotocol/kotlin-sdk)
[![Compose](https://img.shields.io/badge/Compose-Latest-orange.svg)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

---

**项目状态**: ✅ **2.0 正式版全线发售！**  
**系统版本**: 2.0.0 | **MCP SDK**: kotlin-sdk-server 0.6.0 | **Android**: API 24+ (Android 7.0+)  
**最后更新**: 2026-06-03 （记得常来看看有没有更新哦，博士~）

## 🔗 采购中心快捷通道

博士，想要什么直接拿！别忘了点个 Star 赞助一下工程部哦~ ⭐
- 📱 [下载 APK](../../releases) - 最新批次的安装包都在这！
- 📖 [完整文档](docs/DOCS_INDEX.md) - PRTS 整理好的全套说明书
- 🚀 [快速开始](docs/MCP_QUICKSTART.md) - 5 分钟极速上手指南
- 🎨 [预设指南](docs/MCP_PRESET_GUIDE.md) - 教你怎么调制专属动作配方
- 💻 [开发指南](docs/AI_AGENT_GUIDE.md) - 给 AI 助理和高阶干员的硬核手册
- ⚠️ [UUID 协议](docs/UUID_ENCODING.md) - 本天才辛苦解析的正确编码协议，必看！

---

## ✨ 2.0 核心黑科技

### 🌐 罗德岛级内置 MCP Server
- ✅ 搭载官方 Kotlin MCP SDK 0.6.0，稳如泰山！
- ✅ 彻底抛弃 Termux，一键点火启动，多省事~
- ✅ 20+ 个 MCP 工具，从头到脚全方位控制能力
- ✅ 标准 HTTP Streamable 传输协议，数据跑得飞快！

### 🎨 自定义循环预设库
- ✅ 可视化面板，创建预设就像配制理智液一样简单！
- ✅ 多段动作自动无缝循环
- ✅ 支持 JSON 格式导入导出，好东西当然要和别的博士分享~
- ✅ 终端启动时自动从 PRTS 数据库加载所有预设

### 🔧 旗舰级功能包
- ✅ BLE 广播直连控制（直接绕过配对，霸气吧！）
- ✅ 前台服务强力保活 + 失败重试（切后台绝对不掉线！）
- ✅ 完整参数控制（推拉、震动、温度，全都在掌控之中）
- ✅ 加热定时器（1-10分钟自动断电，安全第一~）
- ✅ 专属 MCP 管理台（状态、工具、日志一目了然）
- ✅ **支持自定义 MCP `am` 命令式调起！** 随你怎么折腾都能对接上！

## 🚀 极速部署指南

### 编译安装（给喜欢折腾代码的博士）
```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
# 搞定啦！快去试试看~
```

### 打包正式版 APK
想要拿去量产的话，记得看这本说明书：[正式版打包指南](docs/RELEASE_BUILD_GUIDE.md) 📦

### 申请系统权限
博士，系统门禁还是得敲一下的，不然信号发不出去哦：
```bash
adb shell pm grant aya.strokenet android.permission.BLUETOOTH_ADVERTISE
adb shell pm grant aya.strokenet android.permission.BLUETOOTH_CONNECT
adb shell pm grant aya.strokenet android.permission.ACCESS_FINE_LOCATION
```

### 终端测试
```bash
# 启动推拉（注意看现在的参数标准哦！）
adb shell am start -n aya.strokenet/.MainActivity \
  --es action start --ei depth 50 --ei extend 50 --ei retract 50

# 紧急制动！
adb shell am start -n aya.strokenet/.MainActivity --es action stop
```

## 📖 资料室

### 🎯 快速导航
**👨‍💻 给你的 AI 助理（Agent）**: [`docs/AI_AGENT_GUIDE.md`](docs/AI_AGENT_GUIDE.md) ⭐⭐⭐⭐⭐  
把这个塞给 AI，它自己就知道该怎么做了！

**👤 给博士你阅读的**: 从本文档开始，然后去翻翻 [`docs/DOCS_INDEX.md`](docs/DOCS_INDEX.md)，里面有完整的索引。

### 📚 核心文档（挑重点看~）
- **[docs/MCP_QUICKSTART.md](docs/MCP_QUICKSTART.md)** - **5 分钟快速开始** ⚡⚡⚡
- **[docs/UUID_ENCODING.md](docs/UUID_ENCODING.md)** - UUID 编码协议详解 ⚠️ **必看！这可是最核心的机密！**
- **[docs/SERVICE_INTEGRATION.md](docs/SERVICE_INTEGRATION.md)** - BLE Service 集成说明

## 🔌 通讯控制协议

博士，注意听好啦！为了让你和 UI 界面操作起来更直观，**本天才把所有动作参数（除了加热时间和温度）全部统一标准化为 1-100 啦！**

设备底层的那些奇奇怪怪的硬件限制，我都已经在代码里帮你写好自动乘算和映射逻辑了！你在输入指令的时候，闭着眼睛填 1 到 100 的百分比就行啦，是不是很贴心？嘿嘿~

### 统一参数标准（UI & 入参规范）
| 参数 | 范围 | 说明 |
|------|------|------|
| depth | 1-100 | 推拉深度（已自动映射到底层逻辑） |
| extendSpeed | 1-100 | 伸出速度（已自动映射到底层逻辑） |
| retractSpeed | 1-100 | 缩回速度（已自动映射到底层逻辑） |
| strength | 1-100 | 入体端强度 |
| temp | 0-60 | 加热温度（℃，硬件绝对值）|

*(注：加热倒计时功能支持 1-10 分钟设定)*

### 🤖 自定义 MCP & Intent 命令式调起
除了界面操作和内置服务，现在系统**全面支持自定义 MCP `am` 命令式调起！** 所有的指令都可以通过 Intent 发送到 MainActivity，我会让前台服务帮你完美处理好一切：

```bash
# 启动推拉 (现在统一用 1-100 的直观数值啦！)
am start -n aya.strokenet/.MainActivity \
  --es action start --ei depth 80 --ei extend 50 --ei retract 50

# 运行中实时微调参数
am start -n aya.strokenet/.MainActivity \
  --es action thrust --ei depth 100 --ei extend 80 --ei retract 80

# 调节强度
am start -n aya.strokenet/.MainActivity \
  --es action strength --ei value 90

# 设定加热温度
am start -n aya.strokenet/.MainActivity \
  --es action temp --ei value 42

# 全面停机！
am start -n aya.strokenet/.MainActivity --es action stop
```

### 信号工作流
```
指令下达 (Intent / MCP)
    ↓
MainActivity 接收处理
    ↓
启动强力保活的 BleService (前台服务)
    ↓
呼叫 BleAdvertiser
    ↓
把 BLE 广播发射出去！
    ↓
没发成功？→ 自动重试！（最多2次，300ms极速间隔）
    ↓
终端通知栏实时汇报战况
    ↓
功成身退，自动休眠~
```

## 🔔 为什么一定要用前台服务？

博士你是不是想问，为什么要搞得这么复杂？
因为外面那些粗糙的官方 APP，只要一切换到后台，系统就会把它们的广播切断！设备直接卡死！我们罗德岛的工程部怎么能容忍这种低级失误？

所以我给 StrokeNet 加上了：
- ✅ **前台不死鸟保活**，就算你切出去看终端录像它也能继续跑！
- ✅ **智能防丢重试**（最多2次），确保每一次脉冲都精准传达。
- ✅ **通知栏实时战报**，随时了解设备运行状态。

## ⚠️ 核心机密提示

### 关于 UUID 编码的真相

听好了博士，本项目的 BLE 协议可是我完全基于官方终端逆向解析出来的成果！

这里得感谢一下 [用 AI 远程控制你的 Cachito 大秀炮机](https://claude.ai/public/artifacts/921eda06-e567-4cde-85af-8cde831a608f) 这篇报告提供的思路，它确实给了我做 MCP 集成的灵感。**但是！那篇文章里的 UUID 编码逻辑是有严重偏差的哦！**（参数位置和校验和算得都不对，官方设备可不吃那一套）。

为了不让你走弯路，**本天才已经把完全正确的编码规则写在** [docs/UUID_ENCODING.md](docs/UUID_ENCODING.md) 里啦！
- ✅ 完美的 UUID 格式：`710003XX-YYYY-ZZZZ-0000-WWWWWWWWWWCC`
- ✅ 精准的参数编码位置和真正的校验和计算机制！
  如果你也想自己造设备的话，一定记得以我的这份蓝图为准哦！

### 权限小贴士
Android 12 以上的系统越来越小气了，你得亲自去设置里给它批条子：
- 设置 → 应用 → StrokeNet → 权限
- 把 **蓝牙** 和 **位置信息（精确位置）** 都打开才行！

## 🤝 代理控制方案

### 方案 A：内置 MCP Server（推荐！🌐）
我都把好东西集成进去了，不需要 Termux，一键开启！延迟低，超级稳定！在你的 AI 助理（比如 Claude Desktop）里配置一下就能用啦：

```json
{
  "mcpServers": {
    "strokenet": {
      "url": "http://192.168.x.x:8080/mcp"
    }
  }
}
```

里面准备好了全套预设管理的指令（列出、创建、导入、运行...），翻翻 [MCP_PRESET_GUIDE.md](docs/MCP_PRESET_GUIDE.md) 就能全盘掌握！

### 方案 B：Termux + Python MCP（备用库）
如果是喜欢自己写 Python 脚本的高阶干员，我们依然保留了 1.0 时代的玩法。去翻看 **[Termux MCP 配置指南（1.0 版本留档）](docs/TERMUX_MCP_GUIDE.md)** 吧~

## 📄 许可证

MIT License - 随便拿去用，不过记得本天才的署名哦！详见 [LICENSE](LICENSE)。

## 🙏 致谢

最后，还是得按规矩致谢一下那篇《用 AI 远程控制你的 Cachito 大秀炮机：BLE 逆向 + MCP 全链路教程》，虽然代码逻辑有坑，但它关于 BLE 广播模式代替传统 GATT 连接的思路，还有 MCP 全链路的构想，确实非常精彩！

好啦，简报就到这里！博士，赶快去体验 StrokeNet 2.0 吧！如果用得开心，别忘了给罗德岛工程部（的项目仓库）多投几颗星星（Star）哦！有龙门币赞助就更完美啦！(￣▽￣)～■干杯□～(￣▽￣)