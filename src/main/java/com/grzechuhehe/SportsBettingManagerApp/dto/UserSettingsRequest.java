package com.grzechuhehe.SportsBettingManagerApp.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserSettingsRequest {
    private Integer evEdgeThreshold;
    private String displayCurrency;
}
