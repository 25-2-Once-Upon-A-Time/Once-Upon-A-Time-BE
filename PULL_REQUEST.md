name: 'PULL REQUEST'
about: PR을 올릴 때 사용하는 기본 템플릿입니다.
title: 'feat: 카카오 로그인 및 회원가입 구현'
labels: ['feat', 'auth', 'kakao']
assignees: ''

---

## 🚀 Related Issue
- #이슈번호 (해당하는 이슈 번호를 입력해주세요)

## 📄 Description
카카오 OAuth 인증을 Spring Security OAuth2 Client 없이 REST API 방식으로 직접 구현했습니다.

**주요 기능:**
- **카카오 인가 URL 생성:** `GET /api/v1/auth/kakao/url` 엔드포인트를 통해 프론트엔드에 카카오 로그인 URL을 제공합니다.
- **카카오 로그인 콜백 처리:** `GET /api/v1/auth/kakao/callback` 엔드포인트에서 카카오로부터 받은 인가 코드를 처리하여 카카오 토큰 및 사용자 정보를 조회합니다.
  - 기존 회원인 경우: JWT 기반 액세스 토큰과 리프레시 토큰을 발급하여 로그인 완료합니다.
  - 신규 회원인 경우: 추가 정보 입력을 위한 `kakaoUserId`를 응답하여 회원가입을 유도합니다.
- **카카오 계정 회원가입:** `POST /api/v1/auth/signup/kakao` 엔드포인트를 통해 신규 회원의 추가 정보(이름, 성별, 생년월일, 닉네임, 전화번호)를 받아 회원가입을 완료합니다.
- **로그아웃:** `POST /api/v1/auth/logout` 엔드포인트를 통해 JWT 기반 로그아웃(RefreshToken 삭제)을 처리합니다.

**시스템 구조 요약:**
- OAuth 인증 과정 전체를 REST API 방식으로 직접 구현하여 Spring Security는 JWT 인증/인가만 담당하도록 했습니다.
- `인가 코드 → 카카오 토큰 → 카카오 유저 정보` 흐름을 백엔드에서 직접 처리합니다.

## ✅ Test Checklist
- [x] 테스트 코드를 작성하셨나요? (필요시 추가 예정)
- [x] 직접 Postman이나 DB를 통해 확인해 보셨나요? (예: `GET /api/v1/auth/kakao/url`, `GET /api/v1/auth/kakao/callback` 흐름 및 `POST /api/v1/auth/signup/kakao`, `POST /api/v1/auth/logout` 동작 확인)

## 📸 Screenshots
(Postman 요청/응답 스크린샷, DB 테이블 확인 스크린샷 등 관련 스크린샷을 첨부해주세요.)
