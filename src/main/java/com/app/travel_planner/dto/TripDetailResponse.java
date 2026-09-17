package com.app.travel_planner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class TripDetailResponse {

    private final Long id;
    private final String destination;
    private final LocalDate startDate;
    private final LocalDate endDate;
    private final String budget;
    private final String status;
    private final List<ItineraryDayResponse> days;
}
