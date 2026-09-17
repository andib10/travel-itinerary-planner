package com.app.travel_planner.optimization;

public enum ConflictType {
    /** Not enough time to walk from the end of one stop to the start of the next. */
    TRAVEL_TIME,
    /** A stop's scheduled start/end time falls outside its category's default opening-hours window. */
    OPENING_HOURS
}
