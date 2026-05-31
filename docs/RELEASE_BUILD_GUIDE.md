# StrokeNet 正式版 APK 打包指南

## 📦 打包步骤

### 方式一：使用 Android Studio（推荐）

#### 1. 生成签名密钥（首次打包）

如果还没有签名密钥，需要先生成一个：

**在 Android Studio 中：**
1. 菜单栏：`Build` → `Generate Signed Bundle / APK...`
2. 选择 `APK`，点击 `Next`
3. 点击 `Create new...` 创建新密钥库
4. 填写信息：
   - **Key store path**: 选择保存位置（建议：`C:\Users\你的用户名\.android\strokenet-release.jks`）
   - **Password**: 设置密钥库密码（请牢记！）
   - **Alias**: 密钥别名（例如：`strokenet`）
   - **Password**: 密钥密码（可以与密钥库密码相同）
   - **Validity**: 有效期（建议：25 年或更长）
   - **Certificate**: 填写你的信息
     - First and Last Name: 你的名字
     - Organizational Unit: 组织单位（可选）
     - Organization: 组织名称（可选）
     - City or Locality: 城市
     - State or Province: 省份
     - Country Code: 国家代码（CN）
5. 点击 `OK` 生成密钥库

**⚠️ 重要提示**：
- 密钥库文件（.jks）和密码必须妥善保管
- 丢失密钥库将无法更新已发布的应用
- 建议备份密钥库到安全位置

#### 2. 打包签名 APK

**方式 A：通过 Android Studio 图形界面**

1. 菜单栏：`Build` → `Generate Signed Bundle / APK...`
2. 选择 `APK`，点击 `Next`
3. 选择密钥库文件，输入密码和别名
4. 点击 `Next`
5. 选择构建类型：
   - **Destination Folder**: 输出目录（默认：`app/release/`）
   - **Build Variants**: 选择 `release`
   - **Signature Versions**: 勾选 `V1` 和 `V2`（推荐都勾选）
6. 点击 `Finish`

等待构建完成，APK 将生成在 `app/release/app-release.apk`

**方式 B：通过命令行（需要先配置签名）**

见下方"方式二：使用命令行"

---

### 方式二：使用命令行

#### 1. 配置签名信息

创建 `keystore.properties` 文件（不要提交到 Git）：

```bash
# 在项目根目录创建 keystore.properties
```

**keystore.properties 内容：**
```properties
storePassword=你的密钥库密码
keyPassword=你的密钥密码
keyAlias=strokenet
storeFile=C:/Users/你的用户名/.android/strokenet-release.jks
```

**⚠️ 安全提示**：
- 将 `keystore.properties` 添加到 `.gitignore`
- 不要将密码提交到版本控制系统

#### 2. 更新 build.gradle.kts

在 `app/build.gradle.kts` 中添加签名配置（已为你准备好，见下方）

#### 3. 执行打包命令

```bash
# 清理旧的构建文件
./gradlew clean

# 构建 Release APK
./gradlew assembleRelease

# 构建完成后，APK 位于：
# app/build/outputs/apk/release/app-release.apk
```

---

## 🔧 build.gradle.kts 签名配置

在 `app/build.gradle.kts` 的 `android` 块中添加：

```kotlin
android {
    // ... 其他配置 ...
    
    // 读取签名配置
    signingConfigs {
        create("release") {
            val keystorePropertiesFile = rootProject.file("keystore.properties")
            if (keystorePropertiesFile.exists()) {
                val keystoreProperties = java.util.Properties()
                keystoreProperties.load(java.io.FileInputStream(keystorePropertiesFile))
                
                storeFile = file(keystoreProperties["storeFile"] as String)
                storePassword = keystoreProperties["storePassword"] as String
                keyAlias = keystoreProperties["keyAlias"] as String
                keyPassword = keystoreProperties["keyPassword"] as String
            }
        }
    }
    
    buildTypes {
        release {
            isMinifyEnabled = true  // 启用代码混淆
            isShrinkResources = true  // 启用资源压缩
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")  // 使用 release 签名
        }
    }
}
```

