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
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfig.corsConfigurationSource()))
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
        .oauth2Login(
            oauth2 ->
                oauth2
                    .userInfoEndpoint(e -> e.userService(customOAuth2UserService))
                    .successHandler(successHandler)
                    .failureHandler(failureHandler));
    return http.build();
  }
}
