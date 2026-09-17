package com.app.travel_planner.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
public class ItineraryDayResponse {

    private final Long id;
    private final int dayNumber;
    private final LocalDate date;
    private final List<StopResponse> stops;
}
