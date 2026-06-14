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
