package com.girinlog.conversation.repository;

import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.DailyChatSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyChatSessionRepository extends JpaRepository<DailyChatSession, Long> {

    Optional<DailyChatSession> findByIdAndUserId(Long id, Long userId);

    List<DailyChatSession> findByUserIdAndServiceDateAndStatusOrderByCreatedAtAsc(
            Long userId,
            LocalDate serviceDate,
            DailyChatSessionStatus status
    );

    List<DailyChatSession> findByUserIdAndServiceDateBetweenAndStatusOrderByServiceDateAscCreatedAtAsc(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            DailyChatSessionStatus status
    );
}
