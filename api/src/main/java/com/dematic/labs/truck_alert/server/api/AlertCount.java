/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AlertCount {
    private final long count;

    // JsonProperty required for single-argument constructor only
    public AlertCount(@JsonProperty("count") final long count) {
        this.count = count;
    }

    @Override
    public String toString() {
        return "AlertCount{ " + count + '}';
    }

    public long getCount() {
        return count;
    }
}