---

## 📋 打包前检查清单

- [ ] 图标已正确放置（`app/src/main/res/mipmap-*`）
- [ ] 版本号已更新（`versionCode` 和 `versionName`）
- [ ] 应用名称正确（`app/src/main/res/values/strings.xml`）
- [ ] 权限声明正确（`AndroidManifest.xml`）
- [ ] 已生成签名密钥
- [ ] 签名配置已设置（如果使用命令行）
- [ ] 已测试 Debug 版本功能正常

---

## 🚀 快速打包命令（推荐）

如果你已经配置好签名，使用以下命令一键打包：

```bash
# Windows PowerShell
./gradlew clean assembleRelease

# 打包完成后，APK 位于：
# app/build/outputs/apk/release/app-release.apk
```

---

## 📱 安装测试

```bash
# 卸载旧版本（如果已安装）
adb uninstall aya.strokenet

# 安装 Release APK
adb install app/build/outputs/apk/release/app-release.apk

# 授予权限
adb shell pm grant aya.strokenet android.permission.BLUETOOTH_ADVERTISE
adb shell pm grant aya.strokenet android.permission.BLUETOOTH_CONNECT
adb shell pm grant aya.strokenet android.permission.ACCESS_FINE_LOCATION
```

---

## 🔍 验证签名

```bash
# 查看 APK 签名信息
keytool -printcert -jarfile app/build/outputs/apk/release/app-release.apk

# 或使用 apksigner（Android SDK 自带）
apksigner verify --verbose app/build/outputs/apk/release/app-release.apk
```

---

## 📊 版本管理

每次发布新版本时，记得更新 `app/build.gradle.kts` 中的版本信息：

```kotlin
defaultConfig {
    versionCode = 2  // 每次发布递增（整数）
    versionName = "1.1"  // 版本名称（字符串）
}
```

**版本号规则**：
- `versionCode`: 整数，每次发布必须递增（用于 Google Play 等应用商店判断版本）
- `versionName`: 字符串，显示给用户的版本号（例如：1.0, 1.1, 2.0）

---

## 🛡️ ProGuard 混淆规则

如果启用了代码混淆（`isMinifyEnabled = true`），可能需要在 `app/proguard-rules.pro` 中添加规则：

```proguard
# 保留 BLE 相关类
-keep class aya.strokenet.BleAdvertiser { *; }
-keep class aya.strokenet.BleService { *; }

# 保留数据模型类
-keep class aya.strokenet.data.model.** { *; }

# 保留 Compose 相关
-keep class androidx.compose.** { *; }
```

---

## 📦 APK 大小优化

如果需要减小 APK 体积：

1. **启用资源压缩**（已在上面的配置中）
   ```kotlin
   isShrinkResources = true
   ```

2. **移除未使用的资源**
   - 删除 `res/` 目录中未使用的图片、布局等

3. **使用 WebP 格式图标**
   - 将 PNG 图标转换为 WebP（Android Studio 自带工具）

4. **分离架构 APK**（高级）
   ```kotlin
   splits {
       abi {
           isEnable = true
           reset()
           include("armeabi-v7a", "arm64-v8a", "x86", "x86_64")
           isUniversalApk = false
       }
   }
   ```

---

## 🎯 常见问题

### Q: 打包时提示"找不到签名配置"
**A**: 检查 `keystore.properties` 文件是否存在，路径是否正确。

### Q: 安装时提示"签名不一致"
**A**: 卸载旧版本后重新安装，或使用相同的签名密钥。

### Q: APK 安装后闪退
**A**: 检查 ProGuard 规则，可能混淆了不该混淆的类。

### Q: 如何生成 AAB（Android App Bundle）
**A**: 使用 `./gradlew bundleRelease`，输出在 `app/build/outputs/bundle/release/`

---

## 📄 相关文档

- [BUILD_GUIDE.md](BUILD_GUIDE.md) - 开发构建指南
- [README.md](../README.md) - 项目总览
- [ARCHITECTURE.md](ARCHITECTURE.md) - 架构设计

---

**最后更新**: 2026-05-29
