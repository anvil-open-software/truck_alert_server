/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import org.apache.kafka.common.serialization.Serde;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Map;

public class ActiveTrucksSerde implements Serde<ActiveTrucks> {
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

    public static class Serializer implements org.apache.kafka.common.serialization.Serializer<ActiveTrucks> {
        @Override
        public void configure(final Map<String, ?> map, final boolean b) {
        }

        @Override
        @Nullable
        public byte[] serialize(final String topic, @Nullable final ActiveTrucks activeTrucks) {
            if (activeTrucks == null) {
                return null;
            }
            try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
                 final DataOutputStream dataOutputStream = new DataOutputStream(out)) {
                dataOutputStream.writeInt(activeTrucks.size());
                for (final String truckId : activeTrucks) {
                    dataOutputStream.writeUTF(truckId);
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

    public static class Deserializer implements org.apache.kafka.common.serialization.Deserializer<ActiveTrucks> {
        @Override
        public void configure(final Map<String, ?> map, final boolean b) {

        }

        @Override
        @Nullable
        public ActiveTrucks deserialize(final String topic, @Nullable final byte[] bytes) {
            if (bytes == null || bytes.length == 0) {
                return null;
            }

            try (final DataInputStream dataInputStream = new DataInputStream(new ByteArrayInputStream(bytes))) {
                final ActiveTrucks activeTrucks = new ActiveTrucks();
                for (int i = dataInputStream.readInt(); i > 0; i--) {
                    activeTrucks.add(dataInputStream.readUTF());
                }
                return activeTrucks;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {

        }
    }
}
