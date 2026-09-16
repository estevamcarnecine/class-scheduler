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
    private final GoogleCalendarService googleCalendarService;

    public BookingService(BookingRepository bookingRepository, GoogleCalendarService googleCalendarService) {
        this.bookingRepository = bookingRepository;
        this.googleCalendarService = googleCalendarService;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Instant startTime = request.startTime();
        Instant endTime = startTime.plus(CLASS_DURATION);

        // 1. Checa conflito no banco PostgreSQL
        boolean hasConflict = bookingRepository.existsOverlappingBooking(
            startTime, 
            endTime, 
            BookingStatus.CONFIRMED
        );

        if (hasConflict) {
            throw new SlotConflictException("This 20-minute slot is already booked.");
        }

        // 2. Dispara a criação do evento no Google Calendar
        String googleEventId = googleCalendarService.createCalendarEvent(
            request.studentName(),
            request.studentEmail(),
            startTime,
            endTime
        );

        // 3. Persiste no banco com o ID do evento do Google
        Booking booking = new Booking(
            request.studentName(),
            request.studentEmail(),
            startTime,
            endTime
        );
        booking.setGoogleCalendarEventId(googleEventId);

        Booking saved = bookingRepository.save(booking);

        return BookingResponse.fromEntity(saved);
    }
}