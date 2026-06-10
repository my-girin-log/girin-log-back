# 로컬 실제 구동 검증 스크립트

실제 PostgreSQL + 실제 Gemini로 전체 사용자 시나리오(온보딩→메모→요약→대화→회고)를
손으로 돌려보기 위한 헬퍼다. (자동 E2E는 `FullJourneyE2ETest` 가 별도로 커버)

## 준비

1. PostgreSQL 기동
   ```bash
   docker run -d --name girinlog-pg -e POSTGRES_DB=girinlog -e POSTGRES_USER=girinlog \
     -e POSTGRES_PASSWORD=girinlog -p 5432:5432 postgres:16
   ```
2. 환경변수 채우기
   ```bash
   cp .env.example .env.local   # .env.local 은 gitignore
   # GIRINLOG_JWT_SECRET=$(openssl rand -base64 48)
   # GitHub OAuth App 의 Client ID/Secret, GEMINI_API_KEY 입력
   ```

## 실행

```bash
# 1) 앱 기동 (.env.local 주입 + bootRun)
bash scripts/run-local.sh

# 2) 브라우저로 http://localhost:8080/api/auth/github 로그인
#    → 리다이렉트 주소창의 #token=... 값 복사

# 3) 시나리오 검증 (실제 Gemini 호출)
bash scripts/verify-scenario.sh <복사한토큰>
```

## 참고

- Diary 생성은 06:00 배치(`DailyResetScheduler`)에 의존하므로 라이브에서는 제외하고
  자동 E2E(`FullJourneyE2ETest`)가 `runDailyReset` 직접 호출로 검증한다.
- Gemini 키에 따라 `gemini-2.0-flash` 무료 티어가 막혀(429) 있으면 `.env.local` 에서
  `GEMINI_MODEL` 을 비-thinking 계열(`gemini-flash-lite-latest`)로 오버라이드한다.
  thinking 모델은 작은 `maxOutputTokens`(첫 질문 512 / 끝 판단 64) 예산을 사고에 소모해
  JSON 응답이 잘릴 수 있다.
- 정리: `lsof -tiTCP:8080 -sTCP:LISTEN | xargs kill` · `docker rm -f girinlog-pg`
