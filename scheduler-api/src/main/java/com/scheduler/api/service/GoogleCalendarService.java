package com.scheduler.api.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.Instant;
import java.util.Collections;

@Service
public class GoogleCalendarService {

    @Value("${google.calendar.credentials.path}")
    private String credentialsPath;

    @Value("${google.calendar.id}")
    private String calendarId;

    private Calendar getCalendarClient() throws IOException, GeneralSecurityException {
        GoogleCredentials credentials = GoogleCredentials
                .fromStream(new ClassPathResource(credentialsPath).getInputStream())
                .createScoped(Collections.singleton(CalendarScopes.CALENDAR));

        return new Calendar.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials)
        ).setApplicationName("Booky").build();
    }

    public String createCalendarEvent(String studentName, String studentEmail, Instant start, Instant end, String zoomJoinUrl) {
        try {
            Calendar service = getCalendarClient();

            Event event = new Event()
                    .setSummary("Sessão Booky: " + studentName)
                    .setLocation(zoomJoinUrl)
                    .setDescription(
                        "Sessão de 20 minutos com " + studentName + ".\n\n" +
                        "Link de Acesso Zoom:\n" + zoomJoinUrl + "\n\n" +
                        "E-mail do Cliente: " + studentEmail
                    );

            DateTime startDateTime = new DateTime(start.toEpochMilli());
            event.setStart(new EventDateTime().setDateTime(startDateTime));

            DateTime endDateTime = new DateTime(end.toEpochMilli());
            event.setEnd(new EventDateTime().setDateTime(endDateTime));

            Event createdEvent = service.events().insert(calendarId, event).execute();
            return createdEvent.getId();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create Google Calendar event: " + e.getMessage(), e);
        }
    }
}