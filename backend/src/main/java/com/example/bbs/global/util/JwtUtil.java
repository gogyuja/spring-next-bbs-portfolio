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
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }
}
