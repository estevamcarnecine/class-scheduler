package com.scheduler.api.service;

import com.scheduler.api.domain.Booking;
import com.scheduler.api.domain.BookingStatus;
import com.scheduler.api.dto.BookingRequest;
import com.scheduler.api.dto.BookingResponse;
import com.scheduler.api.exception.SlotConflictException;
import com.scheduler.api.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Service
public class BookingService {

    public static final Duration CLASS_DURATION = Duration.ofMinutes(20);

    private final BookingRepository bookingRepository;

    public BookingService(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Instant startTime = request.startTime();
        Instant endTime = startTime.plus(CLASS_DURATION);

        // Check for double-booking conflict
        boolean hasConflict = bookingRepository.existsOverlappingBooking(
            startTime, 
            endTime, 
            BookingStatus.CONFIRMED
        );

        // Throw our custom domain exception instead of generic IllegalStateException
        if (hasConflict) {
            throw new SlotConflictException("This 20-minute slot is already booked.");
        }

        Booking booking = new Booking(
            request.studentName(),
            request.studentEmail(),
            startTime,
            endTime
        );

        Booking saved = bookingRepository.save(booking);

        return BookingResponse.fromEntity(saved);
    }
}