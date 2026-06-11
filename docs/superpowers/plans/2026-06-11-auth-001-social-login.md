# AUTH-001 소셜 로그인/회원가입 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 카카오·구글 소셜 로그인, 자동 회원가입, OTC 기반 JWT 발급까지 BE+FE 완성

**Architecture:** BE-driven OAuth — Spring Security가 전체 OAuth 플로우 처리. 로그인 성공 시 OTC(30초 Redis)를 발급해 FE가 백채널 POST로 AT를 교환. RT는 HttpOnly 쿠키로 전달.

**Tech Stack:** Java 21 / Spring Boot 3.3.x / Spring Security OAuth2 / JJWT 0.12.x / Redis / MySQL 8 / Next.js 15 App Router / TypeScript / Zustand / TanStack Query v5 / next-auth v5

---

## 파일 구조

```
root/
├── openapi.yaml                              NEW
├── docker-compose.yml                        NEW
├── .env.example                              NEW
backend/                                      (feature/be/auth-social-login)
├── build.gradle                              NEW
├── settings.gradle                           NEW
├── gradlew / gradlew.bat / gradle/wrapper/   NEW (Spring Initializr)
├── .editorconfig                             NEW
├── src/main/resources/
│   ├── application.yml                       NEW
│   ├── application-local.yml.example         NEW
│   └── db/migration/V1__create_users_table.sql  NEW
└── src/main/java/com/example/bbs/
    ├── BbsApplication.java                   NEW
    ├── global/
    │   ├── config/SecurityConfig.java        NEW
    │   ├── config/RedisConfig.java           NEW
    │   ├── config/CorsConfig.java            NEW
    │   ├── exception/BusinessException.java  NEW
    │   ├── exception/ErrorCode.java          NEW
    │   ├── exception/GlobalExceptionHandler.java NEW
    │   ├── response/ApiResponse.java         NEW
    │   └── util/JwtUtil.java                 NEW
    ├── auth/
    │   ├── controller/AuthController.java    NEW
    │   ├── service/CustomOAuth2UserService.java NEW
    │   ├── service/AuthTokenService.java     NEW
    │   ├── handler/OAuth2LoginSuccessHandler.java NEW
    │   ├── handler/OAuth2LoginFailureHandler.java NEW
    │   ├── dto/OAuth2UserInfo.java           NEW
    │   ├── dto/TokenRequest.java             NEW
    │   ├── dto/TokenResponse.java            NEW
    │   ├── domain/OAuth2Provider.java        NEW
    │   └── security/CustomUserDetails.java   NEW
    └── user/
        ├── domain/BaseEntity.java            NEW
        ├── domain/User.java                  NEW
        ├── domain/UserRole.java              NEW
        └── repository/UserRepository.java    NEW
src/test/java/com/example/bbs/
    ├── global/util/JwtUtilTest.java          NEW
    ├── auth/service/AuthTokenServiceTest.java NEW
    └── auth/service/CustomOAuth2UserServiceTest.java NEW
frontend/                                     (feature/fe/auth-social-login)
├── package.json / pnpm-lock.yaml / configs   NEW (create-next-app)
└── src/
    ├── app/login/page.tsx                    NEW
    ├── app/auth/callback/page.tsx            NEW
    ├── app/auth/callback/__tests__/page.test.tsx NEW
    ├── components/atoms/SocialLoginButton.tsx NEW
    ├── stores/authStore.ts                   NEW
    ├── hooks/useAuth.ts                      NEW
    ├── lib/axios.ts                          NEW
    ├── types/api.ts                          NEW (generated)
    ├── mocks/handlers.ts                     NEW
    ├── auth.ts                               NEW
    └── middleware.ts                         NEW
```

---

## Phase 0: Pre-Setup (develop 브랜치)

### Task 0: develop 브랜치 + openapi.yaml

**Files:**
- Create: `openapi.yaml`

- [ ] **Step 0-1: develop 브랜치 생성**

```bash
git checkout -b develop main
```

- [ ] **Step 0-2: openapi.yaml 작성**

```yaml
# openapi.yaml (repo root)
openapi: "3.0.3"
info:
  title: BBS API
  version: "1.0.0"
  description: Spring Next BBS Portfolio API

servers:
  - url: http://localhost:8080
    description: Local

paths:
  /api/auth/token:
    post:
      tags: [auth]
      summary: OTC를 Access Token으로 교환
      operationId: exchangeToken
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/TokenRequest'
      responses:
        '200':
          description: 토큰 발급 성공
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/TokenApiResponse'
        '400':
          $ref: '#/components/responses/ProblemDetail'

  /api/auth/logout:
    post:
      tags: [auth]
      summary: 로그아웃 (AUTH-002 구현 예정)
      operationId: logout
      responses:
        '200':
          description: 구현 예정

  /api/auth/reissue:
    post:
      tags: [auth]
      summary: 토큰 재발급 (AUTH-003 구현 예정)
      operationId: reissue
      responses:
        '200':
          description: 구현 예정

components:
  schemas:
    TokenRequest:
      type: object
      required: [code]
      properties:
        code:
          type: string
          format: uuid

    TokenResponse:
      type: object
      properties:
        accessToken:
          type: string

    ApiResponse:
      type: object
      properties:
        success:
          type: boolean
        data:
          type: object
        message:
          type: string

    TokenApiResponse:
      allOf:
        - $ref: '#/components/schemas/ApiResponse'
        - type: object
          properties:
            data:
              $ref: '#/components/schemas/TokenResponse'

  responses:
    ProblemDetail:
      description: RFC 7807 Problem Details
      content:
        application/problem+json:
          schema:
            type: object
            properties:
              type: { type: string }
              title: { type: string }
              status: { type: integer }
              detail: { type: string }
```

- [ ] **Step 0-3: 커밋**

```bash
git add openapi.yaml
git commit -m "docs: add auth api spec to openapi.yaml"
```

---

## Phase 1: Infrastructure (develop 브랜치)

### Task 1: Docker Compose + .env.example

**Files:**
- Create: `docker-compose.yml`
- Create: `.env.example`

- [ ] **Step 1-1: docker-compose.yml 작성**

```yaml
# docker-compose.yml
services:
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: bbs
      MYSQL_USER: bbs
      MYSQL_PASSWORD: bbs
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 3s
      retries: 3

volumes:
  mysql_data:
```

- [ ] **Step 1-2: .env.example 작성**

