아래는 Gemini / Codex에게 그대로 보여주기 좋은 .md 파일 형식으로 깔끔하게 정리한 카카오 로그인 API 명세서(Kakao Login API Specification) 입니다.

⸻

📌 Kakao Login API Specification

⸻


# 📌 Kakao Login API Specification

카카오 OAuth 인증을 Spring Security OAuth2 Client 없이  
REST API 방식으로 직접 구현하기 위한 명세서입니다.

백엔드는 카카오 REST API를 직접 호출하여  
`인가 코드 → 카카오 토큰 → 카카오 유저 정보` 흐름을 처리합니다.

---

# ✅ 1. GET `/api/v1/auth/kakao/url`

## ✔ 설명
백엔드가 카카오 인가 URL을 생성해 프론트로 전달합니다.

## ✔ 요청 헤더
없음

## ✔ 요청 바디
없음

## ✔ 응답 예시
```json
{
  "code": "SUCCESS",
  "message": "카카오 로그인 URL 생성 성공",
  "data": {
    "redirectUrl": "https://kauth.kakao.com/oauth/authorize?client_id=xxx&redirect_uri=xxx&response_type=code"
  }
}


⸻

✅ 2. GET /api/v1/auth/kakao/callback?code=xxx

✔ 설명

카카오 로그인 후 카카오가 백엔드로 전달한 code를 받아서 처리합니다.

백엔드 처리 과정:
	1.	카카오 토큰 발급 API 호출
	2.	액세스 토큰으로 카카오 사용자 정보 조회
	3.	DB에서 기존 회원인지 확인
	4.	신규 / 기존 여부에 따라 응답 분기

⸻

🔹 응답 케이스 1 — 기존 회원 로그인

{
  "code": "SUCCESS",
  "message": "카카오 로그인 성공",
  "data": {
    "isNewUser": false,
    "accessToken": "ACCESS_TOKEN",
    "refreshToken": "REFRESH_TOKEN"
  }
}


⸻

🔹 응답 케이스 2 — 신규 회원 (추가 정보 필요)

{
  "code": "SUCCESS",
  "message": "회원가입을 위한 추가 정보 필요",
  "data": {
    "isNewUser": true,
    "kakaoUserId": "1234567890"
  }
}


⸻

✅ 3. POST /api/v1/auth/signup/kakao

✔ 설명

카카오 계정으로 최초 회원가입을 수행합니다.

✔ 요청 헤더

Header	Required	Description
Content-Type	O	application/json


⸻

✔ 요청 바디

{
  "kakaoUserId": "string",
  "name": "string",
  "gender": "string",
  "birth": "YYYY.MM.DD",
  "nickname": "string",
  "localPhone": "string",
  "personalPhone": "string"
}


⸻

✔ 응답 예시 (회원가입 + 로그인 완료)

{
  "code": "SUCCESS",
  "message": "회원가입 완료",
  "data": {
    "accessToken": "ACCESS_TOKEN",
    "refreshToken": "refreshToken"
  }
}


⸻

✅ 4. POST /api/v1/auth/logout

✔ 설명

JWT 기반 로그아웃 처리.
RefreshToken 삭제 방식으로 로그아웃 수행.

✔ 요청 헤더

Header	Required	Description
Authorization	O	Bearer ACCESS_TOKEN


⸻

✔ 요청 바디

없음

⸻

✔ 응답 예시

{
  "code": "SUCCESS",
  "message": "로그아웃 완료",
  "data": null
}


⸻

🔧 시스템 구조 요약
	•	OAuth 인증 과정은 전부 REST API 방식으로 직접 구현
	•	Spring Security는 JWT 인증/인가만 담당
	•	카카오 OAuth 과정:
	•	백엔드 → redirect URL 생성
	•	프론트 → 카카오 로그인 진행
	•	카카오 → 우리 서버에 code 전달
	•	백엔드 → code로 토큰 요청 → 사용자 정보 요청
	•	DB에서 기존 회원 여부 확인 후:
	•	isNewUser = false: 로그인 완료
	•	isNewUser = true: 프론트가 추가 정보 입력 페이지 보여줌

⸻

📌 끝.

---

# ☑️ 이 파일을 그대로 복사해서  
`KakaoLoginAPI.md` 파일로 만들면 Gemini 또는 Codex에게 문제 없이 보여줄 수 있어.

필요하면 **프로젝트 전체 구조 API 명세서**, **Swagger 버전**, **ERD 포함된 기술 문서 버전**도 만들어줄까?
⸻

📌 Kakao Login API Specification

📁 File: KakaoLoginAPI.md

⸻


# 📌 Kakao Login API Specification

카카오 OAuth 인증을 Spring Security OAuth2 Client 없이  
REST API 방식으로 직접 구현하기 위한 명세서입니다.

백엔드는 카카오 REST API를 직접 호출하여  
`인가 코드 → 카카오 토큰 → 카카오 유저 정보` 흐름을 처리합니다.

---

# ✅ 1. GET `/api/v1/auth/kakao/url`

## ✔ 설명
백엔드가 카카오 인가 URL을 생성해 프론트로 전달합니다.

## ✔ 요청 헤더
없음

## ✔ 요청 바디
없음

## ✔ 응답 예시
```json
{
  "code": "SUCCESS",
  "message": "카카오 로그인 URL 생성 성공",
  "data": {
    "redirectUrl": "https://kauth.kakao.com/oauth/authorize?client_id=xxx&redirect_uri=xxx&response_type=code"
  }
}


