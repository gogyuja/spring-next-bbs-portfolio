# backend/CLAUDE.md

> 백엔드(Java/Spring) 코드 규칙. BE 파일을 작업할 때만 로드된다.
> 전체 공통 규칙은 루트 CLAUDE.md 참조.

---

## 네이밍

| 대상 | 규칙 | 예시 |
| --- | --- | --- |
| 클래스 | PascalCase | `PostService` |
| 메서드/변수 | camelCase | `createPost` |
| 상수 | UPPER_SNAKE | `MAX_FILE_SIZE` |
| 패키지 | 소문자 | `com.example.bbs.post` |
| 테스트 메서드 | 한글 서술 가능 | `정지된_회원은_게시글을_작성할_수_없다` |

---

## 레이어 책임

```
Controller  → 얇게. 요청 검증 + Service 호출 + 응답 변환만. 비즈니스 로직 금지
Service     → @Transactional 경계. 비즈니스 로직 담당
Domain      → 핵심 규칙. 상태 변경은 메서드로 (setter 금지)
Repository  → 데이터 접근. 복잡 쿼리는 QueryDSL (query/ 패키지)
DTO         → record 사용. Request / Response 분리
```

---

## 핵심 규칙

- 엔티티에 `@Setter` 금지. 생성은 정적 팩토리(`of`, `create`), 변경은 의미 있는 메서드(`updateContent`)
- 엔티티 직접 반환 금지. 항상 Response DTO로 변환
- 연관관계는 `FetchType.LAZY` 기본. 목록 조회는 fetchJoin으로 N+1 방지
- 모든 예외는 커스텀 예외 → `GlobalExceptionHandler`에서 RFC 7807로 변환
- `Optional`은 반환 타입에만. 필드/파라미터에 쓰지 않음
- 매직 넘버/문자열은 상수로

---

## 패키지 구조 (도메인별)

```
com.example.bbs.{domain}/
├── controller/    {Domain}Controller
├── service/       {Domain}Service
├── domain/        {Domain} (Entity)
├── dto/           {Domain}CreateRequest, {Domain}DetailResponse ...
├── repository/    {Domain}Repository (JPA)
│   └── query/     {Domain}QueryRepository (QueryDSL)
└── exception/     {Domain}NotFoundException ...

com.example.bbs.global/
├── config/        SecurityConfig, RedisConfig, S3Config ...
├── exception/     GlobalExceptionHandler, ErrorCode
├── response/      ApiResponse<T>, ProblemDetail
└── util/          JwtUtil, FileUtil
```

---

## 도구 (자동 강제)

`.editorconfig` (Java): UTF-8, LF, 들여쓰기 4칸.

Spotless + Google Java Style (`build.gradle`):

```groovy
plugins { id 'com.diffplug.spotless' version '6.25.0' }
spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
    }
}
```

```bash
./gradlew spotlessApply   # 자동 정리
./gradlew spotlessCheck   # 검사 (CI)
```

---

## 데이터 레이어 규칙

- 기본키: `@GeneratedValue(IDENTITY)` (MySQL AUTO_INCREMENT)
- 감사 필드: 공통 `BaseEntity`에 `@CreatedDate`, `@LastModifiedDate`
- 소프트 삭제: `deleted_at` + 조회 시 필터링
- Redis 용도: Refresh Token, Access Token 블랙리스트, 게시글 조회수 캐시
- DB 스키마 변경: Flyway `V{n}__{설명}.sql` 추가. **커밋된 마이그레이션 파일은 수정 금지**