```bash
# .env.example — 복사해서 .env 로 사용, 실제 값 입력
KAKAO_CLIENT_ID=your-kakao-client-id
KAKAO_CLIENT_SECRET=your-kakao-client-secret
GOOGLE_CLIENT_ID=your-google-client-id
GOOGLE_CLIENT_SECRET=your-google-client-secret
# Base64 인코딩된 32바이트 이상 임의 문자열
JWT_SECRET=base64-encoded-256bit-secret
FRONTEND_URL=http://localhost:3000
COOKIE_SECURE=false
COOKIE_SAME_SITE=Lax
```

- [ ] **Step 1-3: .gitignore에 .env 추가 확인**

```bash
echo ".env" >> .gitignore
echo "backend/application-local.yml" >> .gitignore
```

- [ ] **Step 1-4: 커밋**

```bash
git add docker-compose.yml .env.example .gitignore
git commit -m "chore: add docker-compose and env example"
```

- [ ] **Step 1-5: 컨테이너 기동 확인**

```bash
cp .env.example .env   # 실제 키 없이도 DB/Redis는 올라옴
docker compose up -d mysql redis
docker compose ps      # Status: healthy 확인
```

Expected: mysql, redis 컨테이너 healthy 상태

---

## Phase 2: BE (feature/be/auth-social-login)

### Task 2: Spring Boot 프로젝트 스캐폴딩

**Files:** `backend/` 전체 초기화

- [ ] **Step 2-1: develop에서 BE 브랜치 생성**

```bash
git checkout develop
git checkout -b feature/be/auth-social-login
```

- [ ] **Step 2-2: Spring Initializr로 프로젝트 생성**

```bash
curl -G "https://start.spring.io/starter.tgz" \
  -d "type=gradle-project" \
  -d "language=java" \
  -d "bootVersion=3.3.5" \
  -d "groupId=com.example" \
  -d "artifactId=bbs" \
  -d "packageName=com.example.bbs" \
  -d "javaVersion=21" \
  -d "dependencies=web,security,oauth2-client,data-jpa,data-redis,mysql,flyway,lombok" \
  | tar -xz -C backend/ --strip-components=1
```

- [ ] **Step 2-3: build.gradle 교체 (JJWT, Springdoc, Testcontainers 추가)**

```groovy
// backend/build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.6'
    id 'com.diffplug.spotless' version '6.25.0'
}

group = 'com.example'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly { extendsFrom annotationProcessor }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-security'
    implementation 'org.springframework.boot:spring-boot-starter-oauth2-client'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-data-redis'
    implementation 'com.mysql:mysql-connector-j'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-mysql'
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.security:spring-security-test'
    testImplementation platform('org.testcontainers:testcontainers-bom:1.20.4')
    testImplementation 'org.testcontainers:junit-jupiter'
    testImplementation 'org.testcontainers:mysql'
    testImplementation 'org.testcontainers:testcontainers'
}

tasks.named('test') {
    useJUnitPlatform()
}

spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
    }
}
```

- [ ] **Step 2-4: application.yml 작성**

```yaml
# backend/src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mysql://${DB_HOST:localhost}:3306/bbs?useSSL=false&serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: ${DB_USER:bbs}
    password: ${DB_PASSWORD:bbs}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MySQL8Dialect
  flyway:
    enabled: true
    locations: classpath:db/migration
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: ${KAKAO_CLIENT_ID}
            client-secret: ${KAKAO_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/login/oauth2/code/kakao"
            authorization-grant-type: authorization_code
            scope: profile_nickname, profile_image
            client-authentication-method: client_secret_post
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            redirect-uri: "{baseUrl}/login/oauth2/code/google"
            scope: profile, email
        provider:
          kakao:
            authorization-uri: https://kauth.kakao.com/oauth/authorize
            token-uri: https://kauth.kakao.com/oauth/token
            user-info-uri: https://kapi.kakao.com/v2/user/me
            user-name-attribute: id

jwt:
  secret: ${JWT_SECRET}
  access-token-expiry: 1800000
  refresh-token-expiry: 604800000

frontend:
  url: ${FRONTEND_URL:http://localhost:3000}

cookie:
  secure: ${COOKIE_SECURE:false}
  same-site: ${COOKIE_SAME_SITE:Lax}
```

- [ ] **Step 2-5: application-local.yml.example 작성**

```yaml
# backend/src/main/resources/application-local.yml.example
# 복사 → application-local.yml (gitignore) 후 실제 값 입력
spring:
  security:
    oauth2:
      client:
        registration:
          kakao:
            client-id: YOUR_KAKAO_CLIENT_ID
            client-secret: YOUR_KAKAO_CLIENT_SECRET
          google:
            client-id: YOUR_GOOGLE_CLIENT_ID
            client-secret: YOUR_GOOGLE_CLIENT_SECRET
jwt:
  secret: YOUR_BASE64_256BIT_SECRET
```

- [ ] **Step 2-6: BbsApplication.java 수정 (@EnableJpaAuditing 추가)**

```java
// backend/src/main/java/com/example/bbs/BbsApplication.java
package com.example.bbs;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class BbsApplication {
    public static void main(String[] args) {
        SpringApplication.run(BbsApplication.class, args);
    }
}
```

- [ ] **Step 2-7: 빌드 확인 (컴파일만)**

```bash
cd backend && ./gradlew compileJava
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 2-8: 커밋**

```bash
git add backend/
git commit -m "chore(be): spring boot project scaffolding"
```

---

### Task 3: 글로벌 레이어

**Files:**
- Create: `backend/src/main/java/com/example/bbs/global/response/ApiResponse.java`
- Create: `backend/src/main/java/com/example/bbs/global/exception/ErrorCode.java`
- Create: `backend/src/main/java/com/example/bbs/global/exception/BusinessException.java`
- Create: `backend/src/main/java/com/example/bbs/global/exception/GlobalExceptionHandler.java`

- [ ] **Step 3-1: ApiResponse.java**

```java
package com.example.bbs.global.response;

import lombok.Getter;

