#!/usr/bin/env bash
# 兼容入口：构建全平台产物（含 BIDS jar）
exec "$(dirname "$0")/build-yeds-artifacts.sh"
