package com.stefanzalan.vague_brief.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

/**
 * Incoming event evaluated against {@link AlertRule}s.
 */
public record SystemEvent(
        @NotNull UUID id,
        @NotBlank String title,
        @NotBlank String description,
        @NotNull EventCategory category,
        @NotNull Severity severity,
        @NotNull Instant timestamp
) {
}
