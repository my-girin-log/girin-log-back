#!/usr/bin/env bash
# BE 레포 전용 보호 브랜치 가드 (codex harness 골격을 이 레포에 맞게 조정).
# codex 원본은 "현재 브랜치가 보호 대상이면 모든 Bash 차단"이라 git status/checkout 까지 막힌다.
# 여기서는 보호 브랜치(main 등)에서 git commit / git push 만 차단한다. 나머지는 통과.
# exit 0 = 통과, exit 2 = 차단(에이전트에 사유 전달).
set -euo pipefail

PROTECTED_REGEX='^(main|master|develop)$'
CURRENT="$(git rev-parse --abbrev-ref HEAD 2>/dev/null || echo '')"

# 보호 브랜치가 아니면 무조건 통과.
if ! [[ "$CURRENT" =~ $PROTECTED_REGEX ]]; then
  exit 0
fi

# PreToolUse 입력 JSON(stdin)에서 실행될 명령을 추출한다.
INPUT="$(cat)"
COMMAND="$(printf '%s' "$INPUT" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("tool_input",{}).get("command",""))' 2>/dev/null || echo '')"

# git commit / git push 만 차단. (checkout·status·add·log 등은 허용 → 브랜치를 만들어 빠져나갈 수 있게)
if printf '%s' "$COMMAND" | grep -Eq 'git[[:space:]]+(commit|push)\b'; then
  echo "보호 브랜치($CURRENT)에 직접 commit/push 가 감지되었습니다." >&2
  echo "작업 브랜치를 만들어 진행하세요: git checkout -b <type>/<설명>" >&2
  exit 2
fi

exit 0
