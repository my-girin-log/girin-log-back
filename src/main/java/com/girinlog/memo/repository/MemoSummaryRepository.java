package com.girinlog.memo.repository;

import com.girinlog.memo.domain.MemoSummary;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

public interface MemoSummaryRepository extends JpaRepository<MemoSummary, Long> {

    @EntityGraph(attributePaths = "items")
    List<MemoSummary> findByUserIdAndServiceDateOrderByCreatedAtAsc(Long userId, LocalDate serviceDate);

    @EntityGraph(attributePaths = "items")
    List<MemoSummary> findByUserIdAndIdIn(Long userId, Collection<Long> ids);
}
