package com.example.bbs.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
