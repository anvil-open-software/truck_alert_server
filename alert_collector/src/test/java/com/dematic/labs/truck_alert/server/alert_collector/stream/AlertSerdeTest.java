/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import com.dematic.labs.truck_alert.server.api.Alert;
import org.junit.Test;

import static com.dematic.labs.truck_alert.server.api.AlertJsonSerdeTest.createAlert;
import static java.time.OffsetDateTime.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class AlertSerdeTest {

    @Test
    public void roundtripNullValue() {
        final AlertSerde serde = new AlertSerde();
        final byte[] serialize = serde.serializer().serialize("foo", null);
        final Alert deserialized = serde.deserializer().deserialize("foo", serialize);
        assertNull(deserialized);
    }

    @Test
    public void roundtrip() {
        final Alert alert = createAlert("H2X31-00", parse("2018-04-02T11:32:00.000-07:00"), 3600);
        final AlertSerde serde = new AlertSerde();
        final byte[] serialize = serde.serializer().serialize("foo", alert);
        final Alert deserialized = serde.deserializer().deserialize("foo", serialize);
        assertEquals(alert, deserialized);
    }

}
