package com.app.travel_planner.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalTime;

@Getter
@Setter
public class CreateStopRequest {

    @NotNull
    private Long poiId;

    private LocalTime startTime;

    private LocalTime endTime;

    private String notes;

    private int orderIndex;
}
