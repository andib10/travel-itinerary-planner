package com.app.travel_planner.optimization;

import com.app.travel_planner.entity.PointOfInterest;
import com.app.travel_planner.entity.Stop;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies the nearest-neighbor pass fixes an inefficient order, keeps the first stop
 * fixed, and leaves times untouched.
 */
class RouteSequencerTest {

    private static PointOfInterest poi(String name, double lat, double lng) {
        PointOfInterest poi = new PointOfInterest();
        poi.setName(name);
        poi.setCategory("other");
        poi.setLat(lat);
        poi.setLng(lng);
        return poi;
    }

    private static Stop stop(PointOfInterest poi, int orderIndex, LocalTime start, LocalTime end) {
        Stop stop = new Stop();
        stop.setPointOfInterest(poi);
        stop.setOrderIndex(orderIndex);
        stop.setStartTime(start);
        stop.setEndTime(end);
        return stop;
    }

    @Test
    void inefficientOrder_isCorrectedToNearestNeighborPath() {
        // stop1/stop3 ~100m apart, stop2 ~55km away. Nearest-neighbor should visit
        // stop3 before stop2.
        PointOfInterest poi1 = poi("Start", 45.0, 10.0);
        PointOfInterest poi2 = poi("Far Away", 45.5, 10.0);
        PointOfInterest poi3 = poi("Close To Start", 45.001, 10.0);

        Stop stop1 = stop(poi1, 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Stop stop2 = stop(poi2, 2, LocalTime.of(10, 30), LocalTime.of(11, 30));
        Stop stop3 = stop(poi3, 3, LocalTime.of(12, 0), LocalTime.of(13, 0));

        List<Stop> resequenced = RouteSequencer.resequence(List.of(stop1, stop2, stop3));

        assertThat(resequenced).containsExactly(stop1, stop3, stop2);
        assertThat(stop1.getOrderIndex()).isEqualTo(1);
        assertThat(stop3.getOrderIndex()).isEqualTo(2);
        assertThat(stop2.getOrderIndex()).isEqualTo(3);
    }

    @Test
    void resequencing_doesNotTouchStartOrEndTimes() {
        PointOfInterest poi1 = poi("Start", 45.0, 10.0);
        PointOfInterest poi2 = poi("Far Away", 45.5, 10.0);
        PointOfInterest poi3 = poi("Close To Start", 45.001, 10.0);

        Stop stop1 = stop(poi1, 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Stop stop2 = stop(poi2, 2, LocalTime.of(10, 30), LocalTime.of(11, 30));
        Stop stop3 = stop(poi3, 3, LocalTime.of(12, 0), LocalTime.of(13, 0));

        RouteSequencer.resequence(List.of(stop1, stop2, stop3));

        assertThat(stop1.getStartTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(stop1.getEndTime()).isEqualTo(LocalTime.of(10, 0));
        assertThat(stop2.getStartTime()).isEqualTo(LocalTime.of(10, 30));
        assertThat(stop2.getEndTime()).isEqualTo(LocalTime.of(11, 30));
        assertThat(stop3.getStartTime()).isEqualTo(LocalTime.of(12, 0));
        assertThat(stop3.getEndTime()).isEqualTo(LocalTime.of(13, 0));
    }

    @Test
    void alreadyEfficientOrder_isUnchanged() {
        PointOfInterest poi1 = poi("A", 45.0, 10.0);
        PointOfInterest poi2 = poi("B", 45.001, 10.0);
        PointOfInterest poi3 = poi("C", 45.002, 10.0);

        Stop stop1 = stop(poi1, 1, LocalTime.of(9, 0), LocalTime.of(10, 0));
        Stop stop2 = stop(poi2, 2, LocalTime.of(10, 15), LocalTime.of(11, 0));
        Stop stop3 = stop(poi3, 3, LocalTime.of(11, 15), LocalTime.of(12, 0));

        List<Stop> resequenced = RouteSequencer.resequence(List.of(stop1, stop2, stop3));

        assertThat(resequenced).containsExactly(stop1, stop2, stop3);
    }

    @Test
    void singleStopDay_isUnchanged() {
        Stop onlyStop = stop(poi("Solo", 45.0, 10.0), 1, LocalTime.of(9, 0), LocalTime.of(10, 0));

        List<Stop> resequenced = RouteSequencer.resequence(List.of(onlyStop));

        assertThat(resequenced).containsExactly(onlyStop);
        assertThat(onlyStop.getOrderIndex()).isEqualTo(1);
    }

    @Test
    void fourStops_collinearWithOneBacktrack_resolvesToMonotonicPath() {
        // Input order 0 -> 0.01 -> 0.03 -> 0.02 backtracks; should resolve to 0, 0.01, 0.02, 0.03.
        PointOfInterest start = poi("Start", 45.0, 10.0);
        PointOfInterest near = poi("Near", 45.01, 10.0);
        PointOfInterest far = poi("Far", 45.03, 10.0);
        PointOfInterest mid = poi("Mid", 45.02, 10.0);

        Stop stopStart = stop(start, 1, LocalTime.of(9, 0), LocalTime.of(9, 30));
        Stop stopNear = stop(near, 2, LocalTime.of(9, 45), LocalTime.of(10, 15));
        Stop stopFar = stop(far, 3, LocalTime.of(10, 30), LocalTime.of(11, 0));
        Stop stopMid = stop(mid, 4, LocalTime.of(11, 15), LocalTime.of(11, 45));

        List<Stop> resequenced = RouteSequencer.resequence(List.of(stopStart, stopNear, stopFar, stopMid));

        assertThat(resequenced).containsExactly(stopStart, stopNear, stopMid, stopFar);
        assertThat(stopStart.getOrderIndex()).isEqualTo(1);
        assertThat(stopNear.getOrderIndex()).isEqualTo(2);
        assertThat(stopMid.getOrderIndex()).isEqualTo(3);
        assertThat(stopFar.getOrderIndex()).isEqualTo(4);
    }
}
