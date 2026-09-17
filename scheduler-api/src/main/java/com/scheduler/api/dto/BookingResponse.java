package com.scheduler.api.dto;

import com.scheduler.api.domain.Booking;
import com.scheduler.api.domain.BookingStatus;
import java.time.Instant;

public record BookingResponse(
    Long id,
    String studentName,
    String studentEmail,
    Instant startTime,
    Instant endTime,
    BookingStatus status,
    String googleCalendarEventId,
    String zoomJoinUrl
) {
    public static BookingResponse fromEntity(Booking booking) {
        return new BookingResponse(
            booking.getId(),
            booking.getStudentName(),
            booking.getStudentEmail(),
            booking.getStartTime(),
            booking.getEndTime(),
            booking.getStatus(),
            booking.getGoogleCalendarEventId(),
            booking.getZoomJoinUrl()
        );
    }
}