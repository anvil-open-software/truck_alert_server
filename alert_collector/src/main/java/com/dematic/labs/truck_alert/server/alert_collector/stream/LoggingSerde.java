/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import java.util.Map;

import static org.slf4j.LoggerFactory.getLogger;

public class LoggingSerde<T> implements Serde<T> {
    final private Serializer<T> serializer;
    final private Deserializer<T> deserializer;

    public LoggingSerde(@Nonnull final String qualifier, @Nonnull final Serde<T> serde) {
        serializer = new LoggingSerializer<>(qualifier, serde.serializer());
        deserializer = new LoggingDeserializer<>(qualifier, serde.deserializer());
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
        serializer.configure(configs, isKey);
        deserializer.configure(configs, isKey);
    }

    @Override
    public void close() {
        serializer.close();
        deserializer.close();
    }

    @Override
    @Nonnull
    public Serializer<T> serializer() {
        return serializer;
    }

    @Override
    @Nonnull
    public Deserializer<T> deserializer() {
        return deserializer;
    }

    public static class LoggingSerializer<T> implements Serializer<T> {
        private static final Logger LOGGER = getLogger(LoggingSerializer.class);
        @Nonnull
        private final String qualifier;
        @Nonnull
        private final Serializer<T> serializer;

        LoggingSerializer(@Nonnull final String qualifier, @Nonnull final Serializer<T> serializer) {
            this.qualifier = qualifier;
            this.serializer = serializer;
        }

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            serializer.configure(configs, isKey);
        }

        @Override
        public byte[] serialize(String topic, T data) {
            LOGGER.info("{} - {}", qualifier, data);
            return serializer.serialize(topic, data);
        }

        @Override
        public void close() {
            serializer.close();
        }
    }

    public static class LoggingDeserializer<T> implements Deserializer<T> {
        private static final Logger LOGGER = getLogger(LoggingDeserializer.class);
        @Nonnull
        private final String qualifier;
        @Nonnull
        private final Deserializer<T> deserializer;

        LoggingDeserializer(@Nonnull final String qualifier, @Nonnull final Deserializer<T> deserializer) {
            this.qualifier = qualifier;
            this.deserializer = deserializer;
        }

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {
            deserializer.configure(configs, isKey);
        }

        @Override
        public T deserialize(String topic, byte[] data) {
            final T result = deserializer.deserialize(topic, data);
            LOGGER.info("{} - {}", qualifier, result);
            return result;
        }

        @Override
        public void close() {
            deserializer.close();
        }
    }
}
