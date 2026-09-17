package com.app.travel_planner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalTime;

@Getter
@AllArgsConstructor
public class StopResponse {

    private final Long id;
    private final Long poiId;
    private final String poiName;
    private final Double lat;
    private final Double lng;
    private final LocalTime startTime;
    private final LocalTime endTime;
    private final String notes;
    private final int orderIndex;
}
