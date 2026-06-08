# AGENTS.md — girin-log-back

내가그린기린기록 **백엔드** 레포에서 작업하는 **모든 AI 작업자(Codex · Claude Code · Cursor 등)의 공통 정본 지침**이다.
도구가 무엇이든 이 파일을 먼저 읽는다. (`CLAUDE.md` 등 도구별 파일은 이 문서를 가리키는 얇은 포인터다.)

계약·규칙·용어의 단일 진실(SSOT)은 `vendor/codex` (girin-codex, git submodule) 에 있다.
이 레포는 그걸 **참조해서 구현**한다.

> 한 줄 원칙: **`vendor/codex/api/openapi.yaml` 이 옳다. 코드가 명세와 다르면 틀린 건 코드다.**

## 작업 전 반드시 확인 (순서대로)

1. `vendor/codex/AGENTS.md` — 공통 에이전트 지침 · 절대 규칙 · 문서 우선순위
2. `vendor/codex/requirements/product.md` — 기능 요구사항
3. `vendor/codex/domain/data-model.md` — 엔티티 정의
4. `vendor/codex/api/openapi.yaml` — **API 계약의 최종 진실**
5. `vendor/codex/conventions/glossary.md` — 영문 용어 단일 진실
6. `vendor/codex/conventions/api.md` — 에러 envelope · 06:00 KST 경계 · 인증 · 상태코드
7. `vendor/codex/conventions/coding.md` — 스택 · 도메인 분담 · 디렉터리 · 객체 설계

충돌 시 우선순위는 `vendor/codex/AGENTS.md` 의 "문서 우선순위" 를 따른다.

## 스택

Spring Boot 3.4 · Java 21 · Gradle · Spring Data JPA · PostgreSQL.
테스트: JUnit 5 + AssertJ + Mockito, 통합은 Testcontainers PostgreSQL.

## 디렉터리 (도메인 우선 — 레이어로 나누지 않는다)

```
src/main/java/com/girinlog/
  auth/  persona/                      # BE-A: 사용자 정체성·말투
  memo/  conversation/  diary/  retrospective/  event/   # BE-B: 일일 기록→Diary→Retrospective
  silok/                               # 공유: 실록이 LLM 연동
  common/                              # 공유: 에러 envelope, KST 시각 유틸, 보안 설정
```

한 도메인 안에서만 `controller`/`service`/`domain`/`repository` 로 나눈다.
도메인 간 직접 의존 금지 — 공개 서비스 인터페이스로만 호출한다.

## 절대 규칙 (vendor/codex/AGENTS.md 발췌 — 어기면 hook 또는 리뷰에서 막힌다)

- 명세에 없는 필드/엔드포인트를 추측해 추가하지 않는다. `openapi.yaml` 에 **먼저** 합의·반영 후 구현.
- 용어는 glossary 를 그대로 쓴다(`Retrospective`, `DailyChatSession`, `silok` …). 축약·혼용 금지.
- 일자 경계는 **06:00 KST**. `LocalDate.now()` 를 타임존 없이 쓰지 말고 `common/time` 유틸을 통한다.
- 역질문은 **최대 10회**(서버 정책 + 프롬프트 양쪽). Diary 는 **하루 1개**.
- 에러는 `common/error` 의 고정 envelope 만 쓴다. 메시지 문자열로 분기하지 않고 `code` 로 분기.
- **AI 금지 작업(사용자 승인 필요):** DB 스키마 변경, 인증/인가 정책 변경, OpenAPI 변경, 도메인 정책 변경, 운영 설정 변경.
- 커밋 메시지는 한국어. `main` 직접 push 금지(작업 브랜치 사용).

## 강제 장치 (도구마다 따로 건다 — "지향"은 이 문서, "강제"는 hook)

| 도구 | 설정 파일 | 거는 hook |
| --- | --- | --- |
| Claude Code | `.claude/settings.json` | 포맷·스펙드리프트(PostToolUse) · 보호 브랜치 가드(PreToolUse) |
| OpenAI Codex | `.codex/hooks.json` | 스펙드리프트(PostToolUse) · 보호 브랜치 가드(PreToolUse) |

hook 스크립트 원본은 `vendor/codex/harness/hooks/`(공용), 보호 브랜치 가드만 이 레포용 조정본 `.claude/hooks/guard-protected-branch.sh` 를 쓴다.
hook 을 지원하지 않는 도구를 쓴다면, 위 "절대 규칙"과 `vendor/codex/conventions` 를 사람이 직접 지킨다.

## 명령

```bash
./gradlew build         # 컴파일 + 테스트
./gradlew test          # 테스트만
./gradlew bootRun       # 로컬 실행 (DB·OAuth 환경변수 필요)

git submodule update --remote vendor/codex   # 하네스 최신 계약 동기화
```

## "완료"의 정의 (vendor/codex/conventions/coding.md 9절)

1. 도메인 로직·핵심 정책 테스트 통과(회귀 보호)
2. 응답이 `vendor/codex/api/openapi.yaml` 명세와 일치(명세 준수 테스트)
3. glossary 용어 준수 + 설계 규칙 크게 벗어나지 않음
