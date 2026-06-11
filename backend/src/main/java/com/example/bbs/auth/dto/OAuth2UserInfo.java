package com.example.bbs.auth.dto;

import com.example.bbs.auth.domain.OAuth2Provider;
import java.util.Map;

public record OAuth2UserInfo(
    OAuth2Provider provider,
    String providerId,
    String email,
    String nickname,
    String profileImage) {

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
            (String) profile.get("profile_image_url"));
      }
      case "google" ->
          new OAuth2UserInfo(
              OAuth2Provider.GOOGLE,
              (String) attributes.get("sub"),
              (String) attributes.get("email"),
              (String) attributes.get("name"),
              (String) attributes.get("picture"));
      default -> throw new IllegalArgumentException("Unknown provider: " + registrationId);
    };
  }
}
