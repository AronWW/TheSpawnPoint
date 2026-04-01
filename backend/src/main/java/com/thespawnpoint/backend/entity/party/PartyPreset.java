package com.thespawnpoint.backend.entity.party;

import com.thespawnpoint.backend.entity.game.Game;
import com.thespawnpoint.backend.entity.user.PlayStyle;
import com.thespawnpoint.backend.entity.user.Region;
import com.thespawnpoint.backend.entity.user.SkillLevel;
import com.thespawnpoint.backend.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "party_presets",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "slot_index"}))
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PartyPreset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "slot_index", nullable = false)
    private Integer slotIndex;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(name = "max_members", nullable = false)
    @Builder.Default
    private Integer maxMembers = 4;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "platform", columnDefinition = "varchar(20)[]")
    private List<String> platform;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "languages", columnDefinition = "varchar(50)[]")
    private List<String> languages;

    @Enumerated(EnumType.STRING)
    @Column(name = "skill_level", length = 20)
    private SkillLevel skillLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "play_style", length = 20)
    private PlayStyle playStyle;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "tags", columnDefinition = "varchar(30)[]")
    private List<String> tags;

    @Enumerated(EnumType.STRING)
    @Column(name = "region", length = 30)
    private Region region;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}

