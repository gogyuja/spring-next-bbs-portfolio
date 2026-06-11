package com.example.bbs.auth.controller;

import com.example.bbs.auth.dto.TokenRequest;
import com.example.bbs.auth.dto.TokenResponse;
import com.example.bbs.auth.service.AuthTokenService;
import com.example.bbs.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
