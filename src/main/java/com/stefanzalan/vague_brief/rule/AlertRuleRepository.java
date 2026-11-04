package com.stefanzalan.vague_brief.rule;

import com.stefanzalan.vague_brief.domain.AlertRule;
import org.springframework.stereotype.Repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory repository for storing and retrieving alert rules.
 * Thread-safe using ConcurrentHashMap for concurrent access.
 */
@Repository
public class AlertRuleRepository {

    private final Map<UUID, AlertRule> rules = new ConcurrentHashMap<>();

    public AlertRule save(AlertRule rule) {
        rules.put(rule.id(), rule);
        return rule;
    }

    public Optional<AlertRule> findById(UUID id) {
        return Optional.ofNullable(rules.get(id));
    }

    public List<AlertRule> findAll() {
        return new ArrayList<>(rules.values());
    }

    public List<AlertRule> findEnabled() {
        return rules.values().stream()
                .filter(AlertRule::enabled)
                .toList();
    }

    public void deleteById(UUID id) {
        rules.remove(id);
    }

    public boolean existsById(UUID id) {
        return rules.containsKey(id);
    }

    public void clear() {
        rules.clear();
    }
}
