# girin-log-back

내가그린기린기록 **백엔드** 레포 (Spring Boot · Java 21 · Gradle).

계약·규칙·용어의 단일 진실(SSOT)은 [girin-codex](https://github.com/my-girin-log/girin-codex) 에 있고,
이 레포는 그걸 **`vendor/codex` git submodule** 로 참조해서 구현한다.

> 작업 지침은 [`AGENTS.md`](AGENTS.md) 를 따른다. (모든 AI 작업자 공통 정본)

---

## 처음 클론할 때 (submodule 포함)

`vendor/codex` 는 submodule 이라, 그냥 `git clone` 만 하면 **빈 폴더**로 받아진다.
반드시 submodule 까지 같이 받아야 계약(`openapi.yaml`)·규칙·hook 이 채워진다.

### 방법 1 — 한 번에 (권장)

```bash
git clone --recurse-submodules https://github.com/my-girin-log/girin-log-back.git
cd girin-log-back
```

### 방법 2 — 이미 클론했다면

```bash
git clone https://github.com/my-girin-log/girin-log-back.git
cd girin-log-back
git submodule update --init --recursive
```

받아지면 `vendor/codex/api/openapi.yaml`, `vendor/codex/conventions/` 등이 보여야 한다.

---

## 하네스(codex) 최신으로 동기화

codex 의 계약/규칙이 바뀌면(= 팀이 PR 머지하면) 끌어온다.

```bash
git submodule update --remote vendor/codex   # 최신 codex main 으로 갱신
git add vendor/codex && git commit -m "chore: codex 하네스 동기화"
```

> submodule 은 "특정 커밋"을 가리킨다. 위 명령으로 갱신한 뒤 커밋해야 팀 전체가 같은 codex 버전을 본다.
> 평소 `git pull` 만으로는 submodule 이 자동으로 안 따라온다. pull 후 `git submodule update` 를 함께 돌린다.

### submodule 수정 주의

- `vendor/codex` 내부 파일은 이 레포에서 직접 수정하지 않는다.
- 계약 변경은 [girin-codex](https://github.com/my-girin-log/girin-codex) 에 별도 브랜치/PR로 올린다.
- 이 레포에서는 의도한 경우에만 `vendor/codex` 포인터 변경을 커밋한다.
- 확인:

```bash
git -C vendor/codex status
git diff --submodule=log -- vendor/codex
```

---

## 빌드 / 실행

```bash
./gradlew build         # 컴파일 + 테스트
./gradlew test          # 테스트만
./gradlew bootRun       # 로컬 실행 (DB·OAuth 환경변수 필요)
```

`bootRun` 은 PostgreSQL 연결이 필요하다(`application.yml` 참고, 값은 환경변수 주입).
빌드/단위 테스트는 DB 없이 통과한다.

---

## 디렉터리 (도메인 우선 분할)

```
src/main/java/com/girinlog/
  auth/  persona/                                       # BE-A: 사용자 정체성·말투
  memo/  conversation/  diary/  retrospective/  event/  # BE-B: 일일 기록→Diary→Retrospective
  silok/                                                # 공유: 실록이 LLM 연동
  common/                                               # 공유: 에러 envelope, KST 시각 유틸, 보안
vendor/codex/                                           # SSOT (submodule): 계약·규칙·hook
```

---

## 협업 규칙 (요약 — 상세는 AGENTS.md)

- `main` 직접 push 금지. **작업 브랜치 → PR → 머지**. (hook 이 main 직접 commit/push 를 차단)
- 명세에 없는 엔드포인트/필드를 추측해 추가하지 않는다. `vendor/codex/api/openapi.yaml` 에 **먼저** 합의·반영.
- 용어는 `vendor/codex/conventions/glossary.md` 를 그대로 쓴다.
- 일자 경계는 **06:00 KST**. 커밋 메시지는 **한국어**.

---

## 프론트엔드 작업자도 동일

프론트엔드 레포도 같은 방식으로 `vendor/codex` 를 submodule 로 둔다.
클론·동기화 git 명령(위 "처음 클론할 때" / "동기화")은 **레포만 다를 뿐 완전히 동일**하다.
FE 는 받은 `vendor/codex/api/openapi.yaml` 로 타입을 생성해 호출하고, BE 는 그 명세대로 구현한다.
