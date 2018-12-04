/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import ch.qos.logback.classic.Level;
import com.dematic.labs.toolkit.helpers.test_util.SimpleLogAssertion;
import com.dematic.labs.truck_alert.server.alert_collector.ClockProviderRule;
import com.dematic.labs.truck_alert.server.api.Alert;
import com.dematic.labs.truck_alert.server.api.AlertCount;
import com.dematic.labs.truck_alert.server.api.AlertJsonSerde;
import com.dematic.labs.truck_alert.server.api.MeasuredValue;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.TopologyTestDriver;
import org.apache.kafka.streams.test.ConsumerRecordFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import static com.dematic.labs.truck_alert.server.alert_collector.stream.TopTenAlertRestService.getAlertCount;
import static com.dematic.labs.truck_alert.server.alert_collector.stream.TopTenAlertRestService.getMostRecentAlerts;
import static com.dematic.labs.truck_alert.server.alert_collector.stream.TopTenAlertTopology.ACTIVE_TRUCKS_STORE;
import static com.dematic.labs.truck_alert.server.alert_collector.stream.TopTenAlertTopology.MOST_RECENT_ALERTS_KEY;
import static com.dematic.labs.truck_alert.server.alert_collector.stream.TopTenAlertTopology.MOST_RECENT_ALERTS_STORE;
import static com.dematic.labs.truck_alert.server.api.AlertJsonSerdeTest.createAlert;
import static com.dematic.labs.truck_alert.server.api.AlertJsonSerdeTest.truckIdFor;
import static java.time.Duration.ofSeconds;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class TopTenAlertTopologyTest {
    // fix an instant in the past but in the windowing time frame
    // reset seconds to simplify assertions
    private static final OffsetDateTime START_INSTANT = OffsetDateTime.now().withSecond(0).withNano(0).minusMinutes(55);
    @Rule
    public final ClockProviderRule clockProviderRule = new ClockProviderRule().withInstant(START_INSTANT.plusHours(1));
    @Rule
    public final SimpleLogAssertion simpleLogAssertion = new SimpleLogAssertion().recordFor(TopTenAlertTopology.class).recordAt(Level.WARN);
    private TopologyTestDriver testDriver;
    private final ConsumerRecordFactory<String, Alert> recordFactory = new ConsumerRecordFactory<>("my-topic", new StringSerializer(), new AlertJsonSerde.Serializer());

    @Before
    public void setUpTestDriver() {
        final Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "test");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "dummy:1234");
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 500);
        testDriver = new TopologyTestDriver(TopTenAlertTopology.createTopology("my-topic", 6, SECONDS), config);
    }

    @After
    public void tearDownTestDriver() {
        testDriver.close();
    }

    @Test
    public void submitAlertWithoutMeasurements() {
        final Alert alert = new Alert("H2X3100",
                new MeasuredValue(START_INSTANT, 25.0f),
                new MeasuredValue(START_INSTANT.plus(ofSeconds(10)), 45.0f),
                new MeasuredValue[0]);

        testDriver.pipeInput(recordFactory.create(singletonList(withKey(alert))));

        assertEquals(0,
                getAlertCount(testDriver.getWindowStore(ACTIVE_TRUCKS_STORE)).getCount());
        assertNull(getMostRecentAlerts(testDriver.getKeyValueStore(MOST_RECENT_ALERTS_STORE)));
        simpleLogAssertion.assertThat(startsWith("No measurement for " + alert));
    }

    @Test
    public void submitAlertWitExcessivelyShortInterval() {
        final Alert alert = createAlert("H2X3100", START_INSTANT, 3);
        testDriver.pipeInput(recordFactory.create(singletonList(withKey(alert))));

        assertEquals(0,
                getAlertCount(testDriver.getWindowStore(ACTIVE_TRUCKS_STORE)).getCount());
        assertNull(getMostRecentAlerts(testDriver.getKeyValueStore(MOST_RECENT_ALERTS_STORE)));
        simpleLogAssertion.assertThat(startsWith("Alert interval shorter than 6 Seconds for " + alert));
    }

    @Test
    public void submitSameTruckTenTimes() {
        final List<KeyValue<String, Alert>> alerts = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            alerts.add(withKey(createAlert("H2X3100", START_INSTANT.plusMinutes(i * 5), 3600)));
        }

        testDriver.pipeInput(recordFactory.create(alerts));

        assertState(1, alerts.subList(9, 10));
    }

    @Test
    public void submitTwentyTrucks() {
        final List<KeyValue<String, Alert>> alerts = new ArrayList<>();
        for (int i = 0; i < 20; i++) {
            alerts.add(withKey(createAlert(truckIdFor(i), START_INSTANT, 3600)));
        }

        testDriver.pipeInput(recordFactory.create(alerts));

        assertState(20, alerts.subList(0, 10));
    }

    private void assertState(final int expectedAlertedTrucksCount, @Nonnull final List<KeyValue<String, Alert>> expectedAlerts) {
        final MostRecentAlerts alerts = getMostRecentAlerts(testDriver.getKeyValueStore(MOST_RECENT_ALERTS_STORE));
        assertNotNull(MOST_RECENT_ALERTS_KEY + " not initialized", alerts);
        assertEquals("top-ten recent alerts", expectedAlerts.size(), alerts.size());
        int i = 0;
        for (final Alert alert : alerts) {
            assertEquals("#" + i, expectedAlerts.get(i++).value, alert);
        }

        final AlertCount alertsCount = getAlertCount(testDriver.getWindowStore(ACTIVE_TRUCKS_STORE));
        assertEquals("count of truck with an alert",
                expectedAlertedTrucksCount,
                alertsCount.getCount());
    }

    @Nonnull
    private static KeyValue<String, Alert> withKey(@Nonnull final Alert alert) {
        return KeyValue.pair(alert.getTruck(), alert);
    }
}
