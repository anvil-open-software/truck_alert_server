/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_generator;

import com.dematic.labs.truck_alert.server.api.Alert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static java.lang.Integer.parseInt;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class MainTest {
    @Test
    public void readTemperatureCurves() throws IOException {
        final List<String[]> temperatureCurves = Main.readTemperatureCurves();
        assertEquals(7, temperatureCurves.size());
        temperatureCurves.forEach(s -> assertNotEquals(0, s.length));
    }

    @Test
    public void testTruckIdFor() {
        assertEquals("H2X31-001", Main.truckIdFor(1));
        assertEquals("H2X31-100", Main.truckIdFor(100));
    }

    @Test
    public void testCreateAlert() {
        final String[] temperatureCurve = {"1", "10.0", "11.0", "12.0", "13.0", "14.0", "15.0"};
        final Alert alert = Main.createAlert("H2X31-001", temperatureCurve);
        assertEquals(11.0, alert.getMin().getValue(), 0.0);
        assertEquals(15.0, alert.getMax().getValue(), 0.0);
        assertEquals(6, alert.getMeasurements().length);
        assertEquals(
                alert.getMin().getTimestamp(),
                alert.getMax().getTimestamp().minusSeconds(alert.getMeasurements().length - 1 - parseInt(temperatureCurve[0])));
    }
}
