package com.scheduler.api.service;

import com.scheduler.api.domain.Booking;
import com.scheduler.api.domain.BookingStatus;
import com.scheduler.api.dto.AvailabilityResponse;
import com.scheduler.api.dto.BookingRequest;
import com.scheduler.api.dto.BookingResponse;
import com.scheduler.api.exception.SlotConflictException;
import com.scheduler.api.repository.BookingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.ArrayList;
import java.util.List;

@Service
public class BookingService {

    public static final Duration CLASS_DURATION = Duration.ofMinutes(20);
    public static final ZoneId DEFAULT_ZONE = ZoneId.of("America/Sao_Paulo");

    private final BookingRepository bookingRepository;
    private final GoogleCalendarService googleCalendarService;
    private final ZoomService zoomService;

    public BookingService(
            BookingRepository bookingRepository,
            GoogleCalendarService googleCalendarService,
            ZoomService zoomService) {
        this.bookingRepository = bookingRepository;
        this.googleCalendarService = googleCalendarService;
        this.zoomService = zoomService;
    }

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        Instant startTime = request.startTime();
        Instant endTime = startTime.plus(CLASS_DURATION);

        // 1. Double-booking check in PostgreSQL
        boolean hasConflict = bookingRepository.existsOverlappingBooking(
            startTime, 
            endTime, 
            BookingStatus.CONFIRMED
        );

        if (hasConflict) {
            throw new SlotConflictException("This 20-minute slot is already booked.");
        }

        // 2. Generate unique Zoom Meeting Room
        String zoomJoinUrl = zoomService.createMeeting(
            "Sessão Booky - " + request.studentName(),
            startTime,
            20
        );

        // 3. Dispatch to Google Calendar with unique Zoom URL
        String googleEventId = googleCalendarService.createCalendarEvent(
            request.studentName(),
            request.studentEmail(),
            startTime,
            endTime,
            zoomJoinUrl
        );

        // 4. Save entity with Google ID and Zoom URL
        Booking booking = new Booking(
            request.studentName(),
            request.studentEmail(),
            startTime,
            endTime
        );
        booking.setGoogleCalendarEventId(googleEventId);
        booking.setZoomJoinUrl(zoomJoinUrl);

        Booking saved = bookingRepository.save(booking);

        return BookingResponse.fromEntity(saved);
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailableSlots(LocalDate date) {
        LocalTime workStart = LocalTime.of(8, 0);
        LocalTime workEnd = LocalTime.of(18, 0);

        ZonedDateTime dayStart = date.atTime(workStart).atZone(DEFAULT_ZONE);
        ZonedDateTime dayEnd = date.atTime(workEnd).atZone(DEFAULT_ZONE);

        List<Booking> bookedInDay = bookingRepository.findByStartTimeBetweenAndStatus(
            dayStart.toInstant(),
            dayEnd.toInstant(),
            BookingStatus.CONFIRMED
        );

        List<Instant> available = new ArrayList<>();
        ZonedDateTime current = dayStart;

        while (current.plus(CLASS_DURATION).isBefore(dayEnd) || current.plus(CLASS_DURATION).isEqual(dayEnd)) {
            Instant slotStart = current.toInstant();
            Instant slotEnd = slotStart.plus(CLASS_DURATION);

            boolean isOccupied = bookedInDay.stream().anyMatch(b ->
                b.getStartTime().isBefore(slotEnd) && b.getEndTime().isAfter(slotStart)
            );

            if (!isOccupied) {
                available.add(slotStart);
            }

            current = current.plusMinutes(30);
        }

        return new AvailabilityResponse(date.toString(), available);
    }
}