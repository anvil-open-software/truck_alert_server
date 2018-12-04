/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import com.dematic.labs.truck_alert.server.alert_collector.ClockProvider;
import com.dematic.labs.truck_alert.server.api.Alert;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import static java.time.OffsetDateTime.now;
import static java.util.Collections.unmodifiableSortedSet;
import static org.slf4j.LoggerFactory.getLogger;

public class MostRecentAlerts implements Iterable<Alert> {
    private static final Logger LOGGER = getLogger(MostRecentAlerts.class);
    private static final Comparator<Alert> ALERT_COMPARATOR = Comparator
            .comparing((Alert alert) -> alert.getMax().getTimestamp())
            .reversed()
            .thenComparing(Alert::getTruck);
    private final int maxAlertCount;
    @Nonnull
    private final SortedSet<Alert> alerts = new TreeSet<>(
            ALERT_COMPARATOR
    );
    @Nonnull
    private final Map<String, Alert> alertByTruck = new HashMap<>();

    public MostRecentAlerts() {
        this(10);
    }

    public MostRecentAlerts(final int maxAlertCount) {
        this.maxAlertCount = maxAlertCount;
    }

    @Override
    @Nonnull
    public String toString() {
        return "MostRecentAlerts" + alerts;
    }

    @Override
    @Nonnull
    public Iterator<Alert> iterator() {
        final OffsetDateTime now = windowStartInstant();
        return unmodifiableSortedSet(alerts).stream().filter(a -> a.getMax().getTimestamp().isAfter(now)).iterator();
    }

    @Nonnull
    private OffsetDateTime windowStartInstant() {
        return now(ClockProvider.getClock()).minusHours(1);
    }

    @Nonnull
    MostRecentAlerts add(@Nonnull final Alert alert) {
        LOGGER.debug("Adding {}", alert);
        final OffsetDateTime windowStartInstant = windowStartInstant();

        if (alert.getMax().getTimestamp().isBefore(windowStartInstant)) {
            LOGGER.debug("... too old, omitted");
            return this;
        }

        alertByTruck.computeIfPresent(alert.getTruck(), (id, a) -> {
            LOGGER.debug("... remove previous alert {}", a);
            alerts.remove(a);
            return null;
        });

        for (; !alerts.isEmpty(); ) {
            final Alert last = alerts.last();
            if (!alert.getMax().getTimestamp().isBefore(windowStartInstant)) {
                break;
            }
            remove(last);
        }

        alerts.add(alert);
        alertByTruck.put(alert.getTruck(), alert);

        if (alerts.size() > maxAlertCount) {
            remove(alerts.last());
        }
        LOGGER.debug("Current MostRecentAlerts {}", alerts);
        return this;
    }

    @Nonnull
    private void remove(@Nonnull final Alert alert) {
        LOGGER.debug("Removing {}", alert);
        if (alertByTruck.remove(alert.getTruck(), alert)) {
            alerts.remove(alert);
        } else {
            LOGGER.debug("... was not present in map");
        }
    }

    public int getMaxAlertCount() {
        return maxAlertCount;
    }

    public int size() {
        return alerts.size();
    }
}
