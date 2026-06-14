package com.example.bbs.global.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

  private JwtUtil jwtUtil;

  @BeforeEach
  void setUp() {
    String secret = Base64.getEncoder().encodeToString(new byte[32]);
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
    JwtUtil shortLived = new JwtUtil(Base64.getEncoder().encodeToString(new byte[32]), 1L, 1L);
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
