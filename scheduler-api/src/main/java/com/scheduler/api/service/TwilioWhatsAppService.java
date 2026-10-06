package com.scheduler.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.Map;

@Service
public class TwilioWhatsAppService {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppService.class);

    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${twilio.whatsapp-from}")
    private String whatsappFrom;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendReminder(String toPhone, String studentName, String zoomUrl) {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

        HttpHeaders headers = new HttpHeaders();
        String auth = accountSid + ":" + authToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes());
        headers.set("Authorization", "Basic " + encodedAuth);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        // Normalize phone to Twilio WhatsApp format (e.g. +5515999999999)
        String formattedTo = toPhone.replaceAll("[^0-9+]", "");
        if (!formattedTo.startsWith("+")) {
            formattedTo = "+" + formattedTo;
        }
        if (!formattedTo.startsWith("whatsapp:")) {
            formattedTo = "whatsapp:" + formattedTo;
        }

        String messageBody = String.format(
            "Olá %s! 👋 Sua sessão na Booky começa em 5 minutos.\n\nLink da sua sala no Zoom:\n%s\n\nTe espero lá!",
            studentName,
            zoomUrl
        );

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("From", whatsappFrom);
        body.add("To", formattedTo);
        body.add("Body", messageBody);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);
            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("WhatsApp reminder successfully dispatched to {}", formattedTo);
            } else {
                log.error("Failed to send WhatsApp reminder. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error dispatching WhatsApp message via Twilio: {}", e.getMessage(), e);
        }
    }
}