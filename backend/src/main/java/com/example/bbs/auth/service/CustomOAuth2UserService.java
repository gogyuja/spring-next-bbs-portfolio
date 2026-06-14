package com.example.bbs.auth.service;

import com.example.bbs.auth.domain.OAuth2Provider;
import com.example.bbs.auth.dto.OAuth2UserInfo;
import com.example.bbs.auth.security.CustomUserDetails;
import com.example.bbs.user.domain.User;
import com.example.bbs.user.repository.UserRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

  private final UserRepository userRepository;

  @Override
  @Transactional
  public OAuth2User loadUser(OAuth2UserRequest request) throws OAuth2AuthenticationException {
    OAuth2User oAuth2User = super.loadUser(request);
    String registrationId = request.getClientRegistration().getRegistrationId();
    OAuth2UserInfo info = OAuth2UserInfo.from(registrationId, oAuth2User.getAttributes());

    User user =
        loadOrCreate(
            info.provider(), info.providerId(), info.email(), info.nickname(), info.profileImage());

    return new CustomUserDetails(user, oAuth2User.getAttributes());
  }

  User loadOrCreate(
      OAuth2Provider provider,
      String providerId,
      String email,
      String nickname,
      String profileImage) {
    User user =
        userRepository
            .findByProviderAndProviderId(provider, providerId)
            .orElseGet(
                () ->
                    userRepository.save(
                        User.create(
                            provider,
                            providerId,
                            email,
                            "User-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8),
                            profileImage)));

    if (user.isBanned()) {
      throw new OAuth2AuthenticationException("LOGIN_BANNED");
    }
    return user;
  }
}
