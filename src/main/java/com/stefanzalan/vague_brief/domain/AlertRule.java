package com.stefanzalan.vague_brief.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Admin-configured rule describing when and where a matching event should be sent.
 *
 * @param destination email address or webhook URL, depending on {@code targetChannel}
 */
public record AlertRule(
        @NotNull UUID id,
        @NotBlank String name,
        @NotNull EventCategory category,
        @NotNull Severity minimumSeverity,
        @NotNull ChannelType targetChannel,
        @NotBlank String destination,
        boolean enabled
) {
}
