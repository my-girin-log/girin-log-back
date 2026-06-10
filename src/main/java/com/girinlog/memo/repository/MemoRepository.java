package com.girinlog.memo.repository;

import com.girinlog.memo.domain.Memo;
import com.girinlog.memo.domain.MemoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    List<Memo> findByUserIdAndServiceDateOrderByCreatedAtAsc(Long userId, LocalDate serviceDate);

    List<Memo> findByUserIdAndServiceDateAndStatusOrderByCreatedAtAsc(
            Long userId,
            LocalDate serviceDate,
            MemoStatus status
    );

    Optional<Memo> findByIdAndUserId(Long id, Long userId);

    /** 06:00 배치용: 특정 serviceDate의 모든 사용자 Memo를 상태로 조회. */
    List<Memo> findByServiceDateAndStatus(LocalDate serviceDate, MemoStatus status);
}
