package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.social.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<UserBlock> findByBlockerIdOrderByCreatedAtDesc(Long blockerId);

    @Modifying
    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    @Query("""
            SELECT CASE WHEN COUNT(b) > 0 THEN true ELSE false END
            FROM UserBlock b
            WHERE (b.blocker.id = :id1 AND b.blocked.id = :id2)
               OR (b.blocker.id = :id2 AND b.blocked.id = :id1)
            """)
    boolean isBlockedBetween(@Param("id1") Long id1, @Param("id2") Long id2);

    @Query("""
            SELECT b.blocked.id FROM UserBlock b WHERE b.blocker.id = :userId
            UNION
            SELECT b.blocker.id FROM UserBlock b WHERE b.blocked.id = :userId
            """)
    List<Long> findAllBlockedBetweenIds(@Param("userId") Long userId);
}

