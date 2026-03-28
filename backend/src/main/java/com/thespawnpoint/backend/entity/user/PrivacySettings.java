package com.thespawnpoint.backend.entity.user;
import jakarta.persistence.*;
import lombok.*;
@Entity
@Table(name = "privacy_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrivacySettings {
    @Id
    @Column(name = "user_id", nullable = false)
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "friends_visibility", nullable = false, length = 10)
    @Builder.Default
    private VisibilityLevel friendsVisibility = VisibilityLevel.ALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_visibility", nullable = false, length = 10)
    @Builder.Default
    private VisibilityLevel statusVisibility = VisibilityLevel.ALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "favorite_games_visibility", nullable = false, length = 10)
    @Builder.Default
    private VisibilityLevel favoriteGamesVisibility = VisibilityLevel.ALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "stats_visibility", nullable = false, length = 10)
    @Builder.Default
    private VisibilityLevel statsVisibility = VisibilityLevel.ALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "socials_visibility", nullable = false, length = 10)
    @Builder.Default
    private VisibilityLevel socialsVisibility = VisibilityLevel.ALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "comments_policy", nullable = false, length = 10)
    @Builder.Default
    private VisibilityLevel commentsPolicy = VisibilityLevel.ALL;

    @Enumerated(EnumType.STRING)
    @Column(name = "achievements_visibility", nullable = false, length = 10)
    @Builder.Default
    private VisibilityLevel achievementsVisibility = VisibilityLevel.ALL;
}

