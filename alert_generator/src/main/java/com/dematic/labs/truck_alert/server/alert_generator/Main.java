/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_generator;

import com.dematic.labs.truck_alert.server.api.Alert;
import com.dematic.labs.truck_alert.server.api.AlertJsonSerde;
import com.dematic.labs.truck_alert.server.api.MeasuredValue;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.lang.Float.parseFloat;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;
import static org.slf4j.LoggerFactory.getLogger;

public class Main {
    private static final Logger LOGGER = getLogger(Main.class);
    private static Random random = new Random();

    public static void main(@Nonnull final String[] args) throws InterruptedException, IOException {
        final Config config = ConfigFactory.load();
        final String topic = config.getString("topic-name");
        final TimeUnit timeUnit = config.getEnum(TimeUnit.class, "alert-generator.frequency-unit");
        final int generatorFrequency = config.getInt("alert-generator.frequency");

        final List<String[]> temperatureCurves = readTemperatureCurves();
        try (final Producer<String, Alert> producer = createProducer(config)) {
            //noinspection InfiniteLoopStatement
            for (; ; ) {
                if (random.nextFloat() >= 0.1) {
                    sendAlert(producer, temperatureCurves, topic);
                }
                timeUnit.sleep(generatorFrequency);
            }
        }
    }

    private static void sendAlert(@Nonnull final Producer<String, Alert> producer, @Nonnull final List<String[]> temperatureCurves, @Nonnull final String topic) {
        final String truckId = truckIdFor(random.nextInt(100));

        final String[] temperatureCurve = temperatureCurves.get(random.nextInt(temperatureCurves.size()));
        producer.send(new ProducerRecord<>(topic, truckId, createAlert(truckId, temperatureCurve)));
        producer.flush();
    }

    @Nonnull
    private static Producer<String, Alert> createProducer(@Nonnull final Config config) {
        final Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, config.getString("broker"));
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 0);
        props.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        props.put(ProducerConfig.LINGER_MS_CONFIG, 1);
        props.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, AlertJsonSerde.Serializer.class.getName());

        return new KafkaProducer<>(props);
    }

    @Nonnull
    static String truckIdFor(final int i) {
        return format("H2X31-%03d", i);
    }

    @Nonnull
    static Alert createAlert(@Nonnull final String truckId, @Nonnull final String[] temperatureCurve) {
        OffsetDateTime alertTime = OffsetDateTime.now();
        final MeasuredValue[] measurements = new MeasuredValue[temperatureCurve.length - 1];
        for (int i = temperatureCurve.length; i-- > 1; ) {
            measurements[i - 1] = new MeasuredValue(alertTime, parseFloat(temperatureCurve[i]));
            alertTime = alertTime.minusSeconds(1);
        }
        final Alert alert = new Alert(truckId, measurements[parseInt(temperatureCurve[0])], measurements[measurements.length - 1], measurements);
        LOGGER.debug("{}", alert);
        return alert;
    }

    @Nonnull
    static List<String[]> readTemperatureCurves() throws IOException {
        try (final InputStream resourceAsStream = Main.class.getResourceAsStream("/temperature_curves.csv");
             final BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(resourceAsStream, UTF_8));
             final Stream<String> stream = inputStreamReader.lines()) {
            return stream
                    .map(s -> s.split("\\s*,\\s*"))
                    .collect(toList());
        }
    }
}
