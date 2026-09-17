package com.app.travel_planner.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReorderStopRequest {

    @NotNull
    private Integer newOrderIndex;
}
