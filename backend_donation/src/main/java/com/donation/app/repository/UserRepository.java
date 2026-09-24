package com.donation.app.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.donation.app.model.User;

public interface UserRepository extends JpaRepository<User, Long>{
    Optional<User> findByEmail(String email);
    Boolean existsByEmail(String email);
    Optional<User> findByEmailAndResetTokenAndTokenExpirationAfter(String email, String resetToken, LocalDateTime  tokenExpiration);
}
