package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.achievement.UserAchievement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserAchievementRepository extends JpaRepository<UserAchievement, Long> {
    boolean existsByUserIdAndAchievementCode(Long userId, String achievementCode);
    Optional<UserAchievement> findByUserIdAndAchievementCode(Long userId, String achievementCode);
    List<UserAchievement> findByUserIdOrderByUnlockedAtDesc(Long userId);
    List<UserAchievement> findByUserIdAndAchievementCodeIn(Long userId, List<String> achievementCodes);
    long countByUserId(Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update UserAchievement ua set ua.featuredPosition = null where ua.user.id = :userId")
    void clearFeaturedPositions(@Param("userId") Long userId);
}
