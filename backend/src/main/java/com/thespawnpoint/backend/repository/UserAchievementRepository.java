package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.achievement.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    boolean existsByUserIdAndAchievementCode(Long userId, String achievementCode);
    Optional<UserAchievement> findByUserIdAndAchievementCode(Long userId, String achievementCode);
    List<UserAchievement> findByUserIdOrderByUnlockedAtDesc(Long userId);
    long countByUserId(Long userId);
}
