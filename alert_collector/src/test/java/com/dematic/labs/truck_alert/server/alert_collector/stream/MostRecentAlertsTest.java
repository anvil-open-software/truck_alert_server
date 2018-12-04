/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import com.dematic.labs.truck_alert.server.alert_collector.ClockProviderRule;
import org.junit.Rule;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.util.stream.Collectors;

import static com.dematic.labs.truck_alert.server.api.AlertJsonSerdeTest.createAlert;
import static com.dematic.labs.truck_alert.server.api.AlertJsonSerdeTest.truckIdFor;
import static java.time.OffsetDateTime.parse;
import static java.util.stream.StreamSupport.stream;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.iterableWithSize;
import static org.junit.Assert.assertThat;

public class MostRecentAlertsTest {

    private static final OffsetDateTime START_INSTANT = parse("2018-04-02T11:32:00.000-07:00");

    @Rule
    public final ClockProviderRule clockProviderRule = new ClockProviderRule();

    @Test
    public void oneTruckTenAlerts() {
        clockProviderRule.setInstant(START_INSTANT.plusHours(1));

        final MostRecentAlerts mostRecentAlerts = new MostRecentAlerts(5);
        for (int i = 0; i < 10; i++) {
            mostRecentAlerts.add(createAlert(truckIdFor(0), START_INSTANT.plusMinutes(i * 5), 3600));
        }
        assertThat(mostRecentAlerts, iterableWithSize(1));
        assertThat(stream(mostRecentAlerts.spliterator(), false).map(a -> a.getMin().getTimestamp()).collect(Collectors.toList()),
                contains(START_INSTANT.plusMinutes(9 * 5)));
    }

    @Test
    public void tenTrucksOneAlert() {
        clockProviderRule.setInstant(START_INSTANT.plusHours(1));

        final MostRecentAlerts mostRecentAlerts = new MostRecentAlerts(5);
        for (int i = 0; i < 10; i++) {
            mostRecentAlerts.add(createAlert(truckIdFor(i), START_INSTANT, 3600));
        }
        assertThat(mostRecentAlerts, iterableWithSize(5));
        assertThat(stream(mostRecentAlerts.spliterator(), false).map(a -> a.getTruck()).collect(Collectors.toList()),
                contains("H2X31-00", "H2X31-01", "H2X31-02", "H2X31-03", "H2X31-04"));
    }

    @Nonnull
    static MostRecentAlerts fillMostRecentAlerts(final int maxAlertCount, final int truckCount) {
        final MostRecentAlerts mostRecentAlerts = new MostRecentAlerts(maxAlertCount);
        for (int i = 0; i < truckCount; i++) {
            mostRecentAlerts.add(createAlert(truckIdFor(i), START_INSTANT.plusMinutes(i * 5), 3600));
        }
        return mostRecentAlerts;
    }
}
