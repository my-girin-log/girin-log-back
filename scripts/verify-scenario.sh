#!/usr/bin/env bash
# 실제 사용자 시나리오 라이브 검증 (실제 Gemini 호출).
# 온보딩 → persona → 메모 → 요약 → 대화 시작 → 답변 → 종료 → 회고 생성/조회 → 목록.
# Diary(06:00 배치)는 자동 E2E(FullJourneyE2ETest)로 검증하므로 여기서는 제외.
#
# 사용:
#   1) 다른 터미널에서 scripts/run-local.sh 로 앱 기동
#   2) 브라우저로 http://localhost:8080/api/auth/github 로그인
#      → 마지막 리다이렉트 주소창의 #token=... 값 복사
#   3) bash scripts/verify-scenario.sh <복사한토큰>
set -euo pipefail
BASE="${BASE:-http://localhost:8080}"
TOKEN="${1:-}"
if [[ -z "$TOKEN" ]]; then
  echo "사용법: bash scripts/verify-scenario.sh <accessToken>" >&2
  exit 1
fi
AUTH="Authorization: Bearer ${TOKEN}"
JSON="Content-Type: application/json"
TODAY="$(date +%F)"

# call <method> <path> [jsonBody] → 본문 출력, 비정상 status면 중단
call() {
  local method="$1" path="$2" data="${3:-}"
  local args=(-sS -X "$method" -H "$AUTH" -w '\n__STATUS__%{http_code}')
  [[ -n "$data" ]] && args+=(-H "$JSON" -d "$data")
  local out status body
  out="$(curl "${args[@]}" "${BASE}${path}")"
  status="${out##*__STATUS__}"
  body="${out%__STATUS__*}"
  echo "  → ${method} ${path}  [${status}]"
  echo "$body" | jq . 2>/dev/null || echo "$body"
  if [[ ! "$status" =~ ^2 ]]; then
    echo "[FAIL] 예상치 못한 status ${status}" >&2
    exit 1
  fi
  LAST_BODY="$body"
}

echo "== 1) 온보딩 → Persona 생성 =="
call POST /api/onboarding/submissions '{"answers":[{"questionId":1,"answer":"하루를 순서대로 정리하는 편이에요"}]}'

echo "== 2) Persona 조회 =="
call GET /api/personas/me

echo "== 3) 메모 작성 =="
call POST /api/memos '{"content":"오늘 E2E 라이브 검증을 했고 회고 기능을 점검했다"}'

echo "== 4) 요약 → MemoSummary =="
call POST /api/memos/summaries '{}'
SUMMARY_ID="$(echo "$LAST_BODY" | jq -r '.memoSummaries[0].id')"
echo "  memoSummaryId=${SUMMARY_ID}"

echo "== 5) 대화 시작 =="
call POST /api/daily-chat-sessions "{\"memoSummaryIds\":[${SUMMARY_ID}]}"
SESSION_ID="$(echo "$LAST_BODY" | jq -r '.sessionId')"
echo "  sessionId=${SESSION_ID}"

echo "== 6) 답변 → 역질문 이어짐 =="
call POST "/api/daily-chat-sessions/${SESSION_ID}/answers" '{"content":"검증이 잘 되어서 뿌듯했어요"}'

echo "== 7) 끝내기 → ENDED =="
call POST "/api/daily-chat-sessions/${SESSION_ID}/end"

echo "== 8) 회고 생성 (오늘~오늘) =="
call POST /api/retrospectives "{\"startDate\":\"${TODAY}\",\"endDate\":\"${TODAY}\"}"
RETRO_ID="$(echo "$LAST_BODY" | jq -r '.id')"
echo "  retrospectiveId=${RETRO_ID}"

echo "== 9) 회고 조회 =="
call GET "/api/retrospectives/${RETRO_ID}"

echo "== 10) 목록 조회 (커서/날짜 파라미터 없음) =="
call GET /api/retrospectives
call GET /api/diaries

echo
echo "[DONE] 라이브 시나리오 통과. (Diary 생성은 자동 E2E가 커버)"
