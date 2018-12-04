/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import org.junit.Test;

import java.util.Iterator;

import static com.dematic.labs.truck_alert.server.api.AlertJsonSerdeTest.truckIdFor;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ActiveTrucksSerdeTest {

    @Test
    public void roundtripNullValue() {
        final ActiveTrucksSerde serde = new ActiveTrucksSerde();
        final byte[] serialize = serde.serializer().serialize("foo", null);
        final ActiveTrucks deserialized = serde.deserializer().deserialize("foo", serialize);
        assertNull(deserialized);
    }

    @Test
    public void roundtrip() {
        final ActiveTrucks activeTrucks = new ActiveTrucks();
        activeTrucks.add(truckIdFor(0));
        activeTrucks.add(truckIdFor(2));
        activeTrucks.add(truckIdFor(4));
        final ActiveTrucksSerde serde = new ActiveTrucksSerde();
        final byte[] serialize = serde.serializer().serialize("foo", activeTrucks);
        final ActiveTrucks deserialized = serde.deserializer().deserialize("foo", serialize);
        assertEquals(activeTrucks.size(), deserialized.size());
        final Iterator<String> iterator = deserialized.iterator();
        for (String truckId : activeTrucks) {
            assertTrue(iterator.hasNext());
            assertEquals(truckId, iterator.next());
        }
    }
}
