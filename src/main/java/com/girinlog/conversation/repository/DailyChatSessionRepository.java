package com.girinlog.conversation.repository;

import com.girinlog.conversation.domain.DailyChatSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DailyChatSessionRepository extends JpaRepository<DailyChatSession, Long> {

    Optional<DailyChatSession> findByIdAndUserId(Long id, Long userId);
}
