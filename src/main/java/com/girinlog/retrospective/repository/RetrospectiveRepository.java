package com.girinlog.retrospective.repository;

import com.girinlog.retrospective.domain.Retrospective;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {

    Optional<Retrospective> findByIdAndUserId(Long id, Long userId);

    @Query("""
            select retrospective
            from Retrospective retrospective
            where retrospective.userId = :userId
              and (:cursorId is null or retrospective.id < :cursorId)
            order by retrospective.id desc
            """)
    List<Retrospective> findPage(Long userId, Long cursorId, Pageable pageable);
}
