package com.stefanzalan.vague_brief.web.controller;

import com.stefanzalan.vague_brief.domain.SystemEvent;
import com.stefanzalan.vague_brief.evaluation.EventEvaluationEngine;
import com.stefanzalan.vague_brief.web.dto.IngestEventRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for ingesting system events.
 */
@RestController
@RequestMapping("/api/v1/events")
public class EventIngestionController {

    private final EventEvaluationEngine evaluationEngine;

    public EventIngestionController(EventEvaluationEngine evaluationEngine) {
        this.evaluationEngine = evaluationEngine;
    }

    /**
     * Ingest a system event for rule matching and notification dispatch.
     *
     * @param request the event ingestion request
     * @return HTTP 202 Accepted with confirmation message
     */
    @PostMapping("/ingest")
    public ResponseEntity<String> ingestEvent(@Valid @RequestBody IngestEventRequest request) {
        SystemEvent event = new SystemEvent(
                UUID.randomUUID(),
                request.title(),
                request.description(),
                request.category(),
                request.severity(),
                request.timestamp()
        );

        evaluationEngine.processEvent(event);

        return ResponseEntity
                .status(HttpStatus.ACCEPTED)
                .body("Event accepted for processing: " + event.id());
    }
}
