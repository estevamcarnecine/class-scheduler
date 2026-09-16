package com.scheduler.api.dto;

import java.time.Instant;
import java.util.List;

public record AvailabilityResponse(
    String date,
    List<Instant> availableSlots
) {}