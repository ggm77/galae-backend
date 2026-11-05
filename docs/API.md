# 갈래 API 명세서

갈래 백엔드의 REST API 명세입니다. 코드(`src/main/java/com/seohamin/galae`) 기준으로 작성했고, 실행 중인 서버의 Swagger UI(`/api/swagger/swagger-ui/index.html`)에서도 확인할 수 있습니다.

- [1. 공통 규칙](#1-공통-규칙)
- [2. 인증](#2-인증)
- [3. 질문](#3-질문)
- [4. 답변](#4-답변)
- [5. 나누기](#5-나누기)
- [6. 그룹](#6-그룹)
- [7. 그룹 대화](#7-그룹-대화)
- [8. 생각 탭 (피드·댓글·공감)](#8-생각-탭-피드댓글공감)
- [9. 나 (프로필·열매·사진)](#9-나-프로필열매사진)
- [10. 관리자](#10-관리자)
- [11. 헬스체크](#11-헬스체크)
- [12. 에러 코드](#12-에러-코드)
- [부록: 공통 객체](#부록-공통-객체)

---

## 1. 공통 규칙

### Base URL

```
http://localhost:8080/api/v1
```

헬스체크(`/ping`, `/ready`)만 `/api/v1` 밖에 있습니다. 아래 경로는 모두 `/api/v1`을 뺀 형태로 적었습니다.

### 인증

| 표시 | 뜻 |
| --- | --- |
| 🔓 | 토큰 없이 호출 |
| 🔑 | `Authorization: Bearer <accessToken>` 필요. 없거나 만료되면 `401 AUTHENTICATION_ERROR` |
| 👑 | 🔑 + `ROLE_ADMIN`. 일반 유저는 `403 PERMISSION_DENIED` |
| 🔒 | 🔑 + 그 질문에 내 답을 남긴 뒤에만. 아니면 `403 ANSWER_REQUIRED` |

- 세션·쿠키를 쓰지 않습니다(STATELESS). 매 요청에 헤더를 붙입니다.
- 토큰이 잘못돼도 필터에서 바로 에러를 내지 않고 익명으로 진행하므로, 인증이 필요한 API에서 `401 AUTHENTICATION_ERROR`로 받게 됩니다.

### 요청·응답 형식

- 본문은 `application/json; charset=UTF-8`. 사진 조회 API만 `image/jpeg`.
- 날짜(`LocalDate`)는 `YYYY-MM-DD`, 서비스 기준 시간대 **Asia/Seoul**입니다.
- 일시(`LocalDateTime`)는 오프셋 없는 ISO-8601 `YYYY-MM-DDTHH:mm:ss.SSSSSS`입니다.
- 수정 API(`PATCH`)는 **`null`이거나 빠진 필드는 바꾸지 않습니다.** 앞뒤 공백은 서버에서 잘라 냅니다.
- 목록 응답은 따로 적지 않으면 JSON 배열이고, 결과가 없으면 `[]`입니다.

### 입력 규칙

| 항목 | 규칙 |
| --- | --- |
| 아이디 `handle` | `^[a-z0-9._]{1,20}$` (영문 소문자·숫자·`.`·`_`, 20자 이내) |
| 비밀번호 | 8~72자 |
| 색 `color` | `sage` \| `blue` \| `peach` \| `lilac` |
| 사진 `photo` | `data:image/jpeg;base64,...` (JPEG만, 디코딩 후 300KB 이하). 빈 문자열 `""`이면 삭제 |
| 질문 ID | `^[a-z0-9_-]{1,20}$` (예: `q037`) |
| 주제 ID | `^[a-z0-9_-]{1,20}$` (예: `p001`) |
| 선택지 ID `choiceId` | 질문 안에서의 순서 `0`, `1`, `2` (프론트의 `sel`) |

### 에러 응답

```json
{
  "timestamp": "2026-09-25T14:03:11.512345",
  "status": "FORBIDDEN",
  "code": "ANSWER_REQUIRED",
  "message": "내 답을 먼저 남겨야 볼 수 있습니다."
}
```

- `status`는 HTTP 상태 이름(문자열), 분기는 `code`로 합니다. 전체 목록은 [12. 에러 코드](#12-에러-코드).
- 필드 검증 실패는 `400 INVALID_REQUEST`이고 `message`가 `필드명: 이유` 형태입니다(첫 번째 오류만).
- JSON을 읽을 수 없거나 경로·쿼리 값의 타입이 틀려도 `400 INVALID_REQUEST`입니다.

### 질문이 "보인다"는 것

사용자 API에서 질문은 `status = PUBLISHED`이고 공개일이 오늘 이전(오늘 포함)일 때만 보입니다. 공개 전·초안 질문은 `404 QUESTION_NOT_EXIST`로 응답해 존재 여부를 숨깁니다.

---

## 2. 인증

### 2.1 회원가입 🔓

`POST /auth/signup` → `201 Created`

```json
{
  "handle": "galae.user",
  "password": "password123",
  "name": "갈래"
}
```

| 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `handle` | string | ✓ | 아이디 규칙 |
| `password` | string | ✓ | 8~72자 |
| `name` | string | ✓ | 공백 아님, 10자 이내 |

응답: [TokenResponse](#tokenresponse)

| 에러 | 상황 |
| --- | --- |
| `400 HANDLE_DUPLICATE` | 이미 쓰는 아이디 |
| `400 INVALID_REQUEST` | 규칙 위반 |

### 2.2 로그인 🔓

`POST /auth/login` → `200 OK`

```json
{ "handle": "galae.user", "password": "password123" }
```

응답: [TokenResponse](#tokenresponse)

| 에러 | 상황 |
| --- | --- |
| `400 LOGIN_FAILED` | 아이디가 없거나 비밀번호가 틀림 (둘을 구분하지 않음) |

### 2.3 토큰 갱신 🔓

`POST /auth/token/refresh` → `200 OK`

```json
{ "refreshToken": "eyJhbGciOi..." }
```

응답: [TokenResponse](#tokenresponse) — 액세스·리프레시 토큰을 둘 다 새로 줍니다.

| 에러 | 상황 |
| --- | --- |
| `401 INVALID_TOKEN` | 만료·위조된 토큰, 액세스 토큰을 보냄, 유저가 없음 |

### 2.4 비밀번호 변경 🔑

`PATCH /users/me/password` → `204 No Content`

```json
{ "currentPassword": "password123", "newPassword": "newpassword456" }
```

| 에러 | 상황 |
| --- | --- |
| `400 LOGIN_FAILED` | 현재 비밀번호가 틀림 |
| `400 INVALID_REQUEST` | 새 비밀번호가 8~72자가 아님 |

기존에 발급한 토큰은 만료 전까지 그대로 쓸 수 있습니다.

### 2.5 회원 탈퇴 🔑

`DELETE /users/me` → `204 No Content`

```json
{ "password": "password123" }
```

실수를 막기 위해 비밀번호를 한 번 더 받습니다. 한 트랜잭션으로 처리하므로 중간에 실패하면 아무것도 지워지지 않습니다.

| 데이터 | 처리 |
| --- | --- |
| 그룹 | 가입한 모든 그룹에서 '나가기'와 같게 처리합니다(6장). 그 그룹에 보낸 내 메시지(공유 포함)가 지워지고, **내가 그룹장인 그룹은 사라집니다**. |
| 답변·공유 초안 | 삭제. 참여 수·비율·선택지 코멘트(익명 의견)에서도 빠집니다. |
| 댓글·답글 | 내 댓글과 답글, 거기 달린 공감을 삭제합니다. **다른 사람이 내 댓글에 단 답글은 지우지 않고 원 댓글로 올립니다**(`parentId: null`). |
| 공감 | 내가 누른 공감을 삭제하고, 그 댓글의 `likeCount`를 줄입니다. |
| 열매·프로필·사진 | 삭제. |

- 탈퇴하면 그 계정의 토큰은 만료 전이라도 바로 `401 AUTHENTICATION_ERROR`가 됩니다. 프론트는 저장한 토큰을 지우고 시작 화면으로 보냅니다.
- 같은 아이디는 탈퇴 뒤 다시 가입할 수 있습니다.

| 에러 | 상황 |
| --- | --- |
| `400 LOGIN_FAILED` | 비밀번호가 틀림 |
| `400 INVALID_REQUEST` | `password` 없음 |
| `400 ADMIN_CANNOT_WITHDRAW` | 관리자 계정 (관리자 페이지 접근을 잃지 않도록 막음) |

---

## 3. 질문

### 3.1 질문 목록 🔑

`GET /questions?query=&filter=`

| 쿼리 | 기본값 | 설명 |
| --- | --- | --- |
| `query` | 없음 | 제목·본문 부분 일치 검색 (대소문자 무시) |
| `filter` | `all` | `all` \| `unanswered` \| `answered`. 그 밖의 값은 `all`로 처리 |

공개일 최신순입니다.

```json
[
  {
    "id": "q037",
    "date": "2026-09-25",
    "title": "친구의 거짓말을 알게 됐다면?",
    "body": "…",
    "participantCount": 128,
    "myAnswer": {
      "choiceId": 1,
      "completed": true,
      "answeredOn": "2026-09-25",
      "lastStep": "done"
    }
  }
]
```

`myAnswer`는 아직 답하지 않았으면 `null`입니다. `lastStep`도 저장한 적 없으면 `null`일 수 있습니다.

### 3.2 오늘의 질문 🔑

`GET /questions/today`

오늘 날짜의 질문, 없으면 가장 최근에 공개된 질문을 줍니다. 응답: [QuestionDetail](#questiondetail)

| 에러 | 상황 |
| --- | --- |
| `404 QUESTION_NOT_EXIST` | 공개된 질문이 하나도 없음 |

### 3.3 질문 상세 🔑

`GET /questions/{questionId}`

응답: [QuestionDetail](#questiondetail) — 선택지 비율은 주지 않습니다.

### 3.4 결과 🔑🔒

`GET /questions/{questionId}/result`

```json
{
  "questionId": "q037",
  "participantCount": 128,
  "myChoiceId": 1,
  "choices": [
    { "id": 0, "text": "바로 말한다", "shortText": "말한다", "count": 50, "pct": 39 },
    { "id": 1, "text": "모른 척한다", "shortText": "모른 척", "count": 60, "pct": 47 },
    { "id": 2, "text": "다른 친구와 상의한다", "shortText": "상의", "count": 18, "pct": 14 }
  ]
}
```

- `pct`는 정수이고 합이 항상 100입니다(최대 잉여 방식). 응답이 없으면 모두 0.

### 3.5 선택지 코멘트 🔑🔒

`GET /questions/{questionId}/choices/{choiceId}/opinions?page=0&size=20`

그 선택지를 고른 **다른 사람들**의 이유(빈 이유 제외)를 최신순으로 줍니다. 작성자는 주지 않습니다(익명).

| 쿼리 | 기본값 | 규칙 |
| --- | --- | --- |
| `page` | `0` | 0 이상 |
| `size` | `20` | 1~50 |

```json
{
  "questionId": "q037",
  "choiceId": 1,
  "myReason": "지금 말하면 더 상처가 될 것 같아서",
  "opinions": ["…", "…"],
  "page": 0,
  "hasNext": true
}
```

- `myReason`: 내가 이 선택지를 골랐고 이유를 적었으면 내 이유(맨 위에 보여 줄 용도). 아니면 `null`. 내 이유는 `opinions`에 들어가지 않습니다.

| 에러 | 상황 |
| --- | --- |
| `400 INVALID_REQUEST` | `page`·`size` 범위 밖 |
| `400 CHOICE_NOT_EXIST` | 없는 선택지 |

### 3.6 확장하기 🔑🔒

`GET /questions/{questionId}/reading`

검수에서 **승인된** 관점별 해설만 `sortOrder` 순으로 줍니다.

```json
{
  "questionId": "q037",
  "perspectives": [
    { "label": "성경", "body": "…", "verse": "사랑은 오래 참고…", "ref": "고린도전서 13:4" }
  ]
}
```

`verse`, `ref`는 없으면 `null`입니다.

---

## 4. 답변

### 4.1 내 답변 조회 🔑

`GET /questions/{questionId}/answer`

응답: [Answer](#answer)

| 에러 | 상황 |
| --- | --- |
| `404 ANSWER_NOT_EXIST` | 아직 답하지 않음 |

### 4.2 선택하기 / 다시 하기 🔑

`PUT /questions/{questionId}/answer` → `200 OK`

```json
{ "choiceId": 1, "reason": "지금 말하면 더 상처가 될 것 같아서" }
```

| 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `choiceId` | integer | ✓ | 0 이상, 질문에 있는 선택지 |
| `reason` | string |  | 500자 이내. 없으면 빈 문자열로 저장 |

- **처음 답할 때**: 답을 만들고 `answeredOn`을 오늘로 남깁니다. 업적(첫 가지·여행자·연속·답변 수)을 판정합니다.
- **다시 할 때**: 선택·이유만 바꿉니다. `answeredOn`, `completed`, `lastStep`은 그대로입니다. 선택이나 이유가 바뀌었으면 공유 초안(`shareDraft`)을 비웁니다. 둘 다 같으면 아무것도 바뀌지 않습니다.

응답: [Answer](#answer)

| 에러 | 상황 |
| --- | --- |
| `400 CHOICE_NOT_EXIST` | 없는 선택지 |

### 4.3 확장하기 완료 🔑

`POST /questions/{questionId}/answer/complete`

`completed`를 `true`로 바꿉니다. 여러 번 불러도 결과는 같습니다. 응답: [Answer](#answer)

| 에러 | 상황 |
| --- | --- |
| `404 ANSWER_NOT_EXIST` | 아직 답하지 않음 |

### 4.4 진행 상태 저장 🔑

`PATCH /questions/{questionId}/answer/progress`

```json
{ "lastStep": "share", "shareDraft": [3, 7] }
```

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `lastStep` | string | `answer` \| `result` \| `reading` \| `done` \| `share` \| `summary` |
| `shareDraft` | integer[] | 나누기에서 골라 둔 그룹 ID(가입한 그룹만). `[]`이면 '나만 보기' 초안 |

둘 다 생략 가능하고, 생략한 필드는 그대로 둡니다. 응답: [Answer](#answer)

| 에러 | 상황 |
| --- | --- |
| `404 ANSWER_NOT_EXIST` | 아직 답하지 않음 |
| `403 GROUP_NOT_JOINED` | 가입하지 않은 그룹이 섞여 있음 |
| `400 INVALID_REQUEST` | `lastStep` 값이 목록 밖, `shareDraft`에 `null` |

### 4.5 달력용 내 답변 목록 🔑

`GET /users/me/answers?from=2026-09-01&to=2026-09-30`

`answeredOn`(처음 답한 날)이 기간 안에 있는 답을 최신순으로 줍니다. 두 쿼리 모두 필수이고, `from ≤ to`, 기간은 400일 이내입니다.

```json
[
  {
    "questionId": "q037",
    "questionDate": "2026-09-25",
    "title": "친구의 거짓말을 알게 됐다면?",
    "choiceId": 1,
    "choiceShortText": "모른 척",
    "reason": "…",
    "answeredOn": "2026-09-25",
    "completed": true
  }
]
```

| 에러 | 상황 |
| --- | --- |
| `400 INVALID_REQUEST` | 쿼리 누락·형식 오류, 기간 조건 위반 |

---

## 5. 나누기

### 5.1 나누기 상태 🔑

`GET /questions/{questionId}/share`

응답: [ShareStatus](#sharestatus)

| 에러 | 상황 |
| --- | --- |
| `403 ANSWER_REQUIRED` | 아직 답하지 않음 |

### 5.2 나누기 🔑

`POST /questions/{questionId}/share`

```json
{ "groupIds": [3, 7] }
```

| 필드 | 타입 | 필수 | 규칙 |
| --- | --- | --- | --- |
| `groupIds` | integer[] | ✓ | 100개 이하, 원소 `null` 불가. `[]` = 나만 보기 |

- 고른 그룹마다 **지금 답(선택·이유)**을 공유 메시지 한 건으로 올립니다.
- 지금 답을 이미 보낸 그룹(`sentGroupIds`)에는 다시 보내지 않습니다. 다시 하기로 답이 바뀌면 그 그룹에 새로 보낼 수 있습니다.
- 보낸 뒤 공유 초안을 비웁니다(`shareDraft: null`). 업적(길동무·동반자)을 판정합니다.

응답: [ShareStatus](#sharestatus)

| 에러 | 상황 |
| --- | --- |
| `403 ANSWER_REQUIRED` | 아직 답하지 않음 |
| `403 GROUP_NOT_JOINED` | 가입하지 않은 그룹이 섞여 있음 |

---

## 6. 그룹

### 6.1 내 그룹 목록 🔑

`GET /groups`

가입한 순서대로 줍니다.

```json
[
  {
    "id": 3,
    "name": "독서 모임",
    "description": "매주 수요일",
    "color": "sage",
    "photoUrl": "/api/v1/groups/3/photo?v=1758790000",
    "memberCount": 5,
    "owner": true,
    "todaySharedMembers": [ { "id": 12, "handle": "mina", "name": "미나", "color": "peach", "photoUrl": null } ]
  }
]
```

- `todaySharedMembers`: 오늘의 질문([3.2](#32-오늘의-질문-)과 같은 질문)을 이 그룹에 나눈 멤버([UserSimple](#usersimple)).

### 6.2 그룹 만들기 🔑

`POST /groups` → `201 Created`

```json
{ "name": "독서 모임", "description": "매주 수요일", "color": "sage", "photo": "data:image/jpeg;base64,..." }
```

| 필드 | 필수 | 규칙 |
| --- | --- | --- |
| `name` | ✓ | 공백 아님, 20자 이내 |
| `description` |  | 100자 이내. 없으면 빈 문자열 |
| `color` | ✓ | 색 규칙 |
| `photo` |  | 사진 규칙 |

만든 사람이 그룹장이자 첫 멤버가 되고, 6자리 초대 코드가 발급됩니다(헷갈리는 `0 O 1 I` 제외). 개척자 업적을 판정합니다. 응답: [GroupDetail](#groupdetail)

| 에러 | 상황 |
| --- | --- |
| `400 INVALID_PHOTO` / `400 TOO_BIG_PHOTO` | 사진 형식 오류 / 300KB 초과 |

### 6.3 초대 코드로 참여 🔑

`POST /groups/join` → `200 OK`

```json
{ "code": "ab3k9z" }
```

대소문자와 앞뒤 공백은 무시합니다. 가입만으로 기존 답변이 공유되지는 않습니다. 응답: [GroupDetail](#groupdetail)

| 에러 | 상황 |
| --- | --- |
| `400 GROUP_CODE_INVALID` | 없는 코드 |
| `400 GROUP_ALREADY_JOINED` | 이미 가입함 |

### 6.4 그룹 상세 🔑

`GET /groups/{groupId}`

응답: [GroupDetail](#groupdetail) (초대 코드·멤버 포함)

| 에러 | 상황 |
| --- | --- |
| `404 GROUP_NOT_EXIST` | 없는 그룹 |
| `403 GROUP_NOT_JOINED` | 가입하지 않은 그룹 |

### 6.5 그룹 설정 🔑 (그룹장)

`PATCH /groups/{groupId}`

본문은 [6.2](#62-그룹-만들기-)와 같고 모든 필드가 선택입니다. 빈 `name`은 무시합니다. 응답: [GroupDetail](#groupdetail)

| 에러 | 상황 |
| --- | --- |
| `403 PERMISSION_DENIED` | 그룹장이 아님 |
| `404 GROUP_NOT_EXIST` / `403 GROUP_NOT_JOINED` | 6.4와 같음 |

### 6.6 그룹 나가기 🔑

`DELETE /groups/{groupId}/members/me` → `204 No Content`

- 이 그룹에 보낸 내 메시지(공유 포함)와 이 그룹을 고른 내 공유 초안이 지워집니다. 개인 기록(답변)은 남습니다.
- **그룹장이 나가면 그룹이 삭제됩니다**(모든 멤버·메시지·초안에서 제거).

### 6.7 멤버 내보내기 🔑 (그룹장)

`DELETE /groups/{groupId}/members/{userId}` → `204 No Content`

내보낸 멤버의 메시지와 초안을 6.6과 똑같이 지웁니다.

| 에러 | 상황 |
| --- | --- |
| `403 PERMISSION_DENIED` | 그룹장이 아님 |
| `400 CANNOT_KICK_SELF` | 자기 자신 |
| `404 MEMBER_NOT_EXIST` | 그 유저가 멤버가 아님 |

### 6.8 그룹 사진 🔓

`GET /groups/{groupId}/photo` → `image/jpeg`

`Cache-Control: max-age=2592000, public`(30일). 응답의 `photoUrl`에 `?v=`가 붙어 사진이 바뀌면 주소가 바뀝니다.

| 에러 | 상황 |
| --- | --- |
| `404 GROUP_NOT_EXIST` / `404 PHOTO_NOT_EXIST` | 없는 그룹 / 사진 없음 |

---

## 7. 그룹 대화

### 7.1 질문별 대화 조회 🔑

`GET /groups/{groupId}/questions/{questionId}/messages?after=40`

메시지 ID 순(오래된 순)입니다. 실시간 연결 대신 **증분 폴링**으로 새 메시지를 받습니다.

| 쿼리 | 기본값 | 규칙 |
| --- | --- | --- |
| `after` | 없음(전체) | 0 이상. 이 메시지 ID보다 **새로운** 메시지만 줍니다. 이전 응답의 `lastMessageId`를 넣습니다 |

```json
{
  "groupId": 3,
  "questionId": "q037",
  "locked": false,
  "sharedMemberCount": 2,
  "lastMessageId": 42,
  "messages": [
    {
      "id": 41,
      "author": { "id": 12, "handle": "mina", "name": "미나", "color": "peach", "photoUrl": null },
      "mine": false,
      "share": true,
      "choiceId": 0,
      "choiceShortText": "말한다",
      "text": "솔직한 게 결국 서로에게 낫다고 생각해요",
      "createdAt": "2026-09-25T09:12:30.123456"
    },
    {
      "id": 42,
      "author": { "id": 7, "handle": "galae.user", "name": "갈래", "color": "blue", "photoUrl": null },
      "mine": true,
      "share": false,
      "choiceId": null,
      "choiceShortText": null,
      "text": "저도 비슷하게 생각했어요",
      "createdAt": "2026-09-25T09:15:02.654321"
    }
  ]
}
```

- `locked`: 내가 이 질문에 아직 답하지 않았으면 `true`. 이때 **남의 메시지**는 `choiceId`·`choiceShortText`·`text`가 `null`입니다(작성자·시각은 보임). 프론트는 흐리게 보여 주고 답하기 버튼을 띄웁니다.
- `share`: 나누기로 올라간 메시지(선택·이유)면 `true`. 일반 메시지는 `choiceId`가 항상 `null`.
- `sharedMemberCount`: 이 질문을 이 그룹에 나눈 멤버 수. `after`와 상관없이 전체 기준입니다.
- `lastMessageId`: 다음 폴링의 `after`에 넣을 값. 이번에 받은 마지막 메시지 ID이고, 새 메시지가 없으면 보낸 `after` 그대로, 메시지가 하나도 없으면 `0`입니다.

#### 증분 폴링 사용법

```text
1. 대화 화면을 열면 after 없이 호출 → 전체 메시지, lastMessageId 저장
2. 화면이 열려 있는 동안 몇 초마다 ?after={lastMessageId} 호출
   → 새 메시지만 받아 목록 뒤에 붙이고 lastMessageId 갱신
3. 화면을 떠나면 폴링을 멈춤
```

- **`locked`가 `true`에서 `false`로 바뀌면**(내가 답한 뒤) `after` 없이 한 번 다시 불러야 합니다. 이미 받은 남의 메시지는 내용이 `null`인 채로 받았기 때문입니다.
- 증분 응답에는 **새 메시지만** 옵니다. 누가 그룹을 나가거나 내보내져 그 사람의 메시지가 지워져도 폴링으로는 알 수 없으므로, 필요하면 `after` 없이 다시 불러 목록을 바꿉니다.
- 가입 여부는 매 요청마다 확인합니다. 그룹을 나가거나 내보내지면 다음 폴링부터 `403 GROUP_NOT_JOINED`입니다.

| 에러 | 상황 |
| --- | --- |
| `400 INVALID_REQUEST` | `after`가 음수이거나 숫자가 아님 |
| `404 GROUP_NOT_EXIST` / `403 GROUP_NOT_JOINED` | 그룹 확인 실패 |
| `404 QUESTION_NOT_EXIST` | 보이지 않는 질문 |

### 7.2 일반 메시지 보내기 🔑

`POST /groups/{groupId}/questions/{questionId}/messages` → `201 Created`

```json
{ "text": "저도 비슷하게 생각했어요" }
```

`text`는 필수, 500자 이내. 답하지 않아도 보낼 수 있습니다. 응답: 7.1의 `messages` 원소 하나.

| 에러 | 상황 |
| --- | --- |
| `400 EMPTY_MESSAGE` | 공백만 있음 |
| 7.1과 같음 | 그룹·질문 확인 실패 |

---

## 8. 생각 탭 (피드·댓글·공감)

주제는 검증일(`checkedOn`)이 있는 것만 보입니다. 서버 설정 `galae.feed.include-unchecked: true`면 검증 전 주제도 보입니다(개발용). 보이지 않는 주제는 `404 TOPIC_NOT_EXIST`입니다.

### 8.1 오늘의 피드 🔑

`GET /feed`

보이는 주제를 ID순으로 세워 두고 날짜에 따라 하루 5개씩 돌아가며 줍니다. 5개 이하면 전부 줍니다.

```json
{
  "date": "2026-09-25",
  "topics": [ { "id": "p001", "category": "…", "title": "…", "cards": ["…"], "sources": [ { "label": "…", "url": "https://…" } ], "checkedOn": "2026-09-01", "commentCount": 4 } ]
}
```

`topics` 원소: [Topic](#topic)

### 8.2 주제 하나 🔑

`GET /feed/topics/{topicId}`

공유 링크로 들어온 경우 등에 씁니다. 응답: [Topic](#topic)

### 8.3 댓글 목록 🔑

`GET /feed/topics/{topicId}/comments`

원 댓글 오래된 순, 각 원 댓글의 `replies`에 답글이 오래된 순으로 묶여 있습니다. 응답: [Comment](#comment)[]

### 8.4 댓글·답글 남기기 🔑

`POST /feed/topics/{topicId}/comments` → `201 Created`

```json
{ "text": "흥미로운 관점이네요", "parentId": null }
```

| 필드 | 필수 | 규칙 |
| --- | --- | --- |
| `text` | ✓ | 공백 아님, 500자 이내 |
| `parentId` |  | 답글이면 같은 주제의 댓글 ID |

답글은 한 단계만 있습니다. 답글에 답하면 그 답글의 원 댓글에 붙습니다. 업적(말동무·논객)을 판정합니다. 응답: [Comment](#comment) (`replies: []`, `liked: false`)

| 에러 | 상황 |
| --- | --- |
| `404 COMMENT_NOT_EXIST` | `parentId`가 없거나 다른 주제의 댓글 |
| `400 EMPTY_MESSAGE` | 공백만 있음 |

### 8.5 공감 / 공감 취소 🔑

`PUT /comments/{commentId}/like` · `DELETE /comments/{commentId}/like`

둘 다 멱등입니다(여러 번 눌러도 결과가 같음). 공감할 때 경청자 업적을 판정합니다.

```json
{ "commentId": 41, "liked": true, "likeCount": 8 }
```

| 에러 | 상황 |
| --- | --- |
| `404 COMMENT_NOT_EXIST` | 없는 댓글 |
| `404 TOPIC_NOT_EXIST` | 댓글의 주제가 보이지 않음 |

### 8.6 내 댓글 목록 🔑

`GET /users/me/comments`

최신순입니다.

```json
[
  { "id": 41, "topicId": "p001", "topicTitle": "…", "parentId": null, "text": "…", "likeCount": 8, "createdAt": "2026-09-25T09:12:30.123456" }
]
```

---

## 9. 나 (프로필·열매·사진)

### 9.1 내 프로필 🔑

`GET /users/me`

응답: [UserProfile](#userprofile) — `joinedOn`, `role`이 채워집니다.

### 9.2 프로필 편집 🔑

`PATCH /users/me`

```json
{ "handle": "new.handle", "name": "새이름", "bio": "한 줄 소개", "color": "lilac", "photo": "" }
```

| 필드 | 규칙 |
| --- | --- |
| `handle` | 아이디 규칙, 중복 불가 |
| `name` | 1~10자 (공백만 있으면 무시) |
| `bio` | 80자 이내 |
| `color` | 색 규칙 |
| `photo` | 사진 규칙 (`""` = 삭제) |

모든 필드 선택. 응답: [UserProfile](#userprofile)

| 에러 | 상황 |
| --- | --- |
| `400 HANDLE_DUPLICATE` | 이미 쓰는 아이디 |
| `400 INVALID_PHOTO` / `400 TOO_BIG_PHOTO` | 사진 오류 |

### 9.3 내 열매 🔑

`GET /users/me/achievements`

맺은 순서대로 줍니다. 한 번 맺은 열매는 사라지지 않습니다.

```json
[ { "id": "first", "name": "첫 가지", "description": "처음으로 답했어요", "achievedOn": "2026-09-25" } ]
```

업적 15개:

| id | 이름 | 조건 |
| --- | --- | --- |
| `first` | 첫 가지 | 처음으로 답함 |
| `past` | 여행자 | 지난 질문에 처음 답함 (답한 날 ≠ 질문 날) |
| `share1` | 길동무 | 처음으로 그룹에 나눔 |
| `comment1` | 말동무 | 처음으로 댓글 |
| `group` | 개척자 | 처음으로 그룹을 만듦 |
| `streak7` | 뿌리 | 7일 연속 답함 |
| `streak30` | 수행자 | 30일 연속 |
| `streak100` | 오래된 나무 | 100일 연속 |
| `answer10` | 탐험가 | 답 10개 |
| `answer30` | 사색가 | 답 30개 |
| `answer100` | 현자 | 답 100개 |
| `answer200` | 세계수 | 답 200개 |
| `share10` | 동반자 | 10개 질문을 그룹에 나눔 |
| `comment100` | 논객 | 댓글 100개 |
| `like100` | 경청자 | 공감 100번 |

### 9.4 다른 사람 프로필 🔑

`GET /users/{userId}`

응답: [UserProfile](#userprofile) — 남의 프로필이면 `joinedOn`, `role`이 `null`입니다.

| 에러 | 상황 |
| --- | --- |
| `404 USER_NOT_EXIST` | 없는 유저 |

### 9.5 프로필 사진 🔓

`GET /users/{userId}/photo` → `image/jpeg`

캐시 규칙은 [6.8](#68-그룹-사진-)과 같습니다.

| 에러 | 상황 |
| --- | --- |
| `404 USER_NOT_EXIST` / `404 PHOTO_NOT_EXIST` | 없는 유저 / 사진 없음 |

---

## 10. 관리자

모두 👑 `ROLE_ADMIN`만 쓸 수 있습니다. 관리자 계정은 서버 설정 `galae.admin.handle`·`password`로 시작할 때 준비됩니다. 관리자 페이지는 `/admin/`(정적 HTML)입니다.

### 10.1 전체 질문 👑

`GET /admin/questions`

공개 전·초안 포함, 공개일 최신순. 응답: [AdminQuestion](#adminquestion)[]

### 10.2 질문 등록 👑

`POST /admin/questions` → `201 Created`

```json
{
  "id": "q038",
  "date": "2026-09-26",
  "title": "…",
  "body": "…",
  "status": "DRAFT",
  "choices": [
    { "text": "바로 말한다", "shortText": "말한다" },
    { "text": "모른 척한다", "shortText": "모른 척" }
  ]
}
```

| 필드 | 필수 | 규칙 |
| --- | --- | --- |
| `id` | ✓ | 질문 ID 규칙 |
| `date` | ✓ | 공개일. 하루에 질문 하나 |
| `title` | ✓ | 100자 이내 |
| `body` | ✓ | 1000자 이내 |
| `status` |  | `DRAFT` \| `PUBLISHED`. 없으면 `DRAFT` |
| `choices` | ✓ | 2~3개. `text` 200자, `shortText` 50자 이내 (둘 다 필수). 순서대로 `id` 0, 1, 2 |

응답: [AdminQuestion](#adminquestion)

| 에러 | 상황 |
| --- | --- |
| `400 QUESTION_ALREADY_EXIST` | 같은 ID나 같은 날짜의 질문이 있음 |

### 10.3 질문 수정 👑

`PATCH /admin/questions/{questionId}`

본문은 10.2와 같고 모든 필드가 선택입니다(`id`는 무시). `choices`를 보내면 통째로 바꿉니다.

| 에러 | 상황 |
| --- | --- |
| `404 QUESTION_NOT_EXIST` | 없는 질문 |
| `400 QUESTION_ALREADY_EXIST` | 바꾸려는 날짜에 다른 질문이 있음 |
| `400 QUESTION_HAS_ANSWERS` | 답변이 있는 질문의 선택지를 바꾸려 함 |

### 10.4 해설 등록 👑

`POST /admin/questions/{questionId}/perspectives` → `201 Created`

```json
{
  "label": "성경",
  "body": "…",
  "verse": "…",
  "ref": "고린도전서 13:4",
  "sortOrder": 0,
  "model": "claude-opus-5-5",
  "promptVersion": "v1"
}
```

| 필드 | 필수 | 규칙 |
| --- | --- | --- |
| `label` | ✓ | 20자 이내 |
| `body` | ✓ | 2000자 이내 |
| `verse` |  | 500자 이내 |
| `ref` |  | 100자 이내 |
| `sortOrder` |  | 없으면 그 질문의 현재 해설 수 (맨 뒤) |
| `model` |  | 50자 이내 (AI 파이프라인용) |
| `promptVersion` |  | 30자 이내 |

항상 `DRAFT`로 등록되고, 검수에서 승인해야 앱에 보입니다. 응답: [AdminPerspective](#adminperspective)

### 10.5 해설 수정 👑

`PUT /admin/perspectives/{perspectiveId}`

본문은 10.4와 같습니다(`label`, `body` 필수). 전체를 바꾸므로 빠진 `verse`·`ref`·`model`·`promptVersion`은 `null`이 되고, `sortOrder`만 빼면 기존 값을 유지합니다. **수정하면 다시 `DRAFT`가 되고 검수 기록(`reviewedBy`, `reviewedAt`)이 지워집니다.** 승인된 해설도 수정하는 순간 앱에서 내려갑니다. 응답: [AdminPerspective](#adminperspective)

| 에러 | 상황 |
| --- | --- |
| `404 PERSPECTIVE_NOT_EXIST` | 없는 해설 |

### 10.6 검수 큐 👑

`GET /admin/perspectives?status=DRAFT`

`status`: `DRAFT`(기본) \| `APPROVED` \| `REJECTED` \| `FAILED`. 오래된 순. 응답: [AdminPerspective](#adminperspective)[]

### 10.7 승인·반려 👑

`PATCH /admin/perspectives/{perspectiveId}/review`

```json
{ "status": "APPROVED" }
```

`status`는 `APPROVED` 또는 `REJECTED`. 검수한 관리자와 시각을 남깁니다. 부적절한 해설을 즉시 내릴 때도 `REJECTED`로 씁니다. 응답: [AdminPerspective](#adminperspective)

| 에러 | 상황 |
| --- | --- |
| `400 INVALID_REVIEW_STATUS` | `DRAFT`·`FAILED`를 보냄 |
| `404 PERSPECTIVE_NOT_EXIST` | 없는 해설 |

### 10.8 전체 주제 👑

`GET /admin/topics`

검증 전 포함, ID순. 응답: [Topic](#topic)[] (`commentCount`는 항상 `0`)

### 10.9 주제 검증일 지정 👑

`PATCH /admin/topics/{topicId}/checked`

```json
{ "checkedOn": "2026-09-25" }
```

날짜를 넣으면 노출, `null`이면 보류(숨김). 응답: [Topic](#topic)

| 에러 | 상황 |
| --- | --- |
| `404 TOPIC_NOT_EXIST` | 없는 주제 |

---

## 11. 헬스체크

| 경로 | 응답 | 설명 |
| --- | --- | --- |
| `GET /ping` 🔓 | `pong` (text) | 프로세스 생존 |
| `GET /ready` 🔓 | `ready` (text) | DB 연결 확인. 실패 시 `503 SERVICE_UNAVAILABLE` |

---

## 12. 에러 코드

| HTTP | code | 메시지 |
| --- | --- | --- |
| 400 | `INVALID_REQUEST` | 요청 정보가 잘못되어 있습니다. (검증 실패 시 `필드: 이유`) |
| 400 | `INVALID_PHOTO` | 사진 형식이 올바르지 않습니다. |
| 400 | `TOO_BIG_PHOTO` | 사진이 너무 큽니다. |
| 400 | `HANDLE_DUPLICATE` | 이미 사용 중인 아이디입니다. |
| 400 | `LOGIN_FAILED` | 아이디 또는 비밀번호가 올바르지 않습니다. |
| 400 | `CHOICE_NOT_EXIST` | 선택지가 존재하지 않습니다. |
| 400 | `GROUP_CODE_INVALID` | 초대 코드가 올바르지 않습니다. |
| 400 | `GROUP_ALREADY_JOINED` | 이미 가입한 그룹입니다. |
| 400 | `CANNOT_KICK_SELF` | 자기 자신은 내보낼 수 없습니다. |
| 400 | `EMPTY_MESSAGE` | 내용을 입력해야 합니다. |
| 400 | `QUESTION_ALREADY_EXIST` | 같은 ID나 날짜의 질문이 이미 존재합니다. |
| 400 | `QUESTION_HAS_ANSWERS` | 이미 답변이 있는 질문은 선택지를 바꿀 수 없습니다. |
| 400 | `INVALID_REVIEW_STATUS` | 검수 결과는 APPROVED 또는 REJECTED여야 합니다. |
| 400 | `ADMIN_CANNOT_WITHDRAW` | 관리자 계정은 탈퇴할 수 없습니다. |
| 401 | `AUTHENTICATION_ERROR` | 토큰이 만료되었거나 없습니다. |
| 401 | `INVALID_TOKEN` | 유효하지 않은 토큰입니다. (리프레시) |
| 403 | `PERMISSION_DENIED` | 해당 기능에 접근할 수 없습니다. |
| 403 | `GROUP_NOT_JOINED` | 가입하지 않은 그룹입니다. |
| 403 | `ANSWER_REQUIRED` | 내 답을 먼저 남겨야 볼 수 있습니다. |
| 404 | `USER_NOT_EXIST` | 유저가 존재하지 않습니다. |
| 404 | `QUESTION_NOT_EXIST` | 질문이 존재하지 않습니다. |
| 404 | `ANSWER_NOT_EXIST` | 답변이 존재하지 않습니다. |
| 404 | `PERSPECTIVE_NOT_EXIST` | 해설이 존재하지 않습니다. |
| 404 | `GROUP_NOT_EXIST` | 그룹이 존재하지 않습니다. |
| 404 | `MEMBER_NOT_EXIST` | 그룹 멤버가 존재하지 않습니다. |
| 404 | `TOPIC_NOT_EXIST` | 주제가 존재하지 않습니다. |
| 404 | `COMMENT_NOT_EXIST` | 댓글이 존재하지 않습니다. |
| 404 | `PHOTO_NOT_EXIST` | 사진이 존재하지 않습니다. |
| 409 | `CONFLICT` | 요청이 현재 데이터와 충돌합니다. (같은 요청이 동시에 들어와 유니크 제약 위반) |
| 500 | `INTERNAL_SERVER_ERROR` | 서버에서 오류가 발생했습니다. |
| 503 | `SERVICE_UNAVAILABLE` | 현재 서버를 이용할 수 없습니다. |

`{questionId}`가 들어가는 사용자 API는 모두 질문이 보이지 않으면 `404 QUESTION_NOT_EXIST`, 경로 숫자 ID 형식이 틀리면 `400 INVALID_REQUEST`를 줄 수 있습니다. 각 API 표에서는 반복을 줄이려고 생략했습니다.

---

## 부록: 공통 객체

### TokenResponse

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `userId` | long | |
| `tokenType` | string | `Bearer` |
| `accessToken` | string | |
| `exprTime` | long | 액세스 토큰 만료까지 남은 초 (기본 3600) |
| `refreshToken` | string | 기본 14일 |

### UserSimple

목록·메시지·댓글에 붙는 유저 정보.

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | long | |
| `handle` | string | |
| `name` | string | |
| `color` | string | 가입 시 기본 `blue` |
| `photoUrl` | string \| null | 서버 기준 상대 경로 `/api/v1/users/{id}/photo?v=…`. 사진 없으면 `null` |

### UserProfile

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id`, `handle`, `name`, `color`, `photoUrl` | | UserSimple과 같음 |
| `bio` | string | 가입 시 빈 문자열 |
| `stats.answerCount` | long | 답한 질문 수 (세계수의 선 수) |
| `stats.currentStreak` | int | 지금 이어지는 연속 일수 (오늘 안 답했으면 어제부터 셈) |
| `stats.longestStreak` | int | 가장 길었던 연속 일수 |
| `stats.commentCount` | long | 댓글 수 |
| `achievements` | object[] | [9.3](#93-내-열매-)의 원소와 같음 |
| `joinedOn` | date \| null | 내 프로필에서만 |
| `role` | string \| null | `USER` \| `ADMIN`. 내 프로필에서만 |

### QuestionDetail

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | string | |
| `date` | date | 공개일 |
| `title`, `body` | string | |
| `participantCount` | long | 답한 사람 수 |
| `choices[]` | `{ id, text, shortText }` | 비율 없음 |
| `myAnswer` | [Answer](#answer) \| null | 아직 답하지 않았으면 `null` |

### Answer

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `questionId` | string | |
| `choiceId` | int | |
| `choiceShortText` | string | |
| `reason` | string | 없으면 `""` |
| `completed` | boolean | 확장하기 완료 여부 |
| `answeredOn` | date | 처음 답한 날. 다시 하기로 바뀌지 않음 |
| `lastStep` | string \| null | 마지막으로 보던 단계 |
| `shareDraft` | long[] \| null | 골라 둔 그룹. 초안이 없으면 `null`, '나만 보기' 초안이면 `[]` |
| `updatedAt` | datetime | |

### ShareStatus

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `questionId` | string | |
| `sentGroupIds` | long[] | 지금 답(선택·이유)을 이미 보낸 그룹. 나누기 화면에서 체크된 채 풀 수 없고, 완료·요약의 공유 표시도 이 그룹만 셈 |
| `shareDraft` | long[] \| null | 골라 두고 아직 보내지 않은 그룹 |

### GroupDetail

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | long | |
| `name`, `description`, `color` | string | |
| `photoUrl` | string \| null | `/api/v1/groups/{id}/photo?v=…` |
| `inviteCode` | string | 6자리 대문자·숫자 |
| `ownerId` | long | |
| `owner` | boolean | 내가 그룹장인지 |
| `members[]` | `{ user: UserSimple, owner: boolean, joinedAt: datetime }` | |
| `createdAt` | datetime | |

### Topic

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | string | |
| `category`, `title` | string | |
| `cards` | string[] | 본문 카드 |
| `sources[]` | `{ label, url }` | 출처 |
| `checkedOn` | date \| null | 검증일. `null`이면 보류 |
| `commentCount` | long | |

### Comment

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | long | |
| `topicId` | string | |
| `parentId` | long \| null | 답글이면 원 댓글 ID |
| `author` | [UserSimple](#usersimple) | 생각 탭은 실명(아이디) 표시 |
| `text` | string | |
| `likeCount` | int | |
| `liked` | boolean | 내가 공감했는지 |
| `mine` | boolean | 내 댓글인지 |
| `createdAt` | datetime | |
| `replies` | Comment[] | 원 댓글만 채움. 답글이면 `[]` |

### AdminQuestion

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id`, `date`, `title`, `body` | | |
| `status` | string | `DRAFT` \| `PUBLISHED` |
| `choices[]` | `{ id, text, shortText }` | |
| `perspectives` | [AdminPerspective](#adminperspective)[] | 모든 상태 포함 |
| `participantCount` | long | |
| `maxChoicePct` | int | 가장 많이 고른 선택지의 비율 (반올림) |
| `needsReview` | boolean | 응답이 있고 한 선택지가 80%를 넘으면 `true` (재검토 대상) |

### AdminPerspective

| 필드 | 타입 | 설명 |
| --- | --- | --- |
| `id` | long | |
| `questionId` | string | |
| `label`, `body` | string | |
| `verse`, `ref` | string \| null | |
| `sortOrder` | int | |
| `status` | string | `DRAFT` \| `APPROVED` \| `REJECTED` \| `FAILED` |
| `model`, `promptVersion` | string \| null | |
| `reviewedBy` | long \| null | 검수한 관리자 ID |
| `reviewedAt` | datetime \| null | |
| `createdAt` | datetime | |
