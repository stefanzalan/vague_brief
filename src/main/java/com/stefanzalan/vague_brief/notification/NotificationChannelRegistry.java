package com.stefanzalan.vague_brief.notification;

import com.stefanzalan.vague_brief.domain.ChannelType;
import com.stefanzalan.vague_brief.exception.UnsupportedChannelException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * O(1) lookup of {@link NotificationChannelService} implementations by channel type.
 */
@Component
public class NotificationChannelRegistry {

    private final Map<ChannelType, NotificationChannelService> channels;

    public NotificationChannelRegistry(List<NotificationChannelService> channelServices) {
        this.channels = channelServices.stream()
                .collect(Collectors.toUnmodifiableMap(
                        NotificationChannelService::getSupportedChannel,
                        Function.identity(),
                        (first, duplicate) -> {
                            throw new IllegalStateException(
                                    "Duplicate NotificationChannelService for channel "
                                            + first.getSupportedChannel());
                        }
                ));
    }

    public NotificationChannelService getChannel(ChannelType channelType) {
        NotificationChannelService service = channels.get(channelType);
        if (service == null) {
            throw new UnsupportedChannelException(channelType);
        }
        return service;
    }

    public boolean supports(ChannelType channelType) {
        return channels.containsKey(channelType);
    }
}
