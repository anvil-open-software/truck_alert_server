/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_generator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.Ignore;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import static com.dematic.labs.truck_alert.server.api.ObjectMapperFactory.getObjectMapper;
import static java.lang.Float.parseFloat;
import static java.lang.String.join;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.nio.file.Files.write;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThat;

/**
 * Utility to convert a trove of alerts into a simple files of temperatures.
 * The file will contain one temperature profile per line and assume one temperature every second.
 * Temperatures are recorded as floating number.
 * The first entry in the line is the option-base-zero index of the minimum value for the alert.
 * The maximum value for the alert is always the last recorded temperature.
 * <p>Example:</p>
 * <pre>1, 23.0, 23.0, 32.0, 33.5</pre>
 * The minimum, index 1, is the second 23.0, and the maximum, which is the alert trigger, is 33.5
 */
public class DataSimplifier {
    @Ignore
    @Test
    public void simplify() throws IOException {
        final List<String> result = new ArrayList<>();
        for (final JsonNode alertNode : getObjectMapper().readTree(new File("alertsv4.json"))) {
            assertThat("Top node is not an object... is the input file valid json?", alertNode, instanceOf(ObjectNode.class));
            final String minTimestamp = alertNode.get("min").get("_timestamp").asText();
            int minIndex = -1;
            final List<String> values = new ArrayList<>();
            final JsonNode measurements = alertNode.get("measurements");
            for (JsonNode measurementNode : measurements) {
                values.add(measurementNode.get("value").asText());
                if (minTimestamp.equals(measurementNode.get("_timestamp").asText())) {
                    minIndex = values.size();
                }
            }
            assertNotEquals(-1, minIndex);
            int absoluteMinimum = locateMinimum(values.subList(0, minIndex));
            result.add((minIndex - absoluteMinimum) + ", " + join(", ", values.subList(absoluteMinimum, values.size())));
        }
        write(Paths.get("src/main/resources/temperature_curves.csv"),
                join("\n", result).getBytes(UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    private int locateMinimum(@Nonnull final List<String> values) {
        int minIndex = 0;
        float minValue = parseFloat(values.get(minIndex));
        for (int i = 0; i < values.size(); i++) {
            final float value = parseFloat(values.get(i));
            if (value < minValue) {
                minIndex = i;
            }
        }
        return minIndex;
    }
}
