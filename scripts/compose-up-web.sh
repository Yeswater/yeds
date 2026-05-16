#!/usr/bin/env sh
set -e
cd "$(dirname "$0")/.."
docker compose build --pull=false mysql bids-config bids-export bids-exec bids-web
docker compose up -d bids-web