@Getter
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String message;

    private ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, data, message);
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }
}
```

- [ ] **Step 3-2: ErrorCode.java**

```java
package com.example.bbs.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    OTC_INVALID(HttpStatus.BAD_REQUEST, "코드가 없거나 만료되었습니다"),
    LOGIN_BANNED(HttpStatus.FORBIDDEN, "로그인이 금지된 계정입니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류");

    private final HttpStatus status;
    private final String message;
}
```

- [ ] **Step 3-3: BusinessException.java**

```java
package com.example.bbs.global.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
```

- [ ] **Step 3-4: GlobalExceptionHandler.java**

```java
package com.example.bbs.global.exception;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handle(BusinessException e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            e.getErrorCode().getStatus(), e.getMessage());
        pd.setTitle(e.getErrorCode().name());
        return ResponseEntity.status(e.getErrorCode().getStatus()).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handle(Exception e) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
            org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류");
        return ResponseEntity.internalServerError().body(pd);
    }
}
```

- [ ] **Step 3-5: 커밋**

```bash
git add backend/src/main/java/com/example/bbs/global/
git commit -m "feat(be): add global response and exception layer"
```

---

### Task 4: 도메인 레이어 (User + Flyway)

**Files:**
- Create: `backend/src/main/java/com/example/bbs/user/domain/BaseEntity.java`
- Create: `backend/src/main/java/com/example/bbs/user/domain/User.java`
- Create: `backend/src/main/java/com/example/bbs/user/domain/UserRole.java`
- Create: `backend/src/main/java/com/example/bbs/auth/domain/OAuth2Provider.java`
- Create: `backend/src/main/java/com/example/bbs/user/repository/UserRepository.java`
- Create: `backend/src/main/resources/db/migration/V1__create_users_table.sql`

- [ ] **Step 4-1: OAuth2Provider.java**

```java
package com.example.bbs.auth.domain;

public enum OAuth2Provider {
    KAKAO, GOOGLE
}
```

- [ ] **Step 4-2: UserRole.java**

```java
package com.example.bbs.user.domain;

public enum UserRole {
    USER, MANAGER, ADMIN
}
```

- [ ] **Step 4-3: BaseEntity.java**

```java
package com.example.bbs.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4-4: User.java**

```java
package com.example.bbs.user.domain;

import com.example.bbs.auth.domain.OAuth2Provider;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OAuth2Provider provider;

    @Column(nullable = false, length = 100)
    private String providerId;

    @Column(length = 255)
    private String email;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(length = 500)
    private String profileImage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role;

    private LocalDateTime bannedAt;
    private LocalDateTime banExpiresAt;
    private LocalDateTime deletedAt;

    public static User create(OAuth2Provider provider, String providerId,
                               String email, String nickname, String profileImage) {
        User user = new User();
        user.provider = provider;
        user.providerId = providerId;
        user.email = email;
        user.nickname = nickname;
        user.profileImage = profileImage;
        user.role = UserRole.USER;
        return user;
    }

    public boolean isBanned() {
        return banExpiresAt != null && banExpiresAt.isAfter(LocalDateTime.now());
    }
}
```

- [ ] **Step 4-5: UserRepository.java**

```java
package com.example.bbs.user.repository;

import com.example.bbs.auth.domain.OAuth2Provider;
import com.example.bbs.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(OAuth2Provider provider, String providerId);
}
```

- [ ] **Step 4-6: Flyway V1 마이그레이션 작성**

```sql
-- backend/src/main/resources/db/migration/V1__create_users_table.sql
CREATE TABLE users (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider        VARCHAR(20)   NOT NULL COMMENT 'KAKAO | GOOGLE',
    provider_id     VARCHAR(100)  NOT NULL,
    email           VARCHAR(255)            COMMENT '표시용, 계정 식별 불사용',
    nickname        VARCHAR(50)   NOT NULL,
    profile_image   VARCHAR(500),
    role            VARCHAR(20)   NOT NULL DEFAULT 'USER' COMMENT 'USER | MANAGER | ADMIN',
    banned_at       DATETIME,
    ban_expires_at  DATETIME,
    deleted_at      DATETIME,
    created_at      DATETIME      NOT NULL,
    updated_at      DATETIME      NOT NULL,
    UNIQUE KEY uq_provider (provider, provider_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 4-7: 커밋**

```bash
git add backend/src/
git commit -m "feat(be): add user domain entity and flyway migration V1"
```

---

### Task 5: JwtUtil (TDD)

**Files:**
- Create: `backend/src/main/java/com/example/bbs/global/util/JwtUtil.java`
- Create: `backend/src/test/java/com/example/bbs/global/util/JwtUtilTest.java`

- [ ] **Step 5-1: [RED] JwtUtilTest.java 작성**

```java
package com.example.bbs.global.util;

