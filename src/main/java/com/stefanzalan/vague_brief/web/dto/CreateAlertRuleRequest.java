package com.stefanzalan.vague_brief.web.dto;

import com.stefanzalan.vague_brief.domain.ChannelType;
import com.stefanzalan.vague_brief.domain.EventCategory;
import com.stefanzalan.vague_brief.domain.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a new alert rule.
 */
public record CreateAlertRuleRequest(
        @NotBlank String name,
        @NotNull EventCategory category,
        @NotNull Severity minimumSeverity,
        @NotNull ChannelType targetChannel,
        @NotBlank String destination,
        boolean enabled
) {
}
