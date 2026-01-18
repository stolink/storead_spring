package com.stolink.backend.domain.user.repository;

import com.stolink.backend.domain.user.entity.UserGamification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserGamificationRepository extends JpaRepository<UserGamification, UUID> {
    Optional<UserGamification> findByUserId(UUID userId);
}
