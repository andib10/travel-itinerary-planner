package com.app.travel_planner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
public class TripSummaryResponse {

    private final Long id;
    private final String destination;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String budget;
    private final String status;
}
