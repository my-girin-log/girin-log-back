#!/usr/bin/env bash
# 로컬 실제 구동: .env.local 을 읽어 환경변수 주입 후 bootRun.
# 사용: bash scripts/run-local.sh
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ ! -f .env.local ]]; then
  echo "[ERR] .env.local 이 없습니다. 먼저 값을 채워주세요." >&2
  exit 1
fi
# shellcheck disable=SC1091
source .env.local

missing=()
for v in GIRINLOG_JWT_SECRET GITHUB_CLIENT_ID GITHUB_CLIENT_SECRET GEMINI_API_KEY DB_PASSWORD; do
  [[ -z "${!v:-}" ]] && missing+=("$v")
done
if (( ${#missing[@]} )); then
  echo "[ERR] 비어있는 환경변수: ${missing[*]}" >&2
  echo "      .env.local 을 채운 뒤 다시 실행하세요." >&2
  exit 1
fi

echo "[OK] 환경변수 주입 완료. PostgreSQL(${DB_URL}) 에 연결합니다."
echo "[INFO] JPA_DDL_AUTO=${JPA_DDL_AUTO:-validate} (로컬 검증 전용)"
echo "[INFO] bootRun 시작 — 기동 후 브라우저로 http://localhost:8080/api/auth/github 접속해 로그인하세요."
exec ./gradlew bootRun
