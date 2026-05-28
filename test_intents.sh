#!/bin/bash

# StrokeNet Intent 测试脚本
# 用于快速测试各种控制指令

PACKAGE="com.ec.strokenet"
ACTIVITY=".MainActivity"

echo "StrokeNet 控制测试脚本"
echo "======================="
echo ""

# 检查设备连接
if ! adb devices | grep -q "device$"; then
    echo "错误: 没有检测到 Android 设备"
    echo "请确保设备已连接并启用 USB 调试"
    exit 1
fi

echo "设备已连接"
echo ""

# 显示菜单
echo "请选择测试项目:"
echo "1. 启动推拉 (depth=36, extend=8, retract=8)"
echo "2. 启动推拉 (depth=50, extend=12, retract=10)"
echo "3. 调节推拉 (depth=20, extend=5, retract=5)"
echo "4. 设置强度 50"
echo "5. 设置强度 80"
echo "6. 设置温度 30°C"
echo "7. 设置温度 45°C"
echo "8. 停止"
echo "9. 自定义参数"
echo "0. 退出"
echo ""

read -p "请输入选项 (0-9): " choice

case $choice in
    1)
        echo "发送: 启动推拉 (depth=36, extend=8, retract=8)"
        adb shell am start -n "$PACKAGE/$ACTIVITY" \
            --es action start \
            --ei depth 36 \
            --ei extend 8 \
            --ei retract 8
        ;;
    2)
        echo "发送: 启动推拉 (depth=50, extend=12, retract=10)"
        adb shell am start -n "$PACKAGE/$ACTIVITY" \
            --es action start \
            --ei depth 50 \
            --ei extend 12 \
            --ei retract 10
        ;;
    3)
        echo "发送: 调节推拉 (depth=20, extend=5, retract=5)"
        adb shell am start -n "$PACKAGE/$ACTIVITY" \
            --es action thrust \
            --ei depth 20 \
            --ei extend 5 \
            --ei retract 5
        ;;
    4)
        echo "发送: 设置强度 50"
        adb shell am start -n "$PACKAGE/$ACTIVITY" \
            --es action strength \
            --ei value 50
        ;;
    5)
        echo "发送: 设置强度 80"
        adb shell am start -n "$PACKAGE/$ACTIVITY" \
            --es action strength \
            --ei value 80
        ;;
    6)
        echo "发送: 设置温度 30°C"
        adb shell am start -n "$PACKAGE/$ACTIVITY" \
            --es action temp \
            --ei value 30
        ;;
    7)
        echo "发送: 设置温度 45°C"
        adb shell am start -n "$PACKAGE/$ACTIVITY" \
            --es action temp \
            --ei value 45
        ;;
    8)
        echo "发送: 停止"
        adb shell am start -n "$PACKAGE/$ACTIVITY" \
            --es action stop
        ;;
    9)
        echo "自定义参数"
        read -p "操作类型 (start/thrust/strength/temp/stop): " action
        
        if [ "$action" = "start" ] || [ "$action" = "thrust" ]; then
            read -p "深度 (0-72): " depth
            read -p "伸出速度 (0-15): " extend
            read -p "缩回速度 (0-15): " retract
            adb shell am start -n "$PACKAGE/$ACTIVITY" \
                --es action "$action" \
                --ei depth "$depth" \
                --ei extend "$extend" \
                --ei retract "$retract"
        elif [ "$action" = "strength" ] || [ "$action" = "temp" ]; then
            read -p "数值: " value
            adb shell am start -n "$PACKAGE/$ACTIVITY" \
                --es action "$action" \
                --ei value "$value"
        elif [ "$action" = "stop" ]; then
            adb shell am start -n "$PACKAGE/$ACTIVITY" \
                --es action stop
        else
            echo "无效的操作类型"
            exit 1
        fi
        ;;
    0)
        echo "退出"
        exit 0
        ;;
    *)
        echo "无效的选项"
        exit 1
        ;;
esac

echo ""
echo "指令已发送"
echo ""
echo "查看日志:"
echo "adb logcat | grep BleAdvertiser"