import static org.assertj.core.api.Assertions.*;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        String secret = Base64.getEncoder().encodeToString(new byte[32]); // 256-bit test key
        jwtUtil = new JwtUtil(secret, 1_800_000L, 604_800_000L);
    }

    @Test
    void AccessToken_생성_및_검증에_성공한다() {
        String token = jwtUtil.generateAccessToken(1L, "USER");

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(1L);
        assertThat(jwtUtil.extractRole(token)).isEqualTo("USER");
        assertThat(jwtUtil.extractJti(token)).isNotBlank();
    }

    @Test
    void RefreshToken_생성_및_userId_추출에_성공한다() {
        String token = jwtUtil.generateRefreshToken(2L);

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractUserId(token)).isEqualTo(2L);
    }

    @Test
    void 만료된_토큰은_검증에_실패한다() throws InterruptedException {
        JwtUtil shortLived = new JwtUtil(
            Base64.getEncoder().encodeToString(new byte[32]), 1L, 1L);
        String token = shortLived.generateAccessToken(1L, "USER");
        Thread.sleep(10);

        assertThat(shortLived.validateToken(token)).isFalse();
    }

    @Test
    void getRemainingMillis가_양수를_반환한다() {
        String token = jwtUtil.generateAccessToken(1L, "USER");
        assertThat(jwtUtil.getRemainingMillis(token)).isGreaterThan(0);
    }
}
```

- [ ] **Step 5-2: 테스트 실행 — FAIL 확인**

```bash
cd backend && ./gradlew test --tests "com.example.bbs.global.util.JwtUtilTest"
```

Expected: FAILED (JwtUtil 클래스 없음)

- [ ] **Step 5-3: [GREEN] JwtUtil.java 구현**

```java
package com.example.bbs.global.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long accessTokenExpiry;
    private final long refreshTokenExpiry;

    public JwtUtil(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-expiry}") long accessTokenExpiry,
        @Value("${jwt.refresh-token-expiry}") long refreshTokenExpiry) {
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        this.accessTokenExpiry = accessTokenExpiry;
        this.refreshTokenExpiry = refreshTokenExpiry;
    }

    public String generateAccessToken(Long userId, String role) {
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .claim("role", role)
            .id(UUID.randomUUID().toString())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + accessTokenExpiry))
            .signWith(key)
            .compact();
    }

    public String generateRefreshToken(Long userId) {
        return Jwts.builder()
            .subject(String.valueOf(userId))
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshTokenExpiry))
            .signWith(key)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public Long extractUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    public String extractRole(String token) {
        return getClaims(token).get("role", String.class);
    }

    public String extractJti(String token) {
        return getClaims(token).getId();
    }

    public long getRemainingMillis(String token) {
        return getClaims(token).getExpiration().getTime() - System.currentTimeMillis();
    }

    private Claims getClaims(String token) {
        return Jwts.parser().verifyWith(key).build()
            .parseSignedClaims(token).getPayload();
    }
}
```

- [ ] **Step 5-4: 테스트 실행 — PASS 확인**

```bash
./gradlew test --tests "com.example.bbs.global.util.JwtUtilTest"
```

Expected: 4 tests PASSED

- [ ] **Step 5-5: 커밋**

```bash
git add backend/src/
git commit -m "feat(be): add JwtUtil with TDD"
```

---

### Task 6: AuthTokenService (TDD — Mockito)

**Files:**
- Create: `backend/src/main/java/com/example/bbs/auth/service/AuthTokenService.java`
- Create: `backend/src/test/java/com/example/bbs/auth/service/AuthTokenServiceTest.java`

- [ ] **Step 6-1: [RED] AuthTokenServiceTest.java 작성**

```java
package com.example.bbs.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.example.bbs.global.exception.BusinessException;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class AuthTokenServiceTest {

    @Mock StringRedisTemplate redisTemplate;
    @Mock ValueOperations<String, String> valueOps;
    @InjectMocks AuthTokenService authTokenService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
    }

    @Test
    void OTC_생성_후_교환에_성공한다() {
        String at = "test-access-token";
        when(valueOps.getAndDelete(anyString())).thenReturn(at);

        String code = authTokenService.createOtc(at);
        String result = authTokenService.exchangeOtc(code);

        assertThat(result).isEqualTo(at);
        verify(valueOps).set(eq("OTC:" + code), eq(at), eq(30L), eq(TimeUnit.SECONDS));
    }

    @Test
    void OTC_키가_없으면_BusinessException_발생() {
        when(valueOps.getAndDelete(anyString())).thenReturn(null);

        assertThatThrownBy(() -> authTokenService.exchangeOtc("invalid-code"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    void RT_저장이_호출된다() {
        authTokenService.saveRefreshToken(1L, "rt-value", 604800L);

        verify(valueOps).set(eq("RT:1"), eq("rt-value"), eq(604800L), eq(TimeUnit.SECONDS));
    }
}
```

- [ ] **Step 6-2: 테스트 실행 — FAIL 확인**

```bash
./gradlew test --tests "com.example.bbs.auth.service.AuthTokenServiceTest"
```

Expected: FAILED (AuthTokenService 없음)

- [ ] **Step 6-3: [GREEN] AuthTokenService.java 구현**

```java
package com.example.bbs.auth.service;

import com.example.bbs.global.exception.BusinessException;
import com.example.bbs.global.exception.ErrorCode;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthTokenService {

    private final StringRedisTemplate redisTemplate;

    private static final String RT_PREFIX = "RT:";
    private static final String OTC_PREFIX = "OTC:";

    public void saveRefreshToken(Long userId, String refreshToken, long ttlSeconds) {
        redisTemplate.opsForValue().set(RT_PREFIX + userId, refreshToken, ttlSeconds, TimeUnit.SECONDS);
    }

    public String createOtc(String accessToken) {
        String code = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(OTC_PREFIX + code, accessToken, 30L, TimeUnit.SECONDS);
        return code;
    }

    public String exchangeOtc(String code) {
        String at = redisTemplate.opsForValue().getAndDelete(OTC_PREFIX + code);
        if (at == null) throw new BusinessException(ErrorCode.OTC_INVALID);
        return at;
    }

    public void deleteRefreshToken(Long userId) {
        redisTemplate.delete(RT_PREFIX + userId);
    }
}
```

- [ ] **Step 6-4: 테스트 실행 — PASS 확인**

```bash
./gradlew test --tests "com.example.bbs.auth.service.AuthTokenServiceTest"
```

Expected: 3 tests PASSED

- [ ] **Step 6-5: 커밋**

```bash
git add backend/src/
git commit -m "feat(be): add AuthTokenService with TDD"
```

---

### Task 7: CustomOAuth2UserService + 관련 클래스 (TDD)

**Files:**
- Create: `...auth/dto/OAuth2UserInfo.java`
- Create: `...auth/security/CustomUserDetails.java`
- Create: `...auth/service/CustomOAuth2UserService.java`
- Create: `...auth/service/CustomOAuth2UserServiceTest.java`

- [ ] **Step 7-1: OAuth2UserInfo.java (record + factory)**

```java
package com.example.bbs.auth.dto;

import com.example.bbs.auth.domain.OAuth2Provider;
import java.util.Map;

public record OAuth2UserInfo(
    OAuth2Provider provider,
    String providerId,
    String email,
    String nickname,
    String profileImage
) {
    @SuppressWarnings("unchecked")
    public static OAuth2UserInfo from(String registrationId, Map<String, Object> attributes) {
        return switch (registrationId) {
            case "kakao" -> {
                Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
                Map<String, Object> profile = (Map<String, Object>) account.get("profile");
                yield new OAuth2UserInfo(
                    OAuth2Provider.KAKAO,
                    String.valueOf(attributes.get("id")),
                    (String) account.getOrDefault("email", null),
                    (String) profile.get("nickname"),
                    (String) profile.get("profile_image_url")
                );
            }
            case "google" -> new OAuth2UserInfo(
                OAuth2Provider.GOOGLE,
                (String) attributes.get("sub"),
                (String) attributes.get("email"),
                (String) attributes.get("name"),
                (String) attributes.get("picture")
            );
            default -> throw new IllegalArgumentException("Unknown provider: " + registrationId);
        };
    }
}
```

- [ ] **Step 7-2: CustomUserDetails.java**

```java
package com.example.bbs.auth.security;

import com.example.bbs.user.domain.User;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements OAuth2User {

    private final User user;
    private final Map<String, Object> attributes;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getName() {
        return String.valueOf(user.getId());
    }
}
```

- [ ] **Step 7-3: [RED] CustomOAuth2UserServiceTest.java 작성**

```java
package com.example.bbs.auth.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.example.bbs.auth.domain.OAuth2Provider;
import com.example.bbs.auth.security.CustomUserDetails;
import com.example.bbs.user.domain.User;
import com.example.bbs.user.domain.UserRole;
import com.example.bbs.user.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks CustomOAuth2UserService service;

    // Google 속성 헬퍼
    private Map<String, Object> googleAttributes() {
        return Map.of(
            "sub", "google-id-123",
            "name", "Test User",
            "email", "test@gmail.com",
            "picture", "http://example.com/pic.jpg"
        );
    }

    @Test
    void 신규_사용자는_자동으로_저장된다() {
        when(userRepository.findByProviderAndProviderId(OAuth2Provider.GOOGLE, "google-id-123"))
            .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        // loadUser를 직접 호출하면 super.loadUser()가 실제 HTTP를 하므로
        // 내부 로직만 검증하는 extractAndLoad 메서드로 분리해 테스트
        User result = service.loadOrCreate(OAuth2Provider.GOOGLE, "google-id-123",
            "test@gmail.com", "Test User", null);

        verify(userRepository).save(any(User.class));
        assertThat(result.getNickname()).startsWith("User-");
    }

    @Test
    void 기존_사용자는_저장하지_않는다() {
        User existing = User.create(OAuth2Provider.GOOGLE, "google-id-123",
            "test@gmail.com", "ExistingUser", null);
        when(userRepository.findByProviderAndProviderId(OAuth2Provider.GOOGLE, "google-id-123"))
            .thenReturn(Optional.of(existing));

        User result = service.loadOrCreate(OAuth2Provider.GOOGLE, "google-id-123",
            "test@gmail.com", "ExistingUser", null);

        verify(userRepository, never()).save(any());
        assertThat(result).isSameAs(existing);
    }

    @Test
    void 차단된_사용자는_예외가_발생한다() {
        User banned = User.create(OAuth2Provider.GOOGLE, "google-id-123", null, "User-abc", null);
        // banExpiresAt을 미래로 설정하기 위해 ban 메서드가 없으므로 리플렉션 또는 별도 팩토리 사용
        // User에 testBan() static 팩토리 추가 필요 (테스트 전용)
        // 여기서는 isBanned()가 true를 반환하는 stub User를 사용
        User bannedUser = spy(banned);
        when(bannedUser.isBanned()).thenReturn(true);
        when(userRepository.findByProviderAndProviderId(OAuth2Provider.GOOGLE, "google-id-123"))
            .thenReturn(Optional.of(bannedUser));

        assertThatThrownBy(() -> service.loadOrCreate(OAuth2Provider.GOOGLE, "google-id-123",
            null, "User-abc", null))
            .isInstanceOf(org.springframework.security.oauth2.core.OAuth2AuthenticationException.class);
    }
}
```

- [ ] **Step 7-4: 테스트 실행 — FAIL 확인**

```bash
./gradlew test --tests "com.example.bbs.auth.service.CustomOAuth2UserServiceTest"
```

Expected: FAILED (loadOrCreate 메서드 없음)

- [ ] **Step 7-5: [GREEN] CustomOAuth2UserService.java 구현**

```java
package com.example.bbs.auth.service;

import com.example.bbs.auth.domain.OAuth2Provider;
import com.example.bbs.auth.dto.OAuth2UserInfo;
import com.example.bbs.auth.security.CustomUserDetails;
import com.example.bbs.user.domain.User;
import com.example.bbs.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(request);
        String registrationId = request.getClientRegistration().getRegistrationId();
        OAuth2UserInfo info = OAuth2UserInfo.from(registrationId, oAuth2User.getAttributes());

        User user = loadOrCreate(info.provider(), info.providerId(),
            info.email(), info.nickname(), info.profileImage());

        return new CustomUserDetails(user, oAuth2User.getAttributes());
    }

    /** 테스트를 위해 package-private으로 분리 */
    User loadOrCreate(OAuth2Provider provider, String providerId,
                       String email, String nickname, String profileImage) {
        User user = userRepository.findByProviderAndProviderId(provider, providerId)
            .orElseGet(() -> userRepository.save(
                User.create(provider, providerId, email,
                    "User-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                    profileImage)
            ));

        if (user.isBanned()) {
            throw new OAuth2AuthenticationException("LOGIN_BANNED");
        }
        return user;
    }
}
```

- [ ] **Step 7-6: 테스트 실행 — PASS 확인**

```bash
./gradlew test --tests "com.example.bbs.auth.service.CustomOAuth2UserServiceTest"
```

Expected: 3 tests PASSED

- [ ] **Step 7-7: 커밋**

```bash
git add backend/src/
git commit -m "feat(be): add CustomOAuth2UserService with TDD"
```

---

### Task 8: OAuth2 Handlers

**Files:**
- Create: `...auth/handler/OAuth2LoginSuccessHandler.java`
- Create: `...auth/handler/OAuth2LoginFailureHandler.java`

- [ ] **Step 8-1: OAuth2LoginSuccessHandler.java**

```java
package com.example.bbs.auth.handler;

