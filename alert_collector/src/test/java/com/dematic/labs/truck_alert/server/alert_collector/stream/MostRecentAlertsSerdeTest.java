/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import com.dematic.labs.truck_alert.server.api.Alert;
import org.junit.Test;

import java.util.Iterator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class MostRecentAlertsSerdeTest {

    @Test
    public void roundtripNullValue() {
        final MostRecentAlertsSerde serde = new MostRecentAlertsSerde();
        final byte[] serialize = serde.serializer().serialize("foo", null);
        final MostRecentAlerts deserialized = serde.deserializer().deserialize("foo", serialize);
        assertNull(deserialized);
    }

    @Test
    public void roundtrip() {
        final MostRecentAlerts alerts = MostRecentAlertsTest.fillMostRecentAlerts(5, 5);
        final MostRecentAlertsSerde serde = new MostRecentAlertsSerde();
        final byte[] serialize = serde.serializer().serialize("foo", alerts);
        final MostRecentAlerts deserialized = serde.deserializer().deserialize("foo", serialize);
        assertEquals(alerts.getMaxAlertCount(), deserialized.getMaxAlertCount());
        assertEquals(alerts.size(), deserialized.size());
        final Iterator<Alert> iterator = deserialized.iterator();
        for (Alert alert : alerts) {
            assertTrue(iterator.hasNext());
            assertEquals(alert, iterator.next());
        }
    }
}
