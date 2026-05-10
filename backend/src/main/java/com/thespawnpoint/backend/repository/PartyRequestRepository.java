package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.party.PartyRequest;
import com.thespawnpoint.backend.entity.party.PartyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PartyRequestRepository extends JpaRepository<PartyRequest, Long> {

    List<PartyRequest> findByCreatorId(Long creatorId);

    long countByCreatorId(Long creatorId);

    long countByStatus(PartyStatus status);

    Page<PartyRequest> findByStatusOrderByCreatedAtDesc(PartyStatus status, Pageable pageable);

    boolean existsByCreatorIdAndStatusIn(Long creatorId, List<PartyStatus> statuses);

    Optional<PartyRequest> findByChatId(Long chatId);

    @Query(value = """
            SELECT pr.* FROM party_requests pr
            WHERE pr.status = 'OPEN'
              AND (:gameId     IS NULL OR pr.game_id     = :gameId)
              AND (:skillLevel IS NULL OR pr.skill_level = :skillLevel)
              AND (:playStyle  IS NULL OR pr.play_style  = :playStyle)
              AND (:platform   IS NULL OR :platform = ANY(pr.platform))
            ORDER BY pr.created_at DESC
            """, nativeQuery = true)
    List<PartyRequest> findOpenWithFilters(
            @Param("gameId")     Long gameId,
            @Param("skillLevel") String skillLevel,
            @Param("playStyle")  String playStyle,
            @Param("platform")   String platform
    );

    @Query(value = """
            SELECT pr.* FROM party_requests pr
            JOIN games g ON g.id = pr.game_id
            WHERE pr.status = 'OPEN'
              AND (:gameId     IS NULL OR pr.game_id     = :gameId)
              AND (:skillLevel IS NULL OR pr.skill_level = :skillLevel)
              AND (:playStyle  IS NULL OR pr.play_style  = :playStyle)
              AND (:platform   IS NULL OR :platform = ANY(pr.platform))
              AND (:language   IS NULL OR :language = ANY(pr.languages))
              AND (:region     IS NULL OR pr.region = :region)
              AND (:q          IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%'))
                                       OR LOWER(COALESCE(pr.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                                       OR LOWER(COALESCE(pr.title, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            ORDER BY pr.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM party_requests pr
            JOIN games g ON g.id = pr.game_id
            WHERE pr.status = 'OPEN'
              AND (:gameId     IS NULL OR pr.game_id     = :gameId)
              AND (:skillLevel IS NULL OR pr.skill_level = :skillLevel)
              AND (:playStyle  IS NULL OR pr.play_style  = :playStyle)
              AND (:platform   IS NULL OR :platform = ANY(pr.platform))
              AND (:language   IS NULL OR :language = ANY(pr.languages))
              AND (:region     IS NULL OR pr.region = :region)
              AND (:q          IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%'))
                                       OR LOWER(COALESCE(pr.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                                       OR LOWER(COALESCE(pr.title, '')) LIKE LOWER(CONCAT('%', :q, '%')))
            """,
            nativeQuery = true)
    Page<PartyRequest> findOpenWithFiltersPaged(
            @Param("gameId")     Long gameId,
            @Param("skillLevel") String skillLevel,
            @Param("playStyle")  String playStyle,
            @Param("platform")   String platform,
            @Param("language")   String language,
            @Param("region")     String region,
            @Param("q")          String q,
            Pageable pageable
    );

    @Query(value = """
            WITH viewer_context AS (
                SELECT
                    p.platforms,
                    p.languages,
                    p.region,
                    p.skill_level,
                    p.play_style,
                    (
                        SELECT CAST(COUNT(*) AS INTEGER)
                        FROM user_games ug
                        WHERE ug.user_id = :currentUserId
                    ) AS favorite_count,
                    (
                        CASE WHEN COALESCE(array_length(p.platforms, 1), 0) > 0 THEN 1 ELSE 0 END
                      + CASE WHEN COALESCE(array_length(p.languages, 1), 0) > 0 THEN 1 ELSE 0 END
                      + CASE WHEN p.region IS NOT NULL THEN 1 ELSE 0 END
                      + CASE WHEN p.skill_level IS NOT NULL THEN 1 ELSE 0 END
                      + CASE WHEN p.play_style IS NOT NULL THEN 1 ELSE 0 END
                    ) AS profile_signal_count
                FROM profiles p
                WHERE p.user_id = :currentUserId
            ),
            filtered_parties AS (
                SELECT
                    pr.id,
                    pr.creator_id,
                    pr.game_id,
                    pr.max_members,
                    pr.platform,
                    pr.languages,
                    pr.region,
                    pr.skill_level,
                    pr.play_style,
                    pr.created_at
                FROM party_requests pr
                JOIN games g ON g.id = pr.game_id
                WHERE pr.status = 'OPEN'
                  AND (:gameId     IS NULL OR pr.game_id = :gameId)
                  AND (:skillLevel IS NULL OR pr.skill_level = :skillLevel)
                  AND (:playStyle  IS NULL OR pr.play_style = :playStyle)
                  AND (:platform   IS NULL OR COALESCE(pr.platform, CAST(ARRAY[] AS varchar[])) @> ARRAY[CAST(:platform AS varchar)])
                  AND (:language   IS NULL OR COALESCE(pr.languages, CAST(ARRAY[] AS varchar[])) @> ARRAY[CAST(:language AS varchar)])
                  AND (:region     IS NULL OR pr.region = :region)
                  AND (:q          IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%'))
                                           OR LOWER(COALESCE(pr.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                                           OR LOWER(COALESCE(pr.title, '')) LIKE LOWER(CONCAT('%', :q, '%')))
                  AND (:currentUserId IS NULL OR NOT EXISTS (
                      SELECT 1
                      FROM user_blocks ub
                      WHERE (ub.blocker_id = :currentUserId AND ub.blocked_id = pr.creator_id)
                         OR (ub.blocker_id = pr.creator_id AND ub.blocked_id = :currentUserId)
                  ))
            ),
            member_counts AS (
                SELECT pm.party_request_id, CAST(COUNT(*) AS INTEGER) AS current_members
                FROM party_members pm
                JOIN filtered_parties fp ON fp.id = pm.party_request_id
                GROUP BY pm.party_request_id
            ),
            filtered_creators AS (
                SELECT DISTINCT creator_id
                FROM filtered_parties
            ),
            rating_stats AS (
                SELECT
                    prt.rated_user_id,
                    CAST(COUNT(*) AS INTEGER) AS rating_count,
                    CAST(COALESCE(SUM(prt.score), 0) AS INTEGER) AS rating_sum
                FROM player_ratings prt
                JOIN filtered_creators fc ON fc.creator_id = prt.rated_user_id
                GROUP BY prt.rated_user_id
            ),
            global_rating AS (
                SELECT COALESCE(AVG(score), 3.5) AS average_score
                FROM player_ratings
            ),
            creator_stats AS (
                SELECT
                    pr.creator_id,
                    CAST(COUNT(*) AS INTEGER) AS created_count,
                    CAST(COUNT(*) FILTER (
                        WHERE pr.status = 'COMPLETED'
                          AND pr.started_at IS NOT NULL
                          AND pr.completed_at IS NOT NULL
                          AND pr.auto_completed = false
                    ) AS INTEGER) AS completed_count
                FROM party_requests pr
                JOIN filtered_creators fc ON fc.creator_id = pr.creator_id
                GROUP BY pr.creator_id
            ),
            candidate_scores AS (
                SELECT
                    fp.id AS party_id,
                    (
                        CASE WHEN fav.game_id IS NOT NULL THEN 28 ELSE 0 END
                      + CASE
                            WHEN COALESCE(fp.platform, CAST(ARRAY[] AS varchar[]))
                                 && COALESCE(vc.platforms, CAST(ARRAY[] AS varchar[]))
                            THEN 12 ELSE 0
                        END
                      + CASE
                            WHEN COALESCE(fp.languages, CAST(ARRAY[] AS varchar[]))
                                 && COALESCE(vc.languages, CAST(ARRAY[] AS varchar[]))
                            THEN 12 ELSE 0
                        END
                      + CASE WHEN vc.region IS NOT NULL AND fp.region = vc.region THEN 10 ELSE 0 END
                      + CASE
                            WHEN vc.skill_level IS NULL OR fp.skill_level IS NULL THEN 0
                            WHEN fp.skill_level = vc.skill_level THEN 8
                            WHEN ABS(
                                (CASE fp.skill_level
                                    WHEN 'BEGINNER' THEN 1
                                    WHEN 'INTERMEDIATE' THEN 2
                                    WHEN 'ADVANCED' THEN 3
                                    WHEN 'EXPERT' THEN 4
                                    ELSE 0
                                END)
                              - (CASE vc.skill_level
                                    WHEN 'BEGINNER' THEN 1
                                    WHEN 'INTERMEDIATE' THEN 2
                                    WHEN 'ADVANCED' THEN 3
                                    WHEN 'EXPERT' THEN 4
                                    ELSE 0
                                END)
                            ) = 1 THEN 4
                            ELSE 0
                        END
                      + CASE WHEN vc.play_style IS NOT NULL AND fp.play_style = vc.play_style THEN 8 ELSE 0 END
                      + CASE
                            WHEN (:currentUserId IS NULL
                                  OR (COALESCE(vc.favorite_count, 0) = 0 AND COALESCE(vc.profile_signal_count, 0) < 2))
                            THEN
                                CASE WHEN fp.skill_level IS NULL OR fp.skill_level = 'BEGINNER' THEN 12 ELSE 0 END
                              + CASE WHEN fp.play_style = 'CASUAL' THEN 10 ELSE 0 END
                            ELSE 0
                        END
                      + CASE
                            WHEN fp.created_at >= NOW() - INTERVAL '15 minutes' THEN 10
                            WHEN fp.created_at >= NOW() - INTERVAL '1 hour' THEN 7
                            WHEN fp.created_at >= NOW() - INTERVAL '3 hours' THEN 4
                            WHEN fp.created_at >= NOW() - INTERVAL '8 hours' THEN 2
                            ELSE 0
                        END
                      + CASE
                            WHEN COALESCE(mc.current_members, 0) <= 1
                                 AND fp.created_at >= NOW() - INTERVAL '1 hour' THEN 5
                            WHEN COALESCE(mc.current_members, 0) <= 1 THEN 1
                            WHEN CAST(COALESCE(mc.current_members, 0) AS numeric) / NULLIF(fp.max_members, 0) < 0.25 THEN 4
                            WHEN CAST(COALESCE(mc.current_members, 0) AS numeric) / NULLIF(fp.max_members, 0) < 0.60 THEN 7
                            WHEN CAST(COALESCE(mc.current_members, 0) AS numeric) / NULLIF(fp.max_members, 0) < 0.85 THEN 6
                            ELSE 3
                        END
                      + CASE
                            WHEN COALESCE(rs.rating_count, 0) >= 20 THEN
                                CASE
                                    WHEN ((15 * gr.average_score + COALESCE(rs.rating_sum, 0)) / (15 + COALESCE(rs.rating_count, 0))) >= 4.8 THEN 6
                                    WHEN ((15 * gr.average_score + COALESCE(rs.rating_sum, 0)) / (15 + COALESCE(rs.rating_count, 0))) >= 4.5 THEN 5
                                    WHEN ((15 * gr.average_score + COALESCE(rs.rating_sum, 0)) / (15 + COALESCE(rs.rating_count, 0))) >= 4.0 THEN 3
                                    WHEN ((15 * gr.average_score + COALESCE(rs.rating_sum, 0)) / (15 + COALESCE(rs.rating_count, 0))) >= 3.5 THEN 1
                                    ELSE 0
                                END
                            WHEN COALESCE(cs.completed_count, 0) < 3 AND COALESCE(cs.created_count, 0) <= 3 THEN 3
                            ELSE 0
                        END
                      + CASE
                            WHEN COALESCE(cs.completed_count, 0) < 3 AND COALESCE(cs.created_count, 0) <= 3 THEN
                                CASE
                                    WHEN fp.created_at >= NOW() - INTERVAL '15 minutes' THEN 8
                                    WHEN fp.created_at >= NOW() - INTERVAL '1 hour' THEN 6
                                    WHEN fp.created_at >= NOW() - INTERVAL '3 hours' THEN 3
                                    ELSE 0
                                END
                            ELSE 0
                        END
                    ) AS recommendation_score
                FROM filtered_parties fp
                LEFT JOIN viewer_context vc ON true
                LEFT JOIN user_games fav ON fav.user_id = :currentUserId AND fav.game_id = fp.game_id
                LEFT JOIN member_counts mc ON mc.party_request_id = fp.id
                LEFT JOIN rating_stats rs ON rs.rated_user_id = fp.creator_id
                LEFT JOIN creator_stats cs ON cs.creator_id = fp.creator_id
                CROSS JOIN global_rating gr
            )
            SELECT pr.*
            FROM candidate_scores cs
            JOIN party_requests pr ON pr.id = cs.party_id
            ORDER BY cs.recommendation_score DESC, pr.created_at DESC, pr.id DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM party_requests pr
            JOIN games g ON g.id = pr.game_id
            WHERE pr.status = 'OPEN'
              AND (:gameId     IS NULL OR pr.game_id = :gameId)
              AND (:skillLevel IS NULL OR pr.skill_level = :skillLevel)
              AND (:playStyle  IS NULL OR pr.play_style = :playStyle)
              AND (:platform   IS NULL OR COALESCE(pr.platform, CAST(ARRAY[] AS varchar[])) @> ARRAY[CAST(:platform AS varchar)])
              AND (:language   IS NULL OR COALESCE(pr.languages, CAST(ARRAY[] AS varchar[])) @> ARRAY[CAST(:language AS varchar)])
              AND (:region     IS NULL OR pr.region = :region)
              AND (:q          IS NULL OR LOWER(g.name) LIKE LOWER(CONCAT('%', :q, '%'))
                                       OR LOWER(COALESCE(pr.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))
                                       OR LOWER(COALESCE(pr.title, '')) LIKE LOWER(CONCAT('%', :q, '%')))
              AND (:currentUserId IS NULL OR NOT EXISTS (
                  SELECT 1
                  FROM user_blocks ub
                  WHERE (ub.blocker_id = :currentUserId AND ub.blocked_id = pr.creator_id)
                     OR (ub.blocker_id = pr.creator_id AND ub.blocked_id = :currentUserId)
              ))
            """,
            nativeQuery = true)
    Page<PartyRequest> findOpenWithRecommendedOrder(
            @Param("gameId")        Long gameId,
            @Param("skillLevel")    String skillLevel,
            @Param("playStyle")     String playStyle,
            @Param("platform")      String platform,
            @Param("language")      String language,
            @Param("region")        String region,
            @Param("q")             String q,
            @Param("currentUserId") Long currentUserId,
            Pageable pageable
    );

    @Query(value = """
            SELECT pr.* FROM party_requests pr
            JOIN party_members pm ON pm.party_request_id = pr.id
            WHERE pm.user_id = :userId
              AND pr.status IN ('COMPLETED', 'CANCELLED')
            ORDER BY pr.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*) FROM party_requests pr
            JOIN party_members pm ON pm.party_request_id = pr.id
            WHERE pm.user_id = :userId
              AND pr.status IN ('COMPLETED', 'CANCELLED')
            """,
            nativeQuery = true)
    Page<PartyRequest> findHistoryByUserId(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE PartyRequest p SET p.status = 'CANCELLED', p.isOpen = false, p.completedAt = CURRENT_TIMESTAMP WHERE p.status = 'OPEN' AND p.createdAt < :cutoff")
    int cancelStaleOpenParties(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("UPDATE PartyRequest p SET p.status = 'COMPLETED', p.isOpen = false, p.completedAt = CURRENT_TIMESTAMP, p.autoCompleted = true WHERE p.status = 'IN_GAME' AND p.startedAt < :cutoff")
    int completeStaleInGameParties(@Param("cutoff") Instant cutoff);

    @Query("""
            SELECT p FROM PartyRequest p
            WHERE p.status IN :statuses
              AND p.chat IS NOT NULL
              AND p.completedAt IS NOT NULL
              AND p.completedAt < :cutoff
              AND EXISTS (SELECT cp FROM ChatParticipant cp WHERE cp.chat = p.chat AND cp.archived = false)
            """)
    List<PartyRequest> findPartiesWithChatsToArchive(@Param("statuses") List<PartyStatus> statuses,
                                                     @Param("cutoff") Instant cutoff);
}