import com.example.bbs.auth.security.CustomUserDetails;
import com.example.bbs.auth.service.AuthTokenService;
import com.example.bbs.global.util.JwtUtil;
import com.example.bbs.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final AuthTokenService authTokenService;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Value("${cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${cookie.same-site:Lax}")
    private String cookieSameSite;

    private static final long RT_TTL_SECONDS = 604_800L;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                         Authentication authentication) throws IOException {
        CustomUserDetails details = (CustomUserDetails) authentication.getPrincipal();
        User user = details.getUser();

        String at = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
        String rt = jwtUtil.generateRefreshToken(user.getId());

        authTokenService.saveRefreshToken(user.getId(), rt, RT_TTL_SECONDS);

        ResponseCookie cookie = ResponseCookie.from("refresh_token", rt)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(RT_TTL_SECONDS)
            .sameSite(cookieSameSite)
            .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        String code = authTokenService.createOtc(at);
        getRedirectStrategy().sendRedirect(request, response,
            frontendUrl + "/auth/callback?code=" + code);
    }
}
```

- [ ] **Step 8-2: OAuth2LoginFailureHandler.java**

```java
package com.example.bbs.auth.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuth2LoginFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                          AuthenticationException exception) throws IOException {
        String errorParam = "LOGIN_BANNED".equals(exception.getMessage())
            ? "error=login_banned"
            : "error=auth_failed";
        getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?" + errorParam);
    }
}
```

- [ ] **Step 8-3: 커밋**

```bash
git add backend/src/
git commit -m "feat(be): add OAuth2 success/failure handlers"
```

---

### Task 9: Config 클래스들

**Files:**
- Create: `...global/config/RedisConfig.java`
- Create: `...global/config/CorsConfig.java`
- Create: `...global/config/SecurityConfig.java`

- [ ] **Step 9-1: RedisConfig.java**

```java
package com.example.bbs.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration
public class RedisConfig {
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory factory) {
        return new StringRedisTemplate(factory);
    }
}
```

- [ ] **Step 9-2: CorsConfig.java**

```java
package com.example.bbs.global.config;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

    @Value("${frontend.url}")
    private String frontendUrl;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
