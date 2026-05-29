#!/bin/bash

# 快速启动脚本 - SSE 协议（Claude Desktop 旧版本）

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

echo "启动 StrokeNet MCP Server (SSE 协议)..."
./start.sh sse
