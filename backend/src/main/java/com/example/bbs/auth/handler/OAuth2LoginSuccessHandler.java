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
  public void onAuthenticationSuccess(
      HttpServletRequest request, HttpServletResponse response, Authentication authentication)
      throws IOException {
    CustomUserDetails details = (CustomUserDetails) authentication.getPrincipal();
    User user = details.getUser();

    String at = jwtUtil.generateAccessToken(user.getId(), user.getRole().name());
    String rt = jwtUtil.generateRefreshToken(user.getId());

    authTokenService.saveRefreshToken(user.getId(), rt, RT_TTL_SECONDS);

    ResponseCookie cookie =
        ResponseCookie.from("refresh_token", rt)
            .httpOnly(true)
            .secure(cookieSecure)
            .path("/")
            .maxAge(RT_TTL_SECONDS)
            .sameSite(cookieSameSite)
            .build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    String code = authTokenService.createOtc(at);
    getRedirectStrategy()
        .sendRedirect(request, response, frontendUrl + "/auth/callback?code=" + code);
  }
}
