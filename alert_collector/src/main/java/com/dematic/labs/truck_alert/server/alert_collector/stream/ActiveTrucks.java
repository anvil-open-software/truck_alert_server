/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.util.Collections.unmodifiableSortedSet;
import static org.slf4j.LoggerFactory.getLogger;

public class ActiveTrucks implements Iterable<String> {
    private static final Logger LOGGER = getLogger(ActiveTrucks.class);
    @Nonnull
    private final SortedSet<String> activeTrucks = new TreeSet<>();

    @Override
    @Nonnull
    public String toString() {
        return "ActiveTrucks (" + activeTrucks.size() + ") " + activeTrucks;
    }

    @Override
    @Nonnull
    public Iterator<String> iterator() {
        return unmodifiableSortedSet(activeTrucks).iterator();
    }

    @Nonnull
    ActiveTrucks add(@Nonnull final String truckId) {
        LOGGER.debug("Adding {}", truckId);
        activeTrucks.add(truckId);
        return this;
    }

    public int size() {
        return activeTrucks.size();
    }
}
