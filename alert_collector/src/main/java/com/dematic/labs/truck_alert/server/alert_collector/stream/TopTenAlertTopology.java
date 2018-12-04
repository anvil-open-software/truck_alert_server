/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import com.dematic.labs.truck_alert.server.api.Alert;
import com.dematic.labs.truck_alert.server.api.AlertJsonSerde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.Consumed;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.Materialized;
import org.apache.kafka.streams.kstream.TimeWindows;
import org.apache.kafka.streams.processor.LogAndSkipOnInvalidTimestamp;
import org.apache.kafka.streams.state.KeyValueStore;
import org.apache.kafka.streams.state.WindowStore;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.time.temporal.ChronoUnit;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;
import static org.apache.kafka.streams.kstream.Serialized.with;
import static org.slf4j.LoggerFactory.getLogger;

public class TopTenAlertTopology {
    private static final Logger LOGGER = getLogger(TopTenAlertTopology.class);

    static final String MOST_RECENT_ALERTS_STORE = "most-recent-alerts";
    static final String MOST_RECENT_ALERTS_KEY = "all";
    static final String ACTIVE_TRUCKS_STORE = "active-trucks";
    static final String ACTIVE_TRUCKS_KEY = "all";

    @Nonnull
    public static Topology createTopology(@Nonnull final String topic, final int minimalDuration, @Nonnull final ChronoUnit minimalDurationUnit) {
        final StreamsBuilder builder = new StreamsBuilder();
        /* mapping alert to truck keeps the most recent alert for every truck */
        final KStream<String, Alert> alertByTruck = builder
                .stream(
                        topic,
                        Consumed
                                .with(Serdes.String(), new AlertJsonSerde())
                                .withTimestampExtractor(new LogAndSkipOnInvalidTimestamp()))
                .filter((key, alert) -> {
                    if (alert.getMeasurements().length == 0) {
                        LOGGER.warn("No measurement for {}", alert);
                        return false;
                    }
                    if (minimalDurationUnit.between(alert.getMin().getTimestamp(), alert.getMax().getTimestamp()) < minimalDuration) {
                        LOGGER.warn("Alert interval shorter than {} {} for {}", minimalDuration, minimalDurationUnit, alert);
                        return false;
                    }
                    return true;
                })
                .groupByKey()
                .aggregate(
                        () -> null,
                        (truckId, alert, aggregate) -> alert,
                        Materialized
                                .<String, Alert, KeyValueStore<Bytes, byte[]>>as("alert-by-truck")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(new AlertSerde())
                )
                .toStream();
        alertByTruck
                .groupBy((tuckId, alert) -> MOST_RECENT_ALERTS_KEY,
                        with(Serdes.String(), new AlertSerde()))
                .aggregate(
                        MostRecentAlerts::new,
                        (key, alert, aggregate) -> aggregate.add(alert),
                        Materialized
                                .<String, MostRecentAlerts, KeyValueStore<Bytes, byte[]>>as(MOST_RECENT_ALERTS_STORE)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(new MostRecentAlertsSerde())
                );
        alertByTruck
                .map((truckId, alert) -> KeyValue.pair(ACTIVE_TRUCKS_KEY, alert.getTruck()))
                .groupByKey(with(Serdes.String(), Serdes.String()))
                .windowedBy(TimeWindows
                        .of(ofHours(1).toMillis())
                        .until(ofHours(1).toMillis())
                        .advanceBy(ofMinutes(5).toMillis())
                )
                .aggregate(
                        ActiveTrucks::new,
                        (key, truckId, aggregate) -> aggregate.add(truckId),
                        Materialized.<String, ActiveTrucks, WindowStore<Bytes, byte[]>>as(ACTIVE_TRUCKS_STORE)
                                .withKeySerde(Serdes.String())
                                .withValueSerde(new ActiveTrucksSerde())

                );
        return builder.build();
    }
}
