package com.stefanzalan.vague_brief.exception;

import com.stefanzalan.vague_brief.domain.ChannelType;

public class UnsupportedChannelException extends RuntimeException {

    private final ChannelType channelType;

    public UnsupportedChannelException(ChannelType channelType) {
        super("No notification channel registered for " + channelType);
        this.channelType = channelType;
    }

    public ChannelType getChannelType() {
        return channelType;
    }
}
