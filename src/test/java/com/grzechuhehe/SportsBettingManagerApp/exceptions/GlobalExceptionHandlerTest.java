package com.grzechuhehe.SportsBettingManagerApp.exceptions;

import com.grzechuhehe.SportsBettingManagerApp.dto.ApiErrorResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void shouldHandleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Invalid state");
        MockHttpServletRequest request = new MockHttpServletRequest();
        ResponseEntity<ApiErrorResponse> response = handler.handleIllegalArgumentException(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Invalid state", response.getBody().getMessage());
    }

    @Test
    void shouldHandleMethodArgumentNotValidException() {
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(
                new FieldError("object", "field1", "Error 1"),
                new FieldError("object", "field2", "Error 2")
        ));
        MethodArgumentNotValidException ex = new MethodArgumentNotValidException(mock(MethodParameter.class), bindingResult);
        MockHttpServletRequest request = new MockHttpServletRequest();

        ResponseEntity<ApiErrorResponse> response = handler.handleValidationExceptions(ex, request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        // The existing handler puts errors in a map, not in the message string directly.
        // Let's check the map.
        assertTrue(response.getBody().getValidationErrors().containsKey("field1"));
        assertEquals("Error 1", response.getBody().getValidationErrors().get("field1"));
        assertTrue(response.getBody().getValidationErrors().containsKey("field2"));
        assertEquals("Error 2", response.getBody().getValidationErrors().get("field2"));
    }
}
