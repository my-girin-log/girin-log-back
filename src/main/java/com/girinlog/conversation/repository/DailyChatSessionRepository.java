package com.girinlog.conversation.repository;

import com.girinlog.conversation.domain.DailyChatSession;
import com.girinlog.conversation.domain.DailyChatSessionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyChatSessionRepository extends JpaRepository<DailyChatSession, Long> {

    Optional<DailyChatSession> findByIdAndUserId(Long id, Long userId);

    /** 06:00 배치용: 특정 serviceDate의 모든 사용자 세션을 상태로 조회. */
    List<DailyChatSession> findByServiceDateAndStatus(LocalDate serviceDate, DailyChatSessionStatus status);

    /** 06:00 배치용: 특정 serviceDate에 세션이 있는 사용자 id 목록(중복 제거). */
    @Query("select distinct session.userId from DailyChatSession session where session.serviceDate = :serviceDate")
    List<Long> findDistinctUserIdsByServiceDate(LocalDate serviceDate);

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
