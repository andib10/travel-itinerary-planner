package com.app.travel_planner.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class CreateDayRequest {

    @NotNull
    private Integer dayNumber;

    @NotNull
    private LocalDate date;
}
