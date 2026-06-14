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
  public void onAuthenticationFailure(
      HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
      throws IOException {
    String errorParam =
        "LOGIN_BANNED".equals(exception.getMessage()) ? "error=login_banned" : "error=auth_failed";
    getRedirectStrategy().sendRedirect(request, response, frontendUrl + "/login?" + errorParam);
  }
}
