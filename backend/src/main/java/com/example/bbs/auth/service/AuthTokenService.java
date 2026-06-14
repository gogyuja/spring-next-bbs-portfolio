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
