# Once-Upon-A-Time-BE
옛날 옛적, AI가 들려주는 나만의 동화 📖


## 📁 폴더 구조 (도메인형 DDD 기반)

```
📂 once_upon_a_time
├── 📂 domain
│   ├── 📂 member
│   │   ├── 📂 controller
│   │   ├── 📂 service
│   │   ├── 📂 repository
│   │   ├── 📂 domain
│   │   └── 📂 dto
│   │
│   ├── 📂 book
│   │   ├── 📂 controller
│   │   ├── 📂 service
│   │   ├── 📂 repository
│   │   ├── 📂 domain
│   │   └── 📂 dto
│   │
│   └── 📂 (추가 도메인들)
│
├── 📂 auth
│   ├── 📂 config          # SecurityConfig 등
│   ├── 📂 jwt             # JwtProvider, JwtUtil 등
│   ├── 📂 filter          # JwtFilter, AuthFilter 등
│   └── 📂 userdetails     # CustomUserDetails, UserDetailsService 등
│
├── 📂 global
│   ├── 📂 common          # 공통 응답(Response), Util
│   └── 📂 exception       # 전역 예외 처리
│
└── 📂 config              # Swagger, JPA, Redis 등 전역 설정
```

---

## 🔖 브랜치 전략 (GitHub Flow 기반)

- `main`: 제품 출시 브랜치 (배포 대상)
- `dev`: 통합 개발 브랜치 
- `feat/feature-name`: 기능 개발용 브랜치
- `fix/bug-name`: 버그 수정 브랜치
- `hotfix/critical-name`: 긴급 수정 브랜치

---

## 💬 PR 규칙

- 제목 형식: `[Feat] 로그인 API 구현`
- 템플릿 기반 작성 (유형 / 작업 내용 / 리뷰 포인트 등)

---

## ✅ 커밋 컨벤션

```bash
<태그>: <제목>

- 작업 내용 상세
- 작업 내용 상세2

#이슈번호 (optional)

```

### 예시

- `feat: 회원가입 API 구현`
- `fix: 회원가입 시 닉네임 중복 오류 수정`

---

## 💠 Gitmoji 가이드

| Gitmoji | 태그 | 설명 |
| --- | --- | --- |
| ✨ | feat | 새로운 기능 |
| 🐛 | fix | 버그 수정 |
| ♻️ | refactor | 리팩토링 |
| 🎨 | style | 코드 스타일 변경 |
| 📝 | docs | 문서 수정 |
| 🔧 | chore | 설정 변경 |
| ✅ | test | 테스트 코드 |
| 🚀 | hotfix | 긴급 배포 이슈 |
| 🔀 | merge | 브랜치 병합 |

---

## 🧾 Database 규칙

- **테이블 명**: `lower_snake_case`
- **PK 컬럼명**: `id`
- **기본 날짜 컬럼**: `created_at`, `updated_at`, `deleted_at`
- **작성자/수정자 필드**: `created_by`, `updated_by`
- **FK 명명 규칙**: `{참조테이블}_id`

---
