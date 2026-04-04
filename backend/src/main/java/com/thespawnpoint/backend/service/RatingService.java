package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.SubmitRatingsDTO;
import com.thespawnpoint.backend.dto.UserRatingDTO;
import com.thespawnpoint.backend.entity.party.PartyMember;
import com.thespawnpoint.backend.entity.party.PartyRequest;
import com.thespawnpoint.backend.entity.party.PartyStatus;
import com.thespawnpoint.backend.entity.party.PlayerRating;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RatingService {

    private static final int    BAYESIAN_C          = 15;
    private static final double BAYESIAN_DEFAULT_M  = 3.50;
    private static final int    MIN_RATINGS_VISIBLE = 20;
    private static final long   MIN_GAME_DURATION_MINUTES = 30;

    private final PlayerRatingRepository playerRatingRepository;
    private final PartyRequestRepository partyRequestRepository;
    private final PartyMemberRepository  partyMemberRepository;
    private final UserRepository         userRepository;

    @Transactional
    public void submitRatings(User rater, SubmitRatingsDTO dto) {
        PartyRequest party = partyRequestRepository.findById(dto.getPartyId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Party not found"));

        if (party.getStatus() != PartyStatus.COMPLETED) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Можна оцінювати лише завершені лобі");
        }

        if (Boolean.TRUE.equals(party.getAutoCompleted())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Автоматично завершені лобі не підлягають оцінюванню");
        }

        if (party.getStartedAt() == null || party.getCompletedAt() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Некоректні дані лобі");
        }

        long durationMinutes = Duration.between(party.getStartedAt(), party.getCompletedAt()).toMinutes();
        if (durationMinutes < MIN_GAME_DURATION_MINUTES) {
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Гра тривала менше 30 хвилин — оцінювання недоступне");
        }

        if (!partyMemberRepository.existsByPartyRequestIdAndUserId(dto.getPartyId(), rater.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Ви не були учасником цього лобі");
        }

        if (playerRatingRepository.existsByPartyRequestIdAndRaterId(dto.getPartyId(), rater.getId())) {
            throw new ApiException(HttpStatus.CONFLICT, "Ви вже оцінили гравців у цьому лобі");
        }

        List<PartyMember> members = partyMemberRepository.findByPartyRequestId(dto.getPartyId());
        Set<Long> memberIds = members.stream()
                .map(m -> m.getUser().getId())
                .collect(Collectors.toSet());

        Set<Long> ratedUserIds = new HashSet<>();

        for (SubmitRatingsDTO.IndividualRating ir : dto.getRatings()) {
            if (ir.getUserId().equals(rater.getId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Не можна оцінити себе");
            }

            if (!memberIds.contains(ir.getUserId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Користувач " + ir.getUserId() + " не був учасником лобі");
            }

            if (!ratedUserIds.add(ir.getUserId())) {
                throw new ApiException(HttpStatus.BAD_REQUEST,
                        "Дублікат оцінки для користувача " + ir.getUserId());
            }

            User ratedUser = userRepository.findById(ir.getUserId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

            PlayerRating rating = PlayerRating.builder()
                    .partyRequest(party)
                    .rater(rater)
                    .ratedUser(ratedUser)
                    .score(ir.getScore())
                    .build();
            playerRatingRepository.save(rating);
        }
    }

    public boolean canRateParty(Long userId, Long partyId) {
        PartyRequest party = partyRequestRepository.findById(partyId).orElse(null);
        if (party == null) return false;
        if (party.getStatus() != PartyStatus.COMPLETED) return false;
        if (Boolean.TRUE.equals(party.getAutoCompleted())) return false;
        if (party.getStartedAt() == null || party.getCompletedAt() == null) return false;

        long durationMinutes = Duration.between(party.getStartedAt(), party.getCompletedAt()).toMinutes();
        if (durationMinutes < MIN_GAME_DURATION_MINUTES) return false;

        if (!partyMemberRepository.existsByPartyRequestIdAndUserId(partyId, userId)) return false;
        return !playerRatingRepository.existsByPartyRequestIdAndRaterId(partyId, userId);
    }

    public UserRatingDTO getUserRating(Long userId) {
        int count = playerRatingRepository.countByRatedUserId(userId);
        if (count == 0) {
            return UserRatingDTO.builder()
                    .rating(null)
                    .ratingCount(0)
                    .ratingVisible(false)
                    .build();
        }
        int sum = playerRatingRepository.sumScoresByRatedUserId(userId);
        return buildDTO(count, sum);
    }

    public Map<Long, UserRatingDTO> getUserRatingsMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();

        List<Object[]> rows = playerRatingRepository.findCountAndSumByRatedUserIds(userIds);
        Map<Long, UserRatingDTO> map = new HashMap<>();

        for (Object[] row : rows) {
            Long uid   = (Long) row[0];
            int count  = ((Number) row[1]).intValue();
            int sum    = ((Number) row[2]).intValue();
            map.put(uid, buildDTO(count, sum));
        }

        for (Long id : userIds) {
            map.putIfAbsent(id, UserRatingDTO.builder()
                    .rating(null)
                    .ratingCount(0)
                    .ratingVisible(false)
                    .build());
        }

        return map;
    }

    public Double getVisibleRating(Long userId) {
        int count = playerRatingRepository.countByRatedUserId(userId);
        if (count < MIN_RATINGS_VISIBLE) return null;
        int sum = playerRatingRepository.sumScoresByRatedUserId(userId);
        return computeBayesian(count, sum);
    }

    public Map<Long, Double> getVisibleRatingsMap(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();

        List<Object[]> rows = playerRatingRepository.findCountAndSumByRatedUserIds(userIds);
        Map<Long, Double> map = new HashMap<>();

        for (Object[] row : rows) {
            Long uid  = (Long) row[0];
            int count = ((Number) row[1]).intValue();
            int sum   = ((Number) row[2]).intValue();
            if (count >= MIN_RATINGS_VISIBLE) {
                map.put(uid, computeBayesian(count, sum));
            }
        }

        return map;
    }

    private UserRatingDTO buildDTO(int count, int sum) {
        boolean visible = count >= MIN_RATINGS_VISIBLE;
        Double rating = visible ? computeBayesian(count, sum) : null;
        return UserRatingDTO.builder()
                .rating(rating)
                .ratingCount(count)
                .ratingVisible(visible)
                .build();
    }

    private double computeBayesian(int count, int sum) {
        double m;
        try {
            m = playerRatingRepository.findGlobalAverage();
        } catch (Exception e) {
            m = BAYESIAN_DEFAULT_M;
        }
        double bayesian = (BAYESIAN_C * m + sum) / (BAYESIAN_C + count);
        return Math.round(bayesian * 100.0) / 100.0;
    }
}
