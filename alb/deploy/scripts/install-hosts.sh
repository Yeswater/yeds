#!/usr/bin/env bash
set -euo pipefail

MARKER="# yeds-alb-hosts"
HOSTS_FILE="/etc/hosts"
DESIRED_LINE="127.0.0.1 yeds.local www.yeds.local yeds.com www.yeds.com localhost"

if grep -qF "${MARKER}" "${HOSTS_FILE}" 2>/dev/null; then
  if grep -qF "yeds.local" "${HOSTS_FILE}" 2>/dev/null; then
    echo "hosts 已包含 yeds.local（${MARKER}）"
  else
    echo "补全 yeds.local（需要 sudo）"
    sudo sh -c "printf '\n%s\n' '${DESIRED_LINE}' >> '${HOSTS_FILE}'"
    echo "已追加：${DESIRED_LINE}"
  fi
else
  echo "追加 YEDS hosts（需要 sudo）"
  sudo sh -c "printf '\n%s\n%s\n' '${MARKER}' '${DESIRED_LINE}' >> '${HOSTS_FILE}'"
  echo "完成。"
fi

echo ""
echo "推荐：http://localhost/iam/  或  http://yeds.local/iam/"
echo "验证：ping -c 1 yeds.local 应显示 127.0.0.1"
echo "Chrome 请关闭「使用安全 DNS」"
