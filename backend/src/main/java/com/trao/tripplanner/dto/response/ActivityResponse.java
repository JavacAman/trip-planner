package com.trao.tripplanner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActivityResponse {
    private Long id;
    private String title;
    private String description;
    private String timeOfDay;
    private String location;
    private String estimatedDuration;
    private String estimatedCost;
    private Integer orderIndex;
}
