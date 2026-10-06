package com.scheduler.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record BookingRequest(
    @NotBlank(message = "O nome do aluno é obrigatório")
    String studentName,

    @NotBlank(message = "O e-mail do aluno é obrigatório")
    @Email(message = "E-mail inválido")
    String studentEmail,

    @NotBlank(message = "O telefone do aluno é obrigatório")
    String studentPhone,

    @NotNull(message = "O horário de início é obrigatório")
    Instant startTime
) {}