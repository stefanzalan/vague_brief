package com.stefanzalan.vague_brief.domain;

/**
 * Supported notification channels. New adapters register themselves by exposing
 * an additional enum constant and a matching {@code NotificationChannelService}.
 */
public enum ChannelType {
    EMAIL,
    SLACK
}