⸻

✅ 2. GET /api/v1/auth/kakao/callback?code=xxx

✔ 설명

카카오 로그인 후 카카오가 백엔드로 전달한 code를 받아서 처리합니다.

백엔드 처리 과정:
	1.	카카오 토큰 발급 API 호출
	2.	액세스 토큰으로 카카오 사용자 정보 조회
	3.	DB에서 기존 회원인지 확인
	4.	신규 / 기존 여부에 따라 응답 분기

⸻

🔹 응답 케이스 1 — 기존 회원 로그인

{
  "code": "SUCCESS",
  "message": "카카오 로그인 성공",
  "data": {
    "isNewUser": false,
    "accessToken": "ACCESS_TOKEN",
    "refreshToken": "REFRESH_TOKEN"
  }
}


⸻

🔹 응답 케이스 2 — 신규 회원 (추가 정보 필요)

{
  "code": "SUCCESS",
  "message": "회원가입을 위한 추가 정보 필요",
  "data": {
    "isNewUser": true,
    "kakaoUserId": "1234567890"
  }
}


⸻

✅ 3. POST /api/v1/auth/signup/kakao

✔ 설명

카카오 계정으로 최초 회원가입을 수행합니다.

✔ 요청 헤더

Header	Required	Description
Content-Type	O	application/json


⸻

✔ 요청 바디

{
  "kakaoUserId": "string",
  "name": "string",
  "gender": "string",
  "birth": "YYYY.MM.DD",
  "nickname": "string",
  "localPhone": "string",
  "personalPhone": "string"
}


⸻

✔ 응답 예시 (회원가입 + 로그인 완료)

{
  "code": "SUCCESS",
  "message": "회원가입 완료",
  "data": {
    "accessToken": "ACCESS_TOKEN",
    "refreshToken": "refreshToken"
  }
}


⸻

✅ 4. POST /api/v1/auth/logout

✔ 설명

JWT 기반 로그아웃 처리.
RefreshToken 삭제 방식으로 로그아웃 수행.

✔ 요청 헤더

Header	Required	Description
Authorization	O	Bearer ACCESS_TOKEN


⸻

✔ 요청 바디

없음

⸻

✔ 응답 예시

{
  "code": "SUCCESS",
  "message": "로그아웃 완료",
  "data": null
}


⸻

🔧 시스템 구조 요약
	•	OAuth 인증 과정은 전부 REST API 방식으로 직접 구현
	•	Spring Security는 JWT 인증/인가만 담당
	•	카카오 OAuth 과정:
	•	백엔드 → redirect URL 생성
	•	프론트 → 카카오 로그인 진행
	•	카카오 → 우리 서버에 code 전달
	•	백엔드 → code로 토큰 요청 → 사용자 정보 요청
	•	DB에서 기존 회원 여부 확인 후:
	•	isNewUser = false: 로그인 완료
	•	isNewUser = true: 프론트가 추가 정보 입력 페이지 보여줌

⸻

📌 끝.
