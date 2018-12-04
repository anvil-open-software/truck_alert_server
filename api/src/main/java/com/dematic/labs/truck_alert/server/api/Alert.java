/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Objects;

import static java.util.Arrays.copyOf;

public class Alert {
    @Nonnull
    private final String truck;
    @Nonnull
    private final MeasuredValue min;
    @Nonnull
    private final MeasuredValue max;
    @Nonnull
    private final MeasuredValue[] measurements;

    public Alert(@Nonnull final String truck, @Nonnull final MeasuredValue min, @Nonnull final MeasuredValue max, @Nonnull final MeasuredValue[] measurements) {
        this.truck = truck;
        this.min = min;
        this.max = max;
        this.measurements = copyOf(measurements, measurements.length);
    }

    @Override
    @Nonnull
    public String toString() {
        return "Alert{ " + truck + " [" + min + ", " + max + "], " + measurements.length + "}";
    }

    @Override
    public boolean equals(final @Nullable Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Alert)) {
            return false;
        }
        final Alert alert = (Alert) o;
        return Objects.equals(truck, alert.truck) &&
                Objects.equals(min, alert.min) &&
                Objects.equals(max, alert.max) &&
                Arrays.equals(measurements, alert.measurements);
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(truck, min, max);
        result = 31 * result + Arrays.hashCode(measurements);
        return result;
    }

    @Nonnull
    public String getTruck() {
        return truck;
    }

    @Nonnull
    public MeasuredValue getMin() {
        return min;
    }

    @Nonnull
    public MeasuredValue getMax() {
        return max;
    }

    @Nonnull
    public MeasuredValue[] getMeasurements() {
        return copyOf(measurements, measurements.length);
    }
}
