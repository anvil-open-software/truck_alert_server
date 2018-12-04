/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.api;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.util.Objects;

public class MeasuredValue {
    @Nonnull
    private final OffsetDateTime timestamp;
    private final float value;

    public MeasuredValue(@Nonnull final OffsetDateTime timestamp, final float value) {
        this.timestamp = timestamp;
        this.value = value;
    }

    @Override
    @Nonnull
    public String toString() {
        return "(" + timestamp + ", " + value + ')';
    }

    @Override
    public boolean equals(@Nullable final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MeasuredValue)) {
            return false;
        }
        final MeasuredValue that = (MeasuredValue) o;
        return Float.compare(that.value, value) == 0 &&
                Objects.equals(timestamp, that.timestamp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(timestamp, value);
    }

    @Nonnull
    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public float getValue() {
        return value;
    }
}