```

- [ ] **Step 9-3: SecurityConfig.java**

```java
package com.example.bbs.global.config;

import com.example.bbs.auth.handler.OAuth2LoginFailureHandler;
import com.example.bbs.auth.handler.OAuth2LoginSuccessHandler;
import com.example.bbs.auth.service.CustomOAuth2UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2LoginSuccessHandler successHandler;
    private final OAuth2LoginFailureHandler failureHandler;
    private final CorsConfig corsConfig;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(e -> e.userService(customOAuth2UserService))
                .successHandler(successHandler)
                .failureHandler(failureHandler)
            );
        return http.build();
    }
}
```

- [ ] **Step 9-4: 커밋**

```bash
git add backend/src/
git commit -m "feat(be): add security, cors, redis config"
```

---

### Task 10: AuthController + DTOs

**Files:**
- Create: `...auth/dto/TokenRequest.java`
- Create: `...auth/dto/TokenResponse.java`
- Create: `...auth/controller/AuthController.java`

- [ ] **Step 10-1: TokenRequest / TokenResponse**

```java
// TokenRequest.java
package com.example.bbs.auth.dto;
public record TokenRequest(String code) {}

// TokenResponse.java
package com.example.bbs.auth.dto;
public record TokenResponse(String accessToken) {}
```

- [ ] **Step 10-2: AuthController.java**

```java
package com.example.bbs.auth.controller;

import com.example.bbs.auth.dto.TokenRequest;
import com.example.bbs.auth.dto.TokenResponse;
import com.example.bbs.auth.service.AuthTokenService;
import com.example.bbs.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthTokenService authTokenService;

    @PostMapping("/token")
    public ResponseEntity<ApiResponse<TokenResponse>> exchangeToken(
        @RequestBody TokenRequest request) {
        String at = authTokenService.exchangeOtc(request.code());
        return ResponseEntity.ok(ApiResponse.ok(new TokenResponse(at), "토큰 발급 성공"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.ok(null, "AUTH-002 구현 예정"));
    }

    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue() {
        return ResponseEntity.ok(ApiResponse.ok(null, "AUTH-003 구현 예정"));
    }
}
```

- [ ] **Step 10-3: 커밋**

```bash
git add backend/src/
git commit -m "feat(be): add AuthController with OTC token exchange"
```

---

### Task 11: BE 빌드 검증 + Spotless + PR

- [ ] **Step 11-1: 전체 빌드 + 테스트**

```bash
cd backend
docker compose up -d mysql redis   # 컨테이너 필요시
./gradlew spotlessApply
./gradlew build
```

Expected: BUILD SUCCESSFUL, 모든 테스트 통과

- [ ] **Step 11-2: PR 생성**

```bash
git push -u origin feature/be/auth-social-login
gh pr create \
  --title "feat(auth): 소셜 로그인 BE 구현" \
  --body "$(cat <<'EOF'
## Summary
- Spring Security OAuth2로 카카오/구글 소셜 로그인 구현
- OTC(One-Time Code) 기반 AT 안전 전달 (GETDEL 원자적 처리)
- JWT(Access 30분 / Refresh 7일) + Redis RT 저장
- Docker Compose: MySQL 8 + Redis 7 (healthcheck)
- Flyway V1: users 테이블

## Test Plan
- [ ] `./gradlew test` 통과 확인
- [ ] `docker compose up -d && ./gradlew bootRun` 기동 확인
- [ ] GET http://localhost:8080/oauth2/authorization/kakao 리다이렉트 확인 (키 있을 때)

Closes #14
EOF
)"
```

---

## Phase 3: FE (feature/fe/auth-social-login)

### Task 12: Next.js 프로젝트 스캐폴딩

- [ ] **Step 12-1: BE PR 머지 후 develop에서 FE 브랜치 생성**

```bash
git checkout develop && git pull
git checkout -b feature/fe/auth-social-login
```

- [ ] **Step 12-2: Next.js 프로젝트 생성**

```bash
cd frontend
pnpm create next-app@latest . --typescript --tailwind --app --src-dir \
  --import-alias "@/*" --no-git --no-eslint
```

- [ ] **Step 12-3: 추가 패키지 설치**

```bash
pnpm add zustand axios @tanstack/react-query next-auth@beta
pnpm add -D vitest @vitejs/plugin-react @testing-library/react \
  @testing-library/jest-dom msw openapi-typescript jsdom
```

- [ ] **Step 12-4: shadcn/ui 초기화**

```bash
pnpm dlx shadcn@latest init --defaults
pnpm dlx shadcn@latest add button
```

- [ ] **Step 12-5: vitest.config.ts 작성**

```typescript
// frontend/vitest.config.ts
import { defineConfig } from 'vitest/config';
import react from '@vitejs/plugin-react';
import path from 'path';

export default defineConfig({
  plugins: [react()],
  test: {
    environment: 'jsdom',
    globals: true,
    setupFiles: ['./src/tests/setup.ts'],
  },
  resolve: {
    alias: { '@': path.resolve(__dirname, './src') },
  },
});
```

- [ ] **Step 12-6: src/tests/setup.ts 작성**

```typescript
import '@testing-library/jest-dom';
```

- [ ] **Step 12-7: .env.local 작성**

```bash
# frontend/.env.local
NEXT_PUBLIC_API_URL=http://localhost:8080
AUTH_SECRET=replace-with-random-32-char-string
```

- [ ] **Step 12-8: 커밋**

```bash
git add frontend/
git commit -m "chore(fe): next.js project scaffolding"
```

---

### Task 13: openapi-typescript 타입 생성

- [ ] **Step 13-1: types 생성 실행**

```bash
cd frontend
npx openapi-typescript ../openapi.yaml -o src/types/api.ts
```

Expected: `src/types/api.ts` 생성됨

- [ ] **Step 13-2: .gitignore에서 제외 확인 (생성 파일이므로 gitignore 불필요 — 커밋 포함)**

- [ ] **Step 13-3: package.json에 generate 스크립트 추가**

```json
"scripts": {
  "generate:types": "openapi-typescript ../openapi.yaml -o src/types/api.ts"
}
```

- [ ] **Step 13-4: 커밋**

```bash
git add frontend/src/types/api.ts frontend/package.json
git commit -m "chore(fe): generate api types from openapi.yaml"
```

---

### Task 14: AuthStore + Axios

**Files:**
- Create: `frontend/src/stores/authStore.ts`
- Create: `frontend/src/lib/axios.ts`

- [ ] **Step 14-1: authStore.ts 작성**

```typescript
// frontend/src/stores/authStore.ts
import { create } from 'zustand';
import { jwtDecode } from 'jwt-decode';

