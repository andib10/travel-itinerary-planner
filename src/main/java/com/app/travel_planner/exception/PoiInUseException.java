package com.app.travel_planner.exception;

/**
 * Thrown when deleting a POI still referenced by a saved Stop; blocks rather than cascades.
 */
public class PoiInUseException extends RuntimeException {

    public PoiInUseException(Long poiId, long stopCount) {
        super("Cannot delete POI " + poiId + ": referenced by " + stopCount + " existing stop(s)");
    }
}
