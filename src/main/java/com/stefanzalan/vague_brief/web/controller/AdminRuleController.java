package com.stefanzalan.vague_brief.web.controller;

import com.stefanzalan.vague_brief.domain.AlertRule;
import com.stefanzalan.vague_brief.exception.RuleNotFoundException;
import com.stefanzalan.vague_brief.rule.AlertRuleRepository;
import com.stefanzalan.vague_brief.web.dto.CreateAlertRuleRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing alert rules.
 */
@RestController
@RequestMapping("/api/v1/admin/rules")
public class AdminRuleController {

    private final AlertRuleRepository ruleRepository;

    public AdminRuleController(AlertRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    /**
     * Create a new alert rule.
     *
     * @param request the rule creation request
     * @return the created rule with HTTP 201 Created
     */
    @PostMapping
    public ResponseEntity<AlertRule> createRule(@Valid @RequestBody CreateAlertRuleRequest request) {
        UUID id = UUID.randomUUID();
        AlertRule rule = new AlertRule(
                id,
                request.name(),
                request.category(),
                request.minimumSeverity(),
                request.targetChannel(),
                request.destination(),
                request.enabled()
        );

        AlertRule savedRule = ruleRepository.save(rule);

        return ResponseEntity
                .created(URI.create("/api/v1/admin/rules/" + id))
                .body(savedRule);
    }

    /**
     * List all configured alert rules.
     *
     * @return list of all rules with HTTP 200 OK
     */
    @GetMapping
    public ResponseEntity<List<AlertRule>> listRules() {
        List<AlertRule> rules = ruleRepository.findAll();
        return ResponseEntity.ok(rules);
    }

    /**
     * Get a specific alert rule by ID.
     *
     * @param id the rule ID
     * @return the rule with HTTP 200 OK
     * @throws RuleNotFoundException if rule not found
     */
    @GetMapping("/{id}")
    public ResponseEntity<AlertRule> getRule(@PathVariable UUID id) {
        AlertRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new RuleNotFoundException(id));
        return ResponseEntity.ok(rule);
    }

    /**
     * Delete an alert rule by ID.
     *
     * @param id the rule ID
     * @return HTTP 204 No Content
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@PathVariable UUID id) {
        if (!ruleRepository.existsById(id)) {
            throw new RuleNotFoundException(id);
        }
        ruleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
