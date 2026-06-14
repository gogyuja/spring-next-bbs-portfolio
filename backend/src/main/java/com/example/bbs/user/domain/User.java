package com.example.bbs.user.domain;

import com.example.bbs.auth.domain.OAuth2Provider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private OAuth2Provider provider;

  @Column(nullable = false, length = 100)
  private String providerId;

  @Column(length = 255)
  private String email;

  @Column(nullable = false, length = 50)
  private String nickname;

  @Column(length = 500)
  private String profileImage;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private UserRole role;

  private LocalDateTime bannedAt;
  private LocalDateTime banExpiresAt;
  private LocalDateTime deletedAt;

  public static User create(
      OAuth2Provider provider,
      String providerId,
      String email,
      String nickname,
      String profileImage) {
    User user = new User();
    user.provider = provider;
    user.providerId = providerId;
    user.email = email;
    user.nickname = nickname;
    user.profileImage = profileImage;
    user.role = UserRole.USER;
    return user;
  }

  public boolean isBanned() {
    return banExpiresAt != null && banExpiresAt.isAfter(LocalDateTime.now());
  }
}
