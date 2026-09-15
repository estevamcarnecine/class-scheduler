package com.scheduler.api.repository;

import com.scheduler.api.domain.Booking;
import com.scheduler.api.domain.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    @Query("SELECT COUNT(b) > 0 FROM Booking b WHERE b.status = :status AND b.startTime < :endTime AND b.endTime > :startTime")
    boolean existsOverlappingBooking(
        @Param("startTime") Instant startTime,
        @Param("endTime") Instant endTime,
        @Param("status") BookingStatus status
    );

    List<Booking> findByStartTimeBetweenAndStatus(Instant rangeStart, Instant rangeEnd, BookingStatus status);
}