interface JwtPayload {
  sub: string;
  role: string;
}

interface AuthState {
  accessToken: string | null;
  userId: number | null;
  role: string | null;
  setAuth: (token: string) => void;
  clearAuth: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  accessToken: null,
  userId: null,
  role: null,
  setAuth: (token) => {
    const payload = jwtDecode<JwtPayload>(token);
    set({ accessToken: token, userId: Number(payload.sub), role: payload.role });
  },
  clearAuth: () => set({ accessToken: null, userId: null, role: null }),
}));
```

> `jwt-decode` 패키지 설치: `pnpm add jwt-decode`

- [ ] **Step 14-2: axios.ts 작성**

```typescript
// frontend/src/lib/axios.ts
import axios from 'axios';
import { useAuthStore } from '@/stores/authStore';

const api = axios.create({
  baseURL: process.env.NEXT_PUBLIC_API_URL,
  withCredentials: true,
});

api.interceptors.request.use((config) => {
  const { accessToken } = useAuthStore.getState();
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`;
  }
  config.headers['X-Requested-With'] = 'XMLHttpRequest';
  return config;
});

// 401 응답 인터셉터는 AUTH-003에서 구현
export default api;
```

- [ ] **Step 14-3: 커밋**

```bash
pnpm add jwt-decode
git add frontend/src/
git commit -m "feat(fe): add auth store and axios instance"
```

---

### Task 15: useExchangeToken hook

**Files:**
- Create: `frontend/src/hooks/useAuth.ts`

- [ ] **Step 15-1: useAuth.ts 작성**

```typescript
// frontend/src/hooks/useAuth.ts
import { useMutation } from '@tanstack/react-query';
import api from '@/lib/axios';
import { useAuthStore } from '@/stores/authStore';

interface TokenResponse {
  success: boolean;
  data: { accessToken: string };
  message: string;
}

export function useExchangeToken() {
  const setAuth = useAuthStore((s) => s.setAuth);

  return useMutation({
    mutationFn: (code: string) =>
      api.post<TokenResponse>('/api/auth/token', { code }).then((r) => r.data),
    onSuccess: (data) => {
      setAuth(data.data.accessToken);
    },
  });
}
```

- [ ] **Step 15-2: 커밋**

```bash
git add frontend/src/hooks/
git commit -m "feat(fe): add useExchangeToken hook"
```

---

### Task 16: SocialLoginButton + 로그인 페이지

**Files:**
- Create: `frontend/src/components/atoms/SocialLoginButton.tsx`
- Create: `frontend/src/app/login/page.tsx`

- [ ] **Step 16-1: SocialLoginButton.tsx**

```typescript
// frontend/src/components/atoms/SocialLoginButton.tsx
interface SocialLoginButtonProps {
  provider: 'kakao' | 'google';
  href: string;
}

const PROVIDER_STYLE = {
  kakao: 'bg-yellow-400 hover:bg-yellow-500 text-black',
  google: 'bg-white hover:bg-gray-100 text-gray-700 border border-gray-300',
};

const PROVIDER_LABEL = {
  kakao: '카카오로 로그인',
  google: 'Google로 로그인',
};

export default function SocialLoginButton({ provider, href }: SocialLoginButtonProps) {
  return (
    <a
      href={href}
      className={`flex items-center justify-center w-full py-3 px-4 rounded-lg font-medium transition-colors ${PROVIDER_STYLE[provider]}`}
    >
      {PROVIDER_LABEL[provider]}
    </a>
  );
}
```

- [ ] **Step 16-2: 로그인 페이지**

```typescript
// frontend/src/app/login/page.tsx
import SocialLoginButton from '@/components/atoms/SocialLoginButton';

const API_URL = process.env.NEXT_PUBLIC_API_URL!;

export default function LoginPage() {
  return (
    <main className="flex min-h-screen items-center justify-center bg-gray-50">
      <div className="w-full max-w-sm space-y-4 rounded-xl bg-white p-8 shadow">
        <h1 className="text-center text-2xl font-bold">로그인</h1>
        <SocialLoginButton
          provider="kakao"
          href={`${API_URL}/oauth2/authorization/kakao`}
        />
        <SocialLoginButton
          provider="google"
          href={`${API_URL}/oauth2/authorization/google`}
        />
      </div>
    </main>
  );
}
```

- [ ] **Step 16-3: 커밋**

```bash
git add frontend/src/
git commit -m "feat(fe): add login page with social login buttons"
```

---

### Task 17: CallbackPage (TDD)

**Files:**
- Create: `frontend/src/app/auth/callback/__tests__/page.test.tsx`
- Create: `frontend/src/app/auth/callback/page.tsx`
- Create: `frontend/src/mocks/handlers.ts`

- [ ] **Step 17-1: MSW 핸들러 작성**

```typescript
// frontend/src/mocks/handlers.ts
import { http, HttpResponse } from 'msw';

export const handlers = [
  http.post(`${process.env.NEXT_PUBLIC_API_URL}/api/auth/token`, async ({ request }) => {
    const body = (await request.json()) as { code: string };
    if (body.code === 'valid-code') {
      return HttpResponse.json({
        success: true,
        data: { accessToken: 'test.jwt.token' },
        message: '토큰 발급 성공',
      });
    }
    return new HttpResponse(null, { status: 400 });
  }),
];
```

- [ ] **Step 17-2: [RED] CallbackPage 테스트 작성**

```typescript
// frontend/src/app/auth/callback/__tests__/page.test.tsx
import { render, waitFor } from '@testing-library/react';
import { describe, it, expect, vi, beforeAll, afterAll, afterEach } from 'vitest';
import { setupServer } from 'msw/node';
import { handlers } from '@/mocks/handlers';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import CallbackPage from '../page';

const server = setupServer(...handlers);
const mockReplace = vi.fn();

vi.mock('next/navigation', () => ({
  useRouter: () => ({ replace: mockReplace }),
  useSearchParams: vi.fn(),
}));

vi.mock('@/stores/authStore', () => ({
  useAuthStore: (selector: (s: { setAuth: ReturnType<typeof vi.fn> }) => unknown) =>
    selector({ setAuth: vi.fn() }),
}));

import { useSearchParams } from 'next/navigation';

beforeAll(() => server.listen());
afterEach(() => { server.resetHandlers(); mockReplace.mockClear(); });
afterAll(() => server.close());

function renderWithQuery(ui: React.ReactElement) {
  const qc = new QueryClient();
  return render(<QueryClientProvider client={qc}>{ui}</QueryClientProvider>);
}

describe('CallbackPage', () => {
  it('code가 없으면 /login으로 이동한다', async () => {
    vi.mocked(useSearchParams).mockReturnValue(new URLSearchParams('') as unknown as ReturnType<typeof useSearchParams>);
    renderWithQuery(<CallbackPage />);
    await waitFor(() => expect(mockReplace).toHaveBeenCalledWith('/login'));
  });

  it('유효한 code로 교환 성공 시 /로 이동한다', async () => {
    vi.mocked(useSearchParams).mockReturnValue(new URLSearchParams('code=valid-code') as unknown as ReturnType<typeof useSearchParams>);
    renderWithQuery(<CallbackPage />);
    await waitFor(() => expect(mockReplace).toHaveBeenCalledWith('/'));
  });

  it('교환 실패 시 /login?error=token_exchange_failed로 이동한다', async () => {
    vi.mocked(useSearchParams).mockReturnValue(new URLSearchParams('code=invalid-code') as unknown as ReturnType<typeof useSearchParams>);
    renderWithQuery(<CallbackPage />);
    await waitFor(() =>
      expect(mockReplace).toHaveBeenCalledWith('/login?error=token_exchange_failed')
    );
  });
});
```

- [ ] **Step 17-3: 테스트 실행 — FAIL 확인**

```bash
cd frontend && pnpm vitest run src/app/auth/callback/__tests__/page.test.tsx
```

Expected: FAILED (CallbackPage 없음)

- [ ] **Step 17-4: [GREEN] CallbackPage 구현**

```typescript
// frontend/src/app/auth/callback/page.tsx
'use client';

import { useEffect } from 'react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useExchangeToken } from '@/hooks/useAuth';

export default function CallbackPage() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const code = searchParams.get('code');
  const { mutateAsync: exchangeToken, isPending } = useExchangeToken();

  useEffect(() => {
    if (!code) {
      router.replace('/login');
      return;
    }
    exchangeToken(code)
      .then(() => router.replace('/'))
      .catch(() => router.replace('/login?error=token_exchange_failed'));
  }, [code, exchangeToken, router]);

  if (isPending) {
    return (
      <div className="flex min-h-screen items-center justify-center">
        <p className="text-gray-500">로그인 처리 중...</p>
      </div>
    );
  }

  return null;
}
```

- [ ] **Step 17-5: 테스트 실행 — PASS 확인**

```bash
pnpm vitest run src/app/auth/callback/__tests__/page.test.tsx
```

Expected: 3 tests PASSED

- [ ] **Step 17-6: 커밋**

```bash
git add frontend/src/
git commit -m "feat(fe): add auth callback page with TDD"
```

---

### Task 18: next-auth v5 stub + middleware

**Files:**
- Create: `frontend/src/auth.ts`
- Create: `frontend/src/middleware.ts`

- [ ] **Step 18-1: auth.ts stub**

```typescript
// frontend/src/auth.ts
import NextAuth from 'next-auth';

export const { auth, handlers, signIn, signOut } = NextAuth({
  providers: [],
  secret: process.env.AUTH_SECRET,
  // AUTH-002/003에서 Zustand AT ↔ next-auth 세션 연동 전략 결정 후 완성
});
```

- [ ] **Step 18-2: middleware.ts (pass-through stub)**

```typescript
// frontend/src/middleware.ts
// AUTH-001: 모든 경로 통과. AUTH-002/003에서 보호 라우트 추가.
export { auth as middleware } from '@/auth';
export const config = { matcher: [] };
```

- [ ] **Step 18-3: app/api/auth/[...nextauth]/route.ts 생성**

```typescript
// frontend/src/app/api/auth/[...nextauth]/route.ts
import { handlers } from '@/auth';
export const { GET, POST } = handlers;
```

- [ ] **Step 18-4: 커밋**

```bash
git add frontend/src/
git commit -m "chore(fe): add next-auth v5 stub and passthrough middleware"
```

---

### Task 19: FE 빌드 검증 + PR

- [ ] **Step 19-1: 전체 테스트**

```bash
cd frontend && pnpm vitest run
```

Expected: 3 tests PASSED

- [ ] **Step 19-2: 빌드 검증**

```bash
pnpm build
```

Expected: 빌드 성공

- [ ] **Step 19-3: PR 생성**

```bash
git push -u origin feature/fe/auth-social-login
gh pr create \
  --title "feat(auth): 소셜 로그인 FE 구현" \
  --body "$(cat <<'EOF'
## Summary
- 로그인 페이지: 카카오/구글 소셜 버튼 (BE OAuth 엔드포인트 링크)
- Callback 페이지: OTC 교환 → Zustand AuthStore 저장 → 홈 이동
- Zustand AuthStore: accessToken/userId/role (메모리)
- Axios 인스턴스: withCredentials, X-Requested-With 헤더
- next-auth v5 + middleware stub (AUTH-002/003에서 활성화)
- Vitest + MSW 컴포넌트 테스트 3건

## Test Plan
- [ ] `pnpm vitest run` 통과 확인
- [ ] `pnpm build` 성공 확인
- [ ] BE 연동 후 http://localhost:3000/login 에서 실제 OAuth 플로우 확인

Refs #14
EOF
)"
```

---

## 체크리스트 요약

| Phase | Task | 완료 |
|---|---|---|
| Pre-setup | develop 브랜치 + openapi.yaml | - [ ] |
| Infra | Docker Compose + .env.example | - [ ] |
| BE | Spring Boot 스캐폴딩 | - [ ] |
| BE | 글로벌 레이어 | - [ ] |
| BE | 도메인 레이어 + Flyway V1 | - [ ] |
| BE | JwtUtil TDD | - [ ] |
| BE | AuthTokenService TDD | - [ ] |
| BE | CustomOAuth2UserService TDD | - [ ] |
| BE | OAuth2 Handlers | - [ ] |
| BE | Config 클래스들 | - [ ] |
| BE | AuthController | - [ ] |
| BE | BE PR | - [ ] |
| FE | Next.js 스캐폴딩 | - [ ] |
| FE | openapi-typescript 타입 생성 | - [ ] |
| FE | AuthStore + Axios | - [ ] |
| FE | useExchangeToken hook | - [ ] |
| FE | SocialLoginButton + 로그인 페이지 | - [ ] |
| FE | CallbackPage TDD | - [ ] |
| FE | next-auth stub | - [ ] |
| FE | FE PR | - [ ] |
