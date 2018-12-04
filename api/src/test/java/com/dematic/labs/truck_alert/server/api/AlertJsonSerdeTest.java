/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.api;

import org.junit.Test;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;

import static java.lang.String.format;
import static java.time.Duration.ofSeconds;
import static java.time.OffsetDateTime.parse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.slf4j.LoggerFactory.getLogger;

public class AlertJsonSerdeTest {
    private static final Logger LOGGER = getLogger(AlertJsonSerdeTest.class);


    @Test
    public void roundtripNullValue() {
        final AlertJsonSerde serde = new AlertJsonSerde();
        final byte[] serialize = serde.serializer().serialize("foo", null);
        final Alert deserialized = serde.deserializer().deserialize("foo", serialize);
        assertNull(deserialized);
    }

    @Test
    public void roundtrip() {
        final Alert alert = createAlert("H2X31-00", parse("2018-04-02T11:32:00.000-07:00"), 3600);
        final AlertJsonSerde serde = new AlertJsonSerde();
        final byte[] serialize = serde.serializer().serialize("foo", alert);
        final Alert deserialized = serde.deserializer().deserialize("foo", serialize);
        assertEquals(alert, deserialized);
    }

    @Nonnull
    public static Alert createAlert(@Nonnull final String truckId, @Nonnull final OffsetDateTime startTime, final int measurementCounts) {
        final MeasuredValue[] measurements = new MeasuredValue[measurementCounts];
        for (int i = 0; i < measurements.length; i++) {
            measurements[i] = new MeasuredValue(startTime.plus(ofSeconds(i)), 25.0f + (10.0f / measurementCounts * i));
        }
        final Alert alert = new Alert(truckId, measurements[0], measurements[measurements.length - 1], measurements);
        LOGGER.info(alert.toString());
        return alert;
    }

    /**
     * insert enough leading 0 that truck id will sort properly as string.
     */
    @Nonnull
    public static String truckIdFor(final int i) {
        return format("H2X31-%02d", i);
    }
}
