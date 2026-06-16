package com.trao.tripplanner.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DayItineraryResponse {
    private Long id;
    private Integer dayNumber;
    private String theme;
    private List<ActivityResponse> activities;
}
