package com.stefanzalan.vague_brief.domain;

/**
 * Event severity used both for incoming events and as a rule matching threshold.
 * Ordinal order is INFO &lt; WARNING &lt; CRITICAL.
 */
public enum Severity {
    INFO,
    WARNING,
    CRITICAL;

    public boolean meetsMinimum(Severity minimum) {
        return this.ordinal() >= minimum.ordinal();
    }
}
