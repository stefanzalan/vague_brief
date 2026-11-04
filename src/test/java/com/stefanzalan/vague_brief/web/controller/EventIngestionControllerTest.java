package com.stefanzalan.vague_brief.web.controller;

import com.stefanzalan.vague_brief.evaluation.EventEvaluationEngine;
import com.stefanzalan.vague_brief.web.dto.IngestEventRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventIngestionControllerTest {

    @Mock
    private EventEvaluationEngine evaluationEngine;

    @InjectMocks
    private EventIngestionController controller;

    @Test
    void ingestEvent_withValidData_returns202() {
        // Given
        IngestEventRequest request = new IngestEventRequest(
                "Stock Market Crash",
                "Major indices dropped by 10%",
                com.stefanzalan.vague_brief.domain.EventCategory.MARKET_MOVEMENT,
                com.stefanzalan.vague_brief.domain.Severity.CRITICAL,
                Instant.parse("2024-01-15T10:30:00Z")
        );

        // When
        ResponseEntity<String> response = controller.ingestEvent(request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).contains("Event accepted for processing");
        verify(evaluationEngine).processEvent(any());
    }

    @Test
    void ingestEvent_withValidData_processesEvent() {
        // Given
        IngestEventRequest request = new IngestEventRequest(
                "Breaking News",
                "Important event",
                com.stefanzalan.vague_brief.domain.EventCategory.BREAKING_NEWS,
                com.stefanzalan.vague_brief.domain.Severity.INFO,
                Instant.now()
        );

        // When
        controller.ingestEvent(request);

        // Then
        verify(evaluationEngine).processEvent(any());
    }

    @Test
    void ingestEvent_generatesEventId() {
        // Given
        IngestEventRequest request = new IngestEventRequest(
                "Test Event",
                "Test description",
                com.stefanzalan.vague_brief.domain.EventCategory.NATURAL_DISASTER,
                com.stefanzalan.vague_brief.domain.Severity.WARNING,
                Instant.now()
        );

        // When
        ResponseEntity<String> response = controller.ingestEvent(request);

        // Then
        assertThat(response.getBody()).contains("Event accepted for processing");
    }
}
