package com.alkywallet.dto;

import jakarta.validation.constraints.NotBlank;

public record PreguntaAsistenteDTO(
        @NotBlank(message = "La pregunta no puede estar vacía")
        String pregunta
) {
}
