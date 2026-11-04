package com.stefanzalan.vague_brief.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Channel-agnostic payload handed to a {@code NotificationChannelService}.
 */
public record NotificationPayload(
        @NotBlank String recipient,
        @NotBlank String subject,
        @NotBlank String messageBody,
        @NotNull Severity severity
) {
}
