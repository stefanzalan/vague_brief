package com.stefanzalan.vague_brief.exception;

import com.stefanzalan.vague_brief.domain.ChannelType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsUnsupportedChannelToBadRequest() {
        ProblemDetail problem = handler.handleUnsupportedChannel(new UnsupportedChannelException(ChannelType.SLACK));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Unsupported Channel");
        assertThat(problem.getDetail()).contains("SLACK");
        assertThat(problem.getProperties()).containsEntry("channelType", ChannelType.SLACK);
    }

    @Test
    void mapsMissingRuleToNotFound() {
        UUID ruleId = UUID.fromString("11111111-1111-1111-1111-111111111111");

        ProblemDetail problem = handler.handleRuleNotFound(new RuleNotFoundException(ruleId));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(problem.getTitle()).isEqualTo("Alert Rule Not Found");
        assertThat(problem.getProperties()).containsEntry("ruleId", ruleId);
    }

    @Test
    void mapsConstraintViolationsToBadRequestDetail() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("name");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must not be blank");

        ProblemDetail problem = handler.handleConstraintViolation(
                new ConstraintViolationException(Set.of(violation)));

        assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(problem.getTitle()).isEqualTo("Validation Failed");
        assertThat(problem.getDetail()).isEqualTo("name: must not be blank");
    }
}
