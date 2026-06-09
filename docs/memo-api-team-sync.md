# Memo API 팀 합의 필요 사항

`feat/memo-api` 브랜치에서 Memo/MemoSummary API 첫 구현을 하며 다른 파트와 맞춰야 할 지점을 정리한다.

## BE-A 인증/사용자

- 현재 Memo API는 인증 구현 전이라 `FixedMemoUserContext`에서 임시로 `userId=1`을 사용한다.
- BE-A의 JWT 인증이 붙으면 `MemoUserContext` 구현을 현재 인증 사용자 ID 조회 방식으로 교체해야 한다.
- 교체 시 Memo API의 사용자 소유권 기준은 `userId`로 유지한다.

## 실록이 LLM 연동

- `MemoSummaryGenerator`는 Memo 도메인이 LLM 구현을 직접 알지 않도록 둔 포트다.
- 현재 `DefaultMemoSummaryGenerator`는 실제 LLM 호출 전까지 쓰는 기본 구현이다.
- 공유 `silok` 모듈에서 실제 요약 프롬프트/모델/응답 파싱이 확정되면 이 구현만 교체한다.
- MemoSummary 응답은 OpenAPI 기준 `categoryName`, `summary`, `items`, `chatAvailable`, `chatDisabledReason`를 내려야 한다.

## DB 스키마/마이그레이션

- 이번 구현은 JPA 엔티티만 추가했고 마이그레이션 파일은 만들지 않았다.
- DB 스키마 변경은 AGENTS.md의 AI 금지 작업에 해당하므로 팀 승인 후 진행한다.
- 필요한 테이블 후보:
  - `memos`
  - `memo_summaries`
  - `memo_summary_items`
- `Memo`는 `serviceDate`를 저장해 06:00 KST 기준 날짜 조회를 고정한다.

## API 계약/FE

- 최신 OpenAPI 기준 Memo 수정 API는 MVP 범위에서 빠졌다.
- MemoSummary 목록 조회는 `GET /api/memo-summaries`다. `GET /api/memos/summaries`가 아니다.
- MemoSummary 수정/삭제 API는 MVP 범위가 아니므로 추가하지 않는다.
- `POST /api/memos/summaries`는 요약 후 원본 Memo를 `SUMMARIZED`로 바꾸고 새 빈 `DRAFT` Memo를 반환한다.
- 요약할 `DRAFT` Memo가 없으면 `422`, `code=NO_SUMMARIZABLE_MEMO`로 응답한다.
- `MemoSummaryItem.memoId`는 서버 내부 추적용이고 API 응답에는 포함하지 않는다.
- 이미 DailyChatSession에 사용된 MemoSummary는 `chatAvailable=false`, `chatDisabledReason=ALREADY_CHATTED`로 내려야 한다. 실제 사용 처리 시점은 conversation 도메인과 맞춘다.

## 도메인 정책

- 날짜 기본값은 `LocalDate.now()`가 아니라 `ServiceDay.today(clock)`으로 계산한다.
- 06:00 KST 이전 기록은 전날 서비스 날짜에 속한다.
- `DRAFT` 상태 Memo만 수정/요약할 수 있다.
- `ARCHIVED` 전환은 06:00 KST 일일 작업 공간 초기화 흐름에서 별도로 합의해야 한다.
