package com.stefanzalan.vague_brief.web.controller;

import com.stefanzalan.vague_brief.domain.AlertRule;
import com.stefanzalan.vague_brief.exception.RuleNotFoundException;
import com.stefanzalan.vague_brief.rule.AlertRuleRepository;
import com.stefanzalan.vague_brief.web.dto.CreateAlertRuleRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Admin Rules", description = "APIs for managing alert notification rules")
public class AdminRuleController {

    private final AlertRuleRepository ruleRepository;

    public AdminRuleController(AlertRuleRepository ruleRepository) {
        this.ruleRepository = ruleRepository;
    }

    @Operation(summary = "Create a new alert rule", description = "Creates a new alert rule with the specified configuration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Rule created successfully", content = @Content(schema = @Schema(implementation = AlertRule.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request body")
    })
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

    @Operation(summary = "List all alert rules", description = "Retrieves all configured alert rules")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of rules retrieved successfully")
    })
    @GetMapping
    public ResponseEntity<List<AlertRule>> listRules() {
        List<AlertRule> rules = ruleRepository.findAll();
        return ResponseEntity.ok(rules);
    }

    @Operation(summary = "Get alert rule by ID", description = "Retrieves a specific alert rule by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Rule retrieved successfully", content = @Content(schema = @Schema(implementation = AlertRule.class))),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @GetMapping("/{id}")
    public ResponseEntity<AlertRule> getRule(@Parameter(description = "Rule ID", required = true) @PathVariable UUID id) {
        AlertRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new RuleNotFoundException(id));
        return ResponseEntity.ok(rule);
    }

    @Operation(summary = "Delete alert rule", description = "Deletes an alert rule by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Rule deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Rule not found")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRule(@Parameter(description = "Rule ID", required = true) @PathVariable UUID id) {
        if (!ruleRepository.existsById(id)) {
            throw new RuleNotFoundException(id);
        }
        ruleRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
