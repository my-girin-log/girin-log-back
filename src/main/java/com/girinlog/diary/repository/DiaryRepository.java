package com.girinlog.diary.repository;

import com.girinlog.diary.domain.Diary;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DiaryRepository extends JpaRepository<Diary, Long> {

    Optional<Diary> findByUserIdAndServiceDate(Long userId, LocalDate serviceDate);

    @Query("""
            select diary
            from Diary diary
            where diary.userId = :userId
              and diary.serviceDate >= :startDate
              and diary.serviceDate <= :endDate
              and diary.serviceDate < :cursorDate
            order by diary.serviceDate desc
            """)
    List<Diary> findPageByDateRange(
            Long userId,
            LocalDate startDate,
            LocalDate endDate,
            LocalDate cursorDate,
            Pageable pageable
    );

    @Query("""
            select diary.serviceDate
            from Diary diary
            where diary.userId = :userId
              and diary.serviceDate >= :startDate
              and diary.serviceDate <= :endDate
            order by diary.serviceDate asc
            """)
    List<LocalDate> findServiceDatesByDateRange(Long userId, LocalDate startDate, LocalDate endDate);
}
