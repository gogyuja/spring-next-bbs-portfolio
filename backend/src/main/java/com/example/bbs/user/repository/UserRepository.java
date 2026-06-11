package com.example.bbs.user.repository;

import com.example.bbs.auth.domain.OAuth2Provider;
import com.example.bbs.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
  Optional<User> findByProviderAndProviderId(OAuth2Provider provider, String providerId);
}
