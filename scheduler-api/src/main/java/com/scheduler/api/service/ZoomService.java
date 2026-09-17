package com.scheduler.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Base64;
import java.util.Map;

@Service
public class ZoomService {

    @Value("${zoom.account-id}")
    private String accountId;

    @Value("${zoom.client-id}")
    private String clientId;

    @Value("${zoom.client-secret}")
    private String clientSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    private String getAccessToken() {
        String tokenUrl = "https://zoom.us/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        String authHeader = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
        headers.set("Authorization", "Basic " + authHeader);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "account_credentials");
        body.add("account_id", accountId);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(tokenUrl, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (String) response.getBody().get("access_token");
        }

        throw new RuntimeException("Failed to acquire Zoom OAuth access token");
    }

    public String createMeeting(String topic, Instant startTime, int durationMinutes) {
        String accessToken = getAccessToken();
        String meetingUrl = "https://api.zoom.us/v2/users/me/meetings";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> meetingPayload = Map.of(
            "topic", topic,
            "type", 2,
            "start_time", startTime.toString(),
            "duration", durationMinutes,
            "timezone", "America/Sao_Paulo",
            "settings", Map.of(
                "host_video", true,
                "participant_video", true,
                "join_before_host", false,
                "waiting_room", true
            )
        );

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(meetingPayload, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(meetingUrl, request, Map.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            return (String) response.getBody().get("join_url");
        }

        throw new RuntimeException("Failed to create Zoom meeting");
    }
}