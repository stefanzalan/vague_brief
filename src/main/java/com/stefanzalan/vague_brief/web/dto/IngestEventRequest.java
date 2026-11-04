package com.stefanzalan.vague_brief.web.dto;

import com.stefanzalan.vague_brief.domain.EventCategory;
import com.stefanzalan.vague_brief.domain.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;

/**
 * Request DTO for ingesting a system event.
 */
public record IngestEventRequest(
        @NotBlank String title,
        @NotBlank String description,
        @NotNull EventCategory category,
        @NotNull Severity severity,
        @NotNull Instant timestamp
) {
}
