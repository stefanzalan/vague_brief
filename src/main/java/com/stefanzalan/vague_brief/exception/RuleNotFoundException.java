package com.stefanzalan.vague_brief.exception;

import java.util.UUID;

public class RuleNotFoundException extends RuntimeException {

    private final UUID ruleId;

    public RuleNotFoundException(UUID ruleId) {
        super("Alert rule not found: " + ruleId);
        this.ruleId = ruleId;
    }

    public UUID getRuleId() {
        return ruleId;
    }
}
