package com.thespawnpoint.backend.service;

import com.thespawnpoint.backend.dto.PartyPresetDTO;
import com.thespawnpoint.backend.dto.SavePartyPresetDTO;
import com.thespawnpoint.backend.entity.game.Game;
import com.thespawnpoint.backend.entity.party.PartyPreset;
import com.thespawnpoint.backend.entity.user.PlayStyle;
import com.thespawnpoint.backend.entity.user.Region;
import com.thespawnpoint.backend.entity.user.SkillLevel;
import com.thespawnpoint.backend.entity.user.User;
import com.thespawnpoint.backend.exception.ApiException;
import com.thespawnpoint.backend.repository.GameRepository;
import com.thespawnpoint.backend.repository.PartyPresetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PartyPresetService {

    private final PartyPresetRepository presetRepository;
    private final GameRepository gameRepository;

    public List<PartyPresetDTO> getPresets(User user) {
        return presetRepository.findByUserIdOrderBySlotIndex(user.getId())
                .stream()
                .map(this::toDTO)
                .toList();
    }

    @Transactional
    public PartyPresetDTO savePreset(User user, SavePartyPresetDTO dto) {
        if (dto.getSlotIndex() < 0 || dto.getSlotIndex() > 9) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Слот має бути від 0 до 9");
        }

        Game game = gameRepository.findById(dto.getGameId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Гру не знайдено"));

        SkillLevel skillLevel = parseEnum(SkillLevel.class, dto.getSkillLevel());
        PlayStyle playStyle = parseEnum(PlayStyle.class, dto.getPlayStyle());
        Region region = parseEnum(Region.class, dto.getRegion());

        int maxMembers = dto.getMaxMembers() != null ? dto.getMaxMembers() : game.getMaxPartySize();
        if (maxMembers < 2) maxMembers = 2;
        if (maxMembers > game.getMaxPartySize()) maxMembers = game.getMaxPartySize();

        PartyPreset preset = presetRepository.findByUserIdAndSlotIndex(user.getId(), dto.getSlotIndex())
                .orElse(PartyPreset.builder()
                        .user(user)
                        .slotIndex(dto.getSlotIndex())
                        .build());

        preset.setName(dto.getName());
        preset.setGame(game);
        preset.setMaxMembers(maxMembers);
        preset.setPlatform(dto.getPlatform());
        preset.setLanguages(dto.getLanguages());
        preset.setSkillLevel(skillLevel);
        preset.setPlayStyle(playStyle);
        preset.setTags(dto.getTags());
        preset.setRegion(region);

        presetRepository.save(preset);
        return toDTO(preset);
    }

    @Transactional
    public void deletePreset(User user, Long presetId) {
        PartyPreset preset = presetRepository.findById(presetId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Шаблон не знайдено"));
        if (!preset.getUser().getId().equals(user.getId())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Це не ваш шаблон");
        }
        presetRepository.delete(preset);
    }

    private PartyPresetDTO toDTO(PartyPreset preset) {
        return PartyPresetDTO.builder()
                .id(preset.getId())
                .name(preset.getName())
                .slotIndex(preset.getSlotIndex())
                .gameId(preset.getGame().getId())
                .gameName(preset.getGame().getName())
                .gameImageUrl(preset.getGame().getImageUrl())
                .maxMembers(preset.getMaxMembers())
                .platform(preset.getPlatform())
                .languages(preset.getLanguages())
                .skillLevel(preset.getSkillLevel() != null ? preset.getSkillLevel().name() : null)
                .playStyle(preset.getPlayStyle() != null ? preset.getPlayStyle().name() : null)
                .tags(preset.getTags())
                .region(preset.getRegion() != null ? preset.getRegion().name() : null)
                .build();
    }

    private <E extends Enum<E>> E parseEnum(Class<E> cls, String value) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(cls, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

