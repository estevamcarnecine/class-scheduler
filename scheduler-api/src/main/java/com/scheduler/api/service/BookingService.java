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

    @Transactional(readOnly = true)
    public AvailabilityResponse getAvailableSlots(LocalDate date) {
        // Horário de atendimento padrão: 08:00 às 18:00 (Brasília)
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

            // Intervalo de slots a cada 30 minutos (20 min de aula + 10 min de respiro)
            current = current.plusMinutes(30);
        }

        return new AvailabilityResponse(date.toString(), available);
    }
}