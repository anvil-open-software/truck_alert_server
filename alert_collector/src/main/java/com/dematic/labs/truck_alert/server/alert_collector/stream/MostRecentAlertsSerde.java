/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import com.dematic.labs.truck_alert.server.api.Alert;
import org.apache.kafka.common.serialization.Serde;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class MostRecentAlertsSerde implements Serde<MostRecentAlerts> {
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

    public static class Serializer implements org.apache.kafka.common.serialization.Serializer<MostRecentAlerts> {
        @Override
        public void configure(final Map<String, ?> map, final boolean b) {
        }

        @Override
        @Nullable
        public byte[] serialize(final String topic, @Nullable final MostRecentAlerts mostRecentAlerts) {
            if (mostRecentAlerts == null) {
                return null;
            }
            try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
                 final DataOutputStream dataOutputStream = new DataOutputStream(out)) {
                dataOutputStream.writeInt(mostRecentAlerts.getMaxAlertCount());
                dataOutputStream.writeInt(mostRecentAlerts.size());
                final AlertSerde.Serializer serializer = new AlertSerde().serializer();
                for (final Alert alert : mostRecentAlerts) {
                    serializer.serialize(dataOutputStream, alert);
                }
                dataOutputStream.flush();
                return out.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {

        }
    }

    public static class Deserializer implements org.apache.kafka.common.serialization.Deserializer<MostRecentAlerts> {
        @Override
        public void configure(final Map<String, ?> map, final boolean b) {

        }

        @Override
        @Nullable
        public MostRecentAlerts deserialize(final String topic, @Nullable final byte[] bytes) {
            if (bytes == null || bytes.length == 0) {
                return null;
            }

            try (final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes))) {
                final MostRecentAlerts mostRecentAlerts = new MostRecentAlerts(dataInputStream.readInt());
                final AlertSerde.Deserializer deserializer = new AlertSerde().deserializer();
                for (int i = dataInputStream.readInt(); i > 0; i--) {
                    mostRecentAlerts.add(deserializer.deserialize(dataInputStream));
                }
                return mostRecentAlerts;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {

        }
    }
}
