/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import com.dematic.labs.truck_alert.server.api.Alert;
import com.dematic.labs.truck_alert.server.api.MeasuredValue;
import org.apache.kafka.common.serialization.Serde;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import static java.time.Duration.ofMillis;

public class AlertSerde implements Serde<Alert> {
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
            try (final ByteArrayOutputStream out = new ByteArrayOutputStream();
                 final DataOutputStream dataOutputStream = new DataOutputStream(out)) {
                serialize(dataOutputStream, alert);
                dataOutputStream.flush();
                return out.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        public void serialize(@Nonnull final DataOutput dataOutput, @Nonnull final Alert alert) throws IOException {
            dataOutput.writeUTF(alert.getTruck());
            writeMeasuredValue(dataOutput, null, alert.getMin());
            final MeasuredValue[] measurements = alert.getMeasurements();
            dataOutput.writeShort(measurements.length);
            OffsetDateTime timestamp = null;
            for (MeasuredValue measurement : measurements) {
                timestamp = writeMeasuredValue(dataOutput, timestamp, measurement);
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

            try (final DataInputStream dataInput = new DataInputStream(new ByteArrayInputStream(bytes))) {
                return deserialize(dataInput);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Nonnull
        public Alert deserialize(@Nonnull final DataInput dataInput) throws IOException {
            final String truck = dataInput.readUTF();
            final MeasuredValue min = readMeasuredValue(dataInput, null);
            final MeasuredValue[] measurements = new MeasuredValue[dataInput.readShort()];
            OffsetDateTime previous = null;
            for (int i = 0; i < measurements.length; i++) {
                measurements[i] = readMeasuredValue(dataInput, previous);
                previous = measurements[i].getTimestamp();
            }
            final MeasuredValue max = measurements[measurements.length - 1];
            return new Alert(truck, min, max, measurements);
        }

        @Override
        public void close() {

        }
    }

    @Nonnull
    private static MeasuredValue readMeasuredValue(@Nonnull final DataInput dataInput, @Nullable final OffsetDateTime previous) throws IOException {
        OffsetDateTime timestamp;
        if (previous == null) {
            timestamp = readOffsetDateTime(dataInput);
        } else {
            timestamp = previous.plus(ofMillis(dataInput.readInt()));
        }
        return new MeasuredValue(timestamp, dataInput.readFloat());
    }

    @Nonnull
    private static OffsetDateTime readOffsetDateTime(@Nonnull final DataInput dataInput) throws IOException {
        final byte[] bytes = new byte[dataInput.readByte()];
        dataInput.readFully(bytes);
        return OffsetDateTime.parse(new String(bytes, StandardCharsets.US_ASCII));
    }

    @Nonnull
    private static OffsetDateTime writeMeasuredValue(@Nonnull final DataOutput dataOutputStream, @Nullable final OffsetDateTime previous, @Nonnull final MeasuredValue value) throws IOException {
        final OffsetDateTime timestamp = value.getTimestamp();
        if (previous == null) {
            writeOffsetDateTime(dataOutputStream, value.getTimestamp());
        } else {
            dataOutputStream.writeInt((int) previous.until(timestamp, ChronoUnit.MILLIS));
        }
        dataOutputStream.writeFloat(value.getValue());
        return timestamp;
    }

    private static void writeOffsetDateTime(@Nonnull final DataOutput dataOutput, @Nonnull final OffsetDateTime timestamp) throws IOException {
        final byte[] bytes = timestamp.toString().getBytes(StandardCharsets.US_ASCII);
        dataOutput.write(bytes.length);
        dataOutput.write(bytes);
    }

}
