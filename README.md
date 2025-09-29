# Shutter Heroes Backend

생물 목격 정보 공유 플랫폼 백엔드 서버

## 📋 프로젝트 개요

Shutter Heroes는 사용자들이 목격한 생물(특히 보호종)의 사진을 업로드하고, AI가 자동으로 종을 식별하며, 커뮤니티가 상호작용(댓글, 좋아요)할 수 있는 생물 다양성 기록 플랫폼입니다.

## 🛠 기술 스택

### Framework & Language
- **Java 21**
- **Spring Boot 3.5.6**
- **Spring Security** (JWT, OAuth2)
- **Spring Data JPA**

### Database
- **PostgreSQL 42.7.4**
- **PostGIS** (공간 데이터 처리)
- **Hibernate Spatial**

### Authentication
- **JWT** (JSON Web Token)
- **OAuth2** (카카오 소셜 로그인)

### Documentation & Testing
- **Swagger/OpenAPI 3.0**
- **JUnit 5**
- **Spring REST Docs**

### Build Tool
- **Gradle**

## 📊 도메인 엔티티 구조

### 1. **User (사용자)**
**역할**: 시스템 사용자 정보 관리
- 이메일, 비밀번호, 닉네임, 아바타 이미지
- 사용자 권한 (USER/ADMIN)
- OAuth2 및 일반 로그인 지원

### 2. **Species (생물종)**
**역할**: 생물종 정보 관리
- 한국명, 영문명, 학명
- 보호 상태 (COMMON/PROTECTED/ENDANGERED)
- 보호종 코드 및 관련 정보

### 3. **Sighting (목격 정보)** - 핵심 엔티티
**역할**: 생물 목격 정보의 중심
- 제목, 설명, 목격 일시
- 위치 정보 (PostGIS Point 타입)
- 탐지 방법 (USER/AI)
- 공개 범위 (PUBLIC/FRIENDS_ONLY/PRIVATE)
- AI 신뢰도, 검증 여부

### 4. **Media (미디어)**
**역할**: 업로드된 이미지/동영상 파일 정보
- 파일 경로, MIME 타입
- 크기 정보 (너비, 높이, 바이트)
- 체크섬 (중복 확인용)

### 5. **Comment (댓글)**
**역할**: 목격 정보에 대한 댓글
- 댓글 내용
- 작성 및 수정 시간 관리

### 6. **Like (좋아요)**
**역할**: 목격 정보에 대한 좋아요
- 복합 기본키 사용 (sighting_id + user_id)
- 중복 좋아요 방지 메커니즘

### 7. **Report (신고)**
**역할**: 부적절한 콘텐츠 신고 관리
- 신고 사유
- 해결 여부 및 해결 시간 추적

### 8. **AiDetection (AI 탐지)**
**역할**: AI 이미지 분석 결과 저장
- 탐지된 객체 레이블
- 신뢰도 점수
- 바운딩 박스 좌표 (xMin, yMin, xMax, yMax)

## 🔗 엔티티 관계 다이어그램

```
User ──┬── (1:N) ──> Sighting (작성한 목격 정보)
       ├── (1:N) ──> Media (업로드한 미디어)
       ├── (1:N) ──> Comment (작성한 댓글)
       ├── (1:N) ──> Like (좋아요한 목격 정보)
       └── (1:N) ──> Report (신고한 콘텐츠)

Sighting ──┬── (N:1) ──> User (작성자)
           ├── (N:1) ──> Species (목격된 생물종)
           ├── (N:1) ──> Media (첨부 미디어)
           ├── (1:N) ──> Comment (받은 댓글들)
           ├── (1:N) ──> Like (받은 좋아요들)
           └── (1:N) ──> Report (받은 신고들)

Media ──┬── (N:1) ──> User (업로더)
        └── (1:N) ──> AiDetection (AI 분석 결과들)

Species ──── (1:N) ──> Sighting (관련 목격 정보들)

Comment ──┬── (N:1) ──> Sighting (댓글이 달린 목격 정보)
          └── (N:1) ──> User (댓글 작성자)

Like ──┬── (N:1) ──> Sighting (좋아요한 목격 정보)
       └── (N:1) ──> User (좋아요한 사용자)

Report ──┬── (N:1) ──> Sighting (신고된 목격 정보)
         └── (N:1) ──> User (신고자)

AiDetection ──── (N:1) ──> Media (분석된 미디어)
```
## 🏃‍♂️ 프로젝트 실행 방법

### 사전 요구사항
- Java 21
- PostgreSQL with PostGIS extension
- Gradle

### 데이터베이스 설정
1. PostgreSQL 설치 및 PostGIS 확장 활성화
2. `app` 스키마 생성
3. `application-local.properties`의 데이터베이스 연결 정보 수정

### 애플리케이션 실행
```bash
# 프로젝트 빌드
./gradlew build

# 애플리케이션 실행 (로컬 프로파일)
./gradlew bootRun

# 테스트 실행
./gradlew test
```

### Swagger UI 접속
```
http://localhost:8080/swagger-ui.html
```

## 📝 환경 설정

### application.properties
- JWT 토큰 유효기간: 24시간
- 기본 프로파일: local
- JPA DDL: none (수동 관리)

### application-local.properties
- PostgreSQL: localhost:5433/postgres
- 스키마: app
- OAuth2 리다이렉트: http://localhost:3000

## 🔐 보안
- Spring Security를 통한 인증/인가
- JWT 토큰 기반 무상태 인증
- 카카오 OAuth2 소셜 로그인
- 비밀번호 BCrypt 암호화
