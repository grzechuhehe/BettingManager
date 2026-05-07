package com.grzechuhehe.SportsBettingManagerApp.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {
    @Schema(example = "2023-10-27T10:00:00", description = "Timestamp of the error")
    private LocalDateTime timestamp;
    
    @Schema(example = "400", description = "HTTP status code")
    private int status;
    
    @Schema(example = "Bad Request", description = "HTTP error description")
    private String error;
    
    @Schema(example = "Invalid input data", description = "Detailed error message")
    private String message;
    
    @Schema(example = "/api/bets", description = "Request path that caused the error")
    private String path;
    
    @Schema(description = "Validation errors if any")
    private Map<String, String> validationErrors;
}