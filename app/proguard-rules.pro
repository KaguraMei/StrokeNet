# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# ============================================================
# StrokeNet 项目特定规则
# ============================================================

# 保留 BLE 相关类（核心功能）
-keep class aya.strokenet.BleAdvertiser { *; }
-keep class aya.strokenet.BleService { *; }
-keep class aya.strokenet.MainActivity { *; }

# 保留数据模型类（用于 Intent 传递）
-keep class aya.strokenet.data.model.** { *; }

# 保留 Compose 相关（UI 框架）
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# 保留 Kotlin 相关
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }
-dontwarn kotlin.**
-dontwarn kotlinx.**

# 保留 Android 系统服务相关
-keep class android.bluetooth.** { *; }
-keep class android.app.Service { *; }
-keep class android.content.Intent { *; }

# 保留枚举类
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# 保留 Parcelable 实现
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# 保留序列化相关
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# 移除日志（Release 版本）
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}
