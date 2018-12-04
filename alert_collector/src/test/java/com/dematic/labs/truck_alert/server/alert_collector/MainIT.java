/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector;

import com.dematic.labs.truck_alert.server.alert_collector.stream.TopTenAlertRestService;
import com.dematic.labs.truck_alert.server.api.Alert;
import com.dematic.labs.truck_alert.server.api.AlertCount;
import com.dematic.labs.truck_alert.server.api.AlertJsonSerde;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.dematic.labs.toolkit.helpers.test_util.DockerHelper.getBaseUrl;
import static com.dematic.labs.toolkit.helpers.test_util.DockerHelper.getPort;
import static com.dematic.labs.truck_alert.server.alert_collector.ClockProvider.getClock;
import static com.dematic.labs.truck_alert.server.api.AlertJsonSerdeTest.createAlert;
import static com.dematic.labs.truck_alert.server.api.AlertJsonSerdeTest.truckIdFor;
import static com.dematic.labs.truck_alert.server.api.ObjectMapperFactory.getObjectMapper;
import static java.time.OffsetDateTime.now;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

public class MainIT {
    private static final OffsetDateTime START_INSTANT = now().withMinute(0).withSecond(0).withNano(0);
    @Rule
    public final ClockProviderRule clockProviderRule = new ClockProviderRule();
    @Before
    public void presetTime() {
        clockProviderRule.setInstant(START_INSTANT);
    }

    @Test
    public void sendOneTruckFiveBadThenOneGoodAlerts() {
        assertCount(0, getAlertsCount());
        produce(1, 5, CorruptingSerializer.class);
        final List<Alert> alerts = produceAndConsume(1, 1);
        assertEquals(1, alerts.size());
        assertAlert(truckIdFor(0), START_INSTANT.plusMinutes(5), alerts.get(0));
        assertCount(1, getAlertsCount());
    }

    @Nonnull
    private List<Alert> produceAndConsume(final int fleetSize, final int alertsPerTruck) {
        produce(fleetSize, alertsPerTruck, AlertJsonSerde.Serializer.class);

        for (int i = 0; i < 10; i++) {
            final List<Alert> alerts = getAlerts();
            if (alerts.size() == Math.min(fleetSize, 10)) {
                return alerts;
            }
            try {
                SECONDS.sleep(15);
            } catch (InterruptedException e) {
                ExceptionSoftener.softenException(e);
            }
        }
        return Collections.emptyList();
    }

    private void produce(final int fleetSize, final int alertPerTruck, @Nonnull final Class<? extends Serializer<Alert>> alertSerializerClass) {
        final Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:" + getPort("kafka.port"));
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, alertSerializerClass.getName());

        try (final Producer<String, Alert> producer = new KafkaProducer<>(props)) {
            for (int i = 0; i < alertPerTruck; i++) {
                final OffsetDateTime now = now(getClock());
                for (int truck = 0; truck < fleetSize; truck++) {
                    final String truckId = truckIdFor(truck);
                    producer.send(
                            new ProducerRecord<>("my-topic",
                                    truckId,
                                    createAlert(truckId, now, 10)
                            )
                    );
                }
                clockProviderRule.setInstant(now.plusMinutes(1));
            }
        }
    }

    @Nonnull
    private List<Alert> getAlerts() {
        return ClientBuilder
                .newBuilder()
                .register(JacksonFeature.class).register(ObjectMapperProvider.class)
                .build()
                .target(getBaseUrl("server.port", "alerts/temperature/raise/top-ten"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(TopTenAlertRestService.ALERT_LIST);
    }

    @Nonnull
    private AlertCount getAlertsCount() {
        return ClientBuilder
                .newBuilder()
                .register(JacksonFeature.class).register(ObjectMapperProvider.class)
                .build()
                .target(getBaseUrl("server.port", "alerts/temperature/raise/count"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(AlertCount.class);
    }

    private static void assertAlert(@Nonnull final String expectedTruckId, final @Nonnull OffsetDateTime expectMinTimestamp, final @Nonnull Alert alert) {
        assertEquals(expectedTruckId, alert.getTruck());
        assertEquals(expectMinTimestamp, alert.getMin().getTimestamp());
    }

    private static void assertCount(final long expectedTruckInAlert, @Nonnull final AlertCount alertCount) {
        assertEquals(expectedTruckInAlert, alertCount.getCount());
    }

    public static class CorruptingSerializer implements Serializer<Alert> {

        @Override
        public void configure(final Map<String, ?> map, final boolean b) {
        }

        @Override
        @Nullable
        public byte[] serialize(final String topic, @Nullable final Alert alert) {
            if (alert == null) {
                return null;
            }

            try {
                final ObjectMapper objectMapper = getObjectMapper();
                final JsonNode jsonNodes = objectMapper.valueToTree(alert);
                final ObjectNode min = (ObjectNode) jsonNodes.get("min");
                min.put("timestamp", min.get("timestamp").asText().replace('T', ' '));
                return objectMapper.writeValueAsBytes(jsonNodes);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {

        }
    }
}
