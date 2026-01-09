package com.grzechuhehe.SportsBettingManagerApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private Long id;
    private String username;
    
    public JwtResponse(String token) {
        this.token = token;
    }
}