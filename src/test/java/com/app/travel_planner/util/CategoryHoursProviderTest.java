package com.app.travel_planner.util;

import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies CategoryHoursProvider's hour windows, including the midnight-spanning case.
 */
class CategoryHoursProviderTest {

    @Test
    void bar_withinEveningHours_isOpen() { // bars: 17:00-02:00
        assertThat(CategoryHoursProvider.isWithinHours("bars", LocalTime.of(23, 0))).isTrue();
    }

    @Test
    void bar_withinEarlyMorningHours_isOpen() {
        assertThat(CategoryHoursProvider.isWithinHours("bars", LocalTime.of(1, 0))).isTrue();
    }

    @Test
    void bar_duringDaytime_isClosed() {
        assertThat(CategoryHoursProvider.isWithinHours("bars", LocalTime.of(10, 0))).isFalse();
    }

    @Test
    void bar_atOpenBoundary_isOpen() {
        assertThat(CategoryHoursProvider.isWithinHours("bars", LocalTime.of(17, 0))).isTrue();
    }

    @Test
    void bar_atCloseBoundary_isOpen() {
        assertThat(CategoryHoursProvider.isWithinHours("bars", LocalTime.of(2, 0))).isTrue();
    }

    @Test
    void bar_justAfterCloseBoundary_isClosed() {
        assertThat(CategoryHoursProvider.isWithinHours("bars", LocalTime.of(2, 1))).isFalse();
    }

    @Test
    void bar_justBeforeOpenBoundary_isClosed() {
        assertThat(CategoryHoursProvider.isWithinHours("bars", LocalTime.of(16, 59))).isFalse();
    }

    @Test
    void pubs_alsoSpanMidnight_earlyMorningIsOpen() { // pubs: 17:00-01:00
        assertThat(CategoryHoursProvider.isWithinHours("pubs", LocalTime.of(0, 30))).isTrue();
        assertThat(CategoryHoursProvider.isWithinHours("pubs", LocalTime.of(1, 30))).isFalse();
    }

    @Test
    void museum_normalNonSpanningWindow_stillWorks() { // museums: 09:00-18:00
        assertThat(CategoryHoursProvider.isWithinHours("museums", LocalTime.of(10, 0))).isTrue();
        assertThat(CategoryHoursProvider.isWithinHours("museums", LocalTime.of(20, 30))).isFalse();
    }

    @Test
    void unmappedCategory_fallsBackToOther() { // "other": 08:00-21:00
        assertThat(CategoryHoursProvider.isWithinHours("monuments_and_memorials", LocalTime.of(9, 0))).isTrue();
        assertThat(CategoryHoursProvider.isWithinHours("monuments_and_memorials", LocalTime.of(22, 0))).isFalse();
    }

    @Test
    void nullCategory_fallsBackToOther() {
        assertThat(CategoryHoursProvider.isWithinHours(null, LocalTime.of(9, 0))).isTrue();
    }
}
