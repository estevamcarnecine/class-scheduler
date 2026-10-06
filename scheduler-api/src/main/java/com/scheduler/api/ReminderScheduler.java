package com.scheduler.api.scheduler;

import com.scheduler.api.domain.Booking;
import com.scheduler.api.domain.BookingStatus;
import com.scheduler.api.repository.BookingRepository;
import com.scheduler.api.service.TwilioWhatsAppService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class ReminderScheduler {

    private static final Logger log = LoggerFactory.getLogger(ReminderScheduler.class);

    private final BookingRepository bookingRepository;
    private final TwilioWhatsAppService twilioWhatsAppService;

    public ReminderScheduler(BookingRepository bookingRepository, TwilioWhatsAppService twilioWhatsAppService) {
        this.bookingRepository = bookingRepository;
        this.twilioWhatsAppService = twilioWhatsAppService;
    }

    // Runs every minute on the 0th second
    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void processFiveMinuteReminders() {
        Instant now = Instant.now();
        // Look between 4 and 6 minutes ahead to ensure no missed slots due to second alignment
        Instant windowStart = now.plus(Duration.ofMinutes(4));
        Instant windowEnd = now.plus(Duration.ofMinutes(6));

        List<Booking> dueBookings = bookingRepository.findBookingsDueForReminder(
            BookingStatus.CONFIRMED,
            windowStart,
            windowEnd
        );

        for (Booking booking : dueBookings) {
            log.info("Triggering 5-min WhatsApp reminder for booking ID {} ({})", booking.getId(), booking.getStudentName());
            twilioWhatsAppService.sendReminder(
                booking.getStudentPhone(),
                booking.getStudentName(),
                booking.getZoomJoinUrl()
            );
            booking.setWhatsappReminderSent(true);
            bookingRepository.save(booking);
        }
    }
}