package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.party.PartyPreset;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PartyPresetRepository extends JpaRepository<PartyPreset, Long> {

    List<PartyPreset> findByUserIdOrderBySlotIndex(Long userId);

    Optional<PartyPreset> findByUserIdAndSlotIndex(Long userId, Integer slotIndex);

    int countByUserId(Long userId);

    void deleteByIdAndUserId(Long id, Long userId);
}

