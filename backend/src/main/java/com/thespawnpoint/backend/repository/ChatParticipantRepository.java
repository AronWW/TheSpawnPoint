package com.thespawnpoint.backend.repository;

import com.thespawnpoint.backend.entity.chat.ChatParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatParticipantRepository extends JpaRepository<ChatParticipant, Long> {

    List<ChatParticipant> findByChatId(Long chatId);

    List<ChatParticipant> findByUserId(Long userId);

    boolean existsByChatIdAndUserId(Long chatId, Long userId);

    void deleteByChatIdAndUserId(Long chatId, Long userId);

    int countByChatId(Long chatId);

    Optional<ChatParticipant> findByChatIdAndUserId(Long chatId, Long userId);

    @Modifying
    @Query("UPDATE ChatParticipant cp SET cp.archived = true WHERE cp.chat.id = :chatId AND cp.archived = false")
    int archiveAllByChatId(@Param("chatId") Long chatId);
}
