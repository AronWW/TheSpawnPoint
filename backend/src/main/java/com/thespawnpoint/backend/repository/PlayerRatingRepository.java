package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.party.PlayerRating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlayerRatingRepository extends JpaRepository<PlayerRating, Long> {

    boolean existsByPartyRequestIdAndRaterId(Long partyRequestId, Long raterId);

    boolean existsByPartyRequestIdAndRaterIdAndRatedUserId(Long partyRequestId, Long raterId, Long ratedUserId);

    int countByRatedUserId(Long ratedUserId);

    @Query("SELECT COALESCE(SUM(pr.score), 0) FROM PlayerRating pr WHERE pr.ratedUser.id = :userId")
    int sumScoresByRatedUserId(@Param("userId") Long userId);

    @Query("SELECT COALESCE(AVG(pr.score), 3.5) FROM PlayerRating pr")
    double findGlobalAverage();

    @Query("SELECT pr.ratedUser.id, COUNT(pr), COALESCE(SUM(pr.score), 0) " +
           "FROM PlayerRating pr WHERE pr.ratedUser.id IN :userIds " +
           "GROUP BY pr.ratedUser.id")
    List<Object[]> findCountAndSumByRatedUserIds(@Param("userIds") List<Long> userIds);
}
