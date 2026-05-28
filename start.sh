#!/bin/bash
cd ~/
python daxiu_mcp.py &           # 大秀 MCP
sleep 2
cloudflared tunnel --url http://localhost:3459
# cloudflared tunnel run --protocol http2 --token YOUR_TOKEN
# ↑ 这行会阻塞，所以必须放最后