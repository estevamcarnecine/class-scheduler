package com.scheduler.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record BookingRequest(
    @NotBlank(message = "Student name is required")
    String studentName,

    @NotBlank(message = "Student email is required")
    @Email(message = "Must be a valid email address")
    String studentEmail,

    @NotNull(message = "Start time is required")
    @Future(message = "Start time must be in the future")
    Instant startTime
) {}