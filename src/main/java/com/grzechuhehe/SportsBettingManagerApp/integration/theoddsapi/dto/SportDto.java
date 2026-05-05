package com.grzechuhehe.SportsBettingManagerApp.integration.theoddsapi.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class SportDto {
    private String key;
    private String group;
    private String title;
    private String description;
    private boolean active;
    
    @JsonProperty("has_outrights")
    private boolean hasOutrights;
}
