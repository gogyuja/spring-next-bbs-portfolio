package com.example.bbs.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.bbs.auth.domain.OAuth2Provider;
import com.example.bbs.user.domain.User;
import com.example.bbs.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

@ExtendWith(MockitoExtension.class)
class CustomOAuth2UserServiceTest {

  @Mock UserRepository userRepository;
  @InjectMocks CustomOAuth2UserService service;

  @Test
  void 신규_사용자는_자동으로_저장된다() {
    when(userRepository.findByProviderAndProviderId(OAuth2Provider.GOOGLE, "google-id-123"))
        .thenReturn(Optional.empty());
    when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

    User result =
        service.loadOrCreate(
            OAuth2Provider.GOOGLE, "google-id-123", "test@gmail.com", "Test User", null);

    verify(userRepository).save(any(User.class));
    assertThat(result.getNickname()).startsWith("User-");
  }

  @Test
  void 기존_사용자는_저장하지_않는다() {
    User existing =
        User.create(OAuth2Provider.GOOGLE, "google-id-123", "test@gmail.com", "ExistingUser", null);
    when(userRepository.findByProviderAndProviderId(OAuth2Provider.GOOGLE, "google-id-123"))
        .thenReturn(Optional.of(existing));

    User result =
        service.loadOrCreate(
            OAuth2Provider.GOOGLE, "google-id-123", "test@gmail.com", "ExistingUser", null);

    verify(userRepository, never()).save(any());
    assertThat(result).isSameAs(existing);
  }

  @Test
  void 차단된_사용자는_예외가_발생한다() {
    User bannedUser =
        Mockito.spy(User.create(OAuth2Provider.GOOGLE, "google-id-123", null, "User-abc", null));
    Mockito.when(bannedUser.isBanned()).thenReturn(true);
    when(userRepository.findByProviderAndProviderId(OAuth2Provider.GOOGLE, "google-id-123"))
        .thenReturn(Optional.of(bannedUser));

    assertThatThrownBy(
            () ->
                service.loadOrCreate(
                    OAuth2Provider.GOOGLE, "google-id-123", null, "User-abc", null))
        .isInstanceOf(OAuth2AuthenticationException.class);
  }
}
