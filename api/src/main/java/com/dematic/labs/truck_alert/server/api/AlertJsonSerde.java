/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.common.serialization.Serde;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Map;

import static com.dematic.labs.truck_alert.server.api.ObjectMapperFactory.getObjectMapper;

public class AlertJsonSerde implements Serde<Alert> {
    final private Serializer serializer = new Serializer();
    final private Deserializer deserializer = new Deserializer();

    @Override
    public void configure(final Map<String, ?> map, final boolean b) {
        serializer.configure(map, b);
        deserializer.configure(map, b);
    }

    @Override
    public void close() {
        serializer.close();
        deserializer.close();
    }

    @Override
    @Nonnull
    public Serializer serializer() {
        return serializer;
    }

    @Override
    @Nonnull
    public Deserializer deserializer() {
        return deserializer;
    }

    public static class Serializer implements org.apache.kafka.common.serialization.Serializer<Alert> {
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
                return getObjectMapper().writeValueAsBytes(alert);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {

        }
    }

    public static class Deserializer implements org.apache.kafka.common.serialization.Deserializer<Alert> {
        @Override
        public void configure(final Map<String, ?> map, final boolean b) {

        }

        @Override
        @Nullable
        public Alert deserialize(final String topic, @Nullable final byte[] bytes) {
            if (bytes == null || bytes.length == 0) {
                return null;
            }
            try {
                return getObjectMapper().readValue(bytes, Alert.class);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {

        }
    }
}
