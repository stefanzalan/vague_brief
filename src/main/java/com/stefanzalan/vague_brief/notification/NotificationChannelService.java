package com.stefanzalan.vague_brief.notification;

import com.stefanzalan.vague_brief.domain.ChannelType;
import com.stefanzalan.vague_brief.domain.NotificationPayload;

/**
 * Strategy for dispatching a notification on a single channel.
 */
public interface NotificationChannelService {

    ChannelType getSupportedChannel();

    void sendNotification(NotificationPayload payload);
}
