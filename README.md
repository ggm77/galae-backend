# 갈래 Backend

**하나의 질문, 여러 갈래의 생각.**

일상의 딜레마에 먼저 나의 선택을 남기고, 다른 사람들의 생각과 성경을 포함한 여러 관점을 통해 판단을 돌아보는 **갈래** 서비스의 백엔드 리포지토리입니다. 프론트엔드는 [jeondowon/galae](https://github.com/jeondowon/galae)이며, 화면과 규칙은 프론트의 `docs/APP_FLOW.md`, 업적은 `docs/ACHIEVEMENTS.md`, 설계는 `docs/PLAN.md`를 따릅니다.

## 🚀 주요 기능

- **자체 로그인**: 아이디(영문 소문자·숫자·`.`·`_`, 20자)와 비밀번호(8자 이상)로 가입. BCrypt + JWT(access/refresh). OAuth는 쓰지 않습니다.
- **오늘의 갈래**: 오늘의 질문, 선택과 이유(500자) 제출, 다시 하기, 확장하기 완료, 마지막 단계·공유 초안 저장.
- **결과 잠금**: 결과 비율·선택지 코멘트·확장하기는 내 답을 남긴 뒤에만 서버가 내려줍니다. 의견은 익명입니다.
- **그룹**: 만들기, 6자리 초대 코드 참여, 설정·내보내기(그룹장), 나가기(그룹장이 나가면 그룹 삭제), 질문별 그룹 대화.
- **나누기**: 지금 답을 그룹 메시지 한 건으로 보냅니다. 이미 보낸 그룹에는 다시 보내지 않고, 다시 하기로 답이 바뀌면 새로 보낼 수 있습니다. 내가 답하기 전에는 그룹원의 선택과 글을 내려주지 않습니다.
- **생각 탭**: 검증을 마친 주제를 하루 5개씩 날짜에 따라 순환합니다. 댓글·답글(한 단계)·공감을 지원합니다.
- **세계수와 열매**: 업적 15개를 서버에서 판정하고, 맺은 날을 저장합니다. 한 번 맺은 열매는 사라지지 않습니다.
- **나 탭**: 프로필(사진·색·이름·아이디·소개), 선택·연속·댓글 수, 달력용 답변 목록, 내 댓글, 다른 사람 프로필.
- **관리자**: 질문 등록·수정, 관점별 해설 등록과 검수(DRAFT → APPROVED/REJECTED), 80% 쏠림 재검토 표시, 생각 주제 검증일 지정.

## 🛠 기술 스택

- **Language**: Java 25
- **Framework**: Spring Boot 4.1.2 (Spring MVC, Spring Security 7, Spring Data JPA / Hibernate 7)
- **Build Tool**: Gradle
- **Database**: MariaDB (운영), H2 (로컬·테스트, MariaDB 모드)
- **Security**: Spring Security, JWT (jjwt 0.12)
- **Docs**: springdoc-openapi (Swagger UI)

## 🏗 프로젝트 구조

[treasurehunter](../treasurehunter)와 같은 Package-by-Feature + Layered Architecture(Controller → Service → Repository)입니다.

```text
src/main/java/com/seohamin/galae/
├── domain/
│   ├── auth/         # 가입, 로그인, 토큰 갱신, 비밀번호 변경, 관리자 계정 준비
│   ├── user/         # 유저, 프로필, 사진
│   ├── question/     # 질문, 선택지, 관점별 해설, 결과, 의견, 기본 질문 시드
│   ├── answer/       # 나의 선택과 이유, 진행 상태, 달력, 연속 일수 계산
│   ├── group/        # 그룹, 멤버, 그룹 대화, 나누기
│   ├── feed/         # 생각 탭 주제, 하루 5개 순환, 주제 시드
│   ├── comment/      # 댓글, 답글, 공감
│   ├── achievement/  # 업적 15개 판정과 저장
│   └── admin/        # 질문 등록, 해설 검수, 주제 검증일
├── global/           # 인증(JWT 필터), 설정(Security, CORS, Swagger), 예외, 날짜, 유틸
└── GalaeApplication.java
src/main/resources/seed/
├── questions.json    # 프론트 src/questionData.js의 질문·선택지·성경 해설
└── paradoxes.json    # 프론트 content/paradoxes.json
```

## ▶️ 로컬 실행

`src/main/resources/application.yaml`은 비밀 값이 들어가므로 git에서 제외합니다. 아래 예시로 만들고 실행합니다. DB는 MariaDB(`galae` 데이터베이스, utf8mb4)를 쓰며, 테이블은 `ddl-auto: update`로 처음 실행할 때 만들어집니다.

```yaml
spring:
  application:
    name: galae
  datasource:
    driver-class-name: org.mariadb.jdbc.Driver
    url: jdbc:mariadb://localhost:3306/galae?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: galae
    password: <비밀번호>
  # DB 없이 띄워 볼 때는 H2 파일 DB
  # datasource:
  #   url: jdbc:h2:file:./data/galae;MODE=MariaDB;DATABASE_TO_LOWER=TRUE;NON_KEYWORDS=VALUE,USER
  #   username: sa
  #   password:
  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false

server:
  port: 8080
  forward-headers-strategy: framework

springdoc:
  api-docs:
    path: /api/swagger/v3/api-docs
  swagger-ui:
    path: /api/swagger/swagger-ui.html

jwt:
  secret: <openssl rand -base64 64 결과>
  access-token:
    expr-time: 3600       # 초
  refresh-token:
    expr-time: 1209600    # 초

cors:
  allowed-origins: http://localhost:*,http://127.0.0.1:*   # 배포 시 포트폴리오 도메인 추가

galae:
  zone: Asia/Seoul
  today: 2026-09-25        # 비우면 실제 날짜. 프론트 데모의 오늘과 맞출 때만 지정
  feed:
    include-unchecked: true  # 배포 시 false (검증 전 주제 숨김)
  seed:
    enabled: true            # 질문·주제 테이블이 비어 있으면 기본 콘텐츠 넣기
  admin:
    handle: admin            # 둘 다 있으면 시작할 때 관리자 계정 준비
    password: <관리자 비밀번호>
```

```sh
./gradlew bootRun     # http://localhost:8080
./gradlew test        # 통합·단위 테스트 (메모리 H2, src/test/resources/application.yaml)
```

- Swagger UI: `http://localhost:8080/api/swagger/swagger-ui/index.html`
- 헬스체크: `/ping`, `/ready`

## 📡 API

요청·응답 필드와 에러까지 담은 전체 명세는 [docs/API.md](docs/API.md)에 있습니다.

모든 API는 `/api/v1` 아래에 있고, 인증이 필요한 API는 `Authorization: Bearer <accessToken>` 헤더를 씁니다. 에러는 `{ timestamp, status, code, message }` 형태이며 `code`로 구분합니다(예: `ANSWER_REQUIRED`, `GROUP_CODE_INVALID`).

| 영역 | 메서드·경로 | 설명 |
| --- | --- | --- |
| 인증 | `POST /auth/signup`, `POST /auth/login`, `POST /auth/token/refresh` | 가입·로그인·갱신 (토큰 없이) |
| | `PATCH /users/me/password` | 비밀번호 변경 |
| 질문 | `GET /questions?query=&filter=all\|unanswered\|answered` | 질문 탭 목록 |
| | `GET /questions/today`, `GET /questions/{id}` | 오늘의 질문, 질문 상세 (비율 없음) |
| | `GET /questions/{id}/result` | 선택별 수·비율 🔒 |
| | `GET /questions/{id}/choices/{choiceId}/opinions?page=&size=` | 선택지 코멘트 (익명, 내 이유는 `myReason`) 🔒 |
| | `GET /questions/{id}/reading` | 확장하기 (승인된 해설만) 🔒 |
| 답변 | `GET·PUT /questions/{id}/answer` | 내 답 조회, 선택하기·다시 하기 `{choiceId, reason}` |
| | `POST /questions/{id}/answer/complete` | 확장하기 완료 |
| | `PATCH /questions/{id}/answer/progress` | `{lastStep, shareDraft}` 저장 |
| | `GET /users/me/answers?from=&to=` | 달력 (답한 날 기준) |
| 나누기 | `GET·POST /questions/{id}/share` | 보낸 그룹·초안 조회, `{groupIds}`로 보내기 (빈 배열 = 나만 보기) |
| 그룹 | `GET·POST /groups`, `POST /groups/join` | 내 그룹(오늘 나눈 멤버 포함), 만들기, 초대 코드 참여 |
| | `GET·PATCH /groups/{id}` | 상세(초대 코드·멤버), 설정(그룹장) |
| | `DELETE /groups/{id}/members/me`, `DELETE /groups/{id}/members/{userId}` | 나가기, 내보내기(그룹장) |
| | `GET·POST /groups/{id}/questions/{questionId}/messages?after=` | 질문별 그룹 대화 (답하기 전 `locked`). `after`=이전 응답의 `lastMessageId`로 새 메시지만 받는 증분 폴링 |
| 생각 | `GET /feed`, `GET /feed/topics/{id}` | 오늘의 5개 주제, 주제 하나 |
| | `GET·POST /feed/topics/{id}/comments` | 댓글 목록(답글 묶음), 댓글·답글 `{text, parentId}` |
| | `PUT·DELETE /comments/{id}/like` | 공감, 공감 취소 |
| 나 | `GET·PATCH /users/me`, `GET /users/me/achievements`, `GET /users/me/comments` | 프로필, 열매, 내 댓글 |
| | `GET /users/{id}` | 다른 사람 프로필 |
| 사진 | `GET /users/{id}/photo`, `GET /groups/{id}/photo` | JPEG (토큰 없이, `<img>`용) |
| 관리자 | `GET·POST /admin/questions`, `PATCH /admin/questions/{id}` | 질문 관리 (ROLE_ADMIN) |
| | `POST /admin/questions/{id}/perspectives`, `PUT /admin/perspectives/{id}` | 해설 등록·수정 (DRAFT) |
| | `GET /admin/perspectives?status=`, `PATCH /admin/perspectives/{id}/review` | 검수 큐, 승인·반려 |
| | `GET /admin/topics`, `PATCH /admin/topics/{id}/checked` | 주제 검증일 |

🔒: 그 질문에 내 답을 남긴 뒤에만 열립니다. 아니면 `403 ANSWER_REQUIRED`.

### 프론트 연동 메모

- 선택지 `id`(0, 1, 2)가 프론트의 `sel`입니다. 공유 메시지는 `share: true`이고 `choiceId`·`text`를 담습니다.
- 사진은 `data:image/jpeg;base64,...`로 보내고, 응답의 `photoUrl`(서버 기준 상대 경로)로 불러옵니다. 빈 문자열을 보내면 삭제됩니다.
- 날짜는 서비스 기준(Asia/Seoul) `YYYY-MM-DD`입니다. 처음 답한 날(`answeredOn`)은 다시 하기로 바뀌지 않습니다.
- 설정의 '기록 초기화'는 프론트 데모용이라 서버에는 두지 않았습니다.

## 📌 아직 하지 않은 것

- AI 해설 생성 파이프라인 (PLAN 5장). 해설 테이블에 `model`, `promptVersion`, 검수 상태 칸은 준비되어 있습니다.
- 출처 DB(`sources`, PLAN 7.5), 개인 맞춤 해설, 6개월 뒤 같은 질문 다시 묻기, 생각 변화율 측정.
- 탈퇴, 알림, 신고 기능.
