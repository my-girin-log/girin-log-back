# CLAUDE.md — girin-log-back

이 레포의 공통 AI 작업 지침은 **`AGENTS.md`** 를 따른다. (모든 AI 작업자의 정본)

Claude Code는 작업 전 아래 순서로 확인한다.

1. `AGENTS.md` — 이 레포의 정본 지침
2. `vendor/codex/AGENTS.md` · `vendor/codex/requirements/product.md`
3. `vendor/codex/domain/data-model.md`
4. `vendor/codex/api/openapi.yaml` — API 계약의 최종 진실
5. `vendor/codex/conventions/` (glossary · api · coding)

충돌 시 `vendor/codex/api/openapi.yaml` 이 API 계약의 최종 진실이다.
강제 hook 은 `.claude/settings.json` 에 설정돼 있다.
