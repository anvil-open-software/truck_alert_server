/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.StreamsMetadata;

import javax.annotation.Nonnull;

class MetadataService {

    @Nonnull
    private final KafkaStreams streams;

    MetadataService(@Nonnull final KafkaStreams streams) {
        this.streams = streams;
    }

    @Nonnull
    <K> StreamsMetadata streamsMetadataForStoreAndKey(@Nonnull final String store,
                                                      @Nonnull final K key,
                                                      @Nonnull final Serializer<K> keySerializer) {
        final StreamsMetadata metadata = streams.metadataForKey(store, key, keySerializer);
        if (metadata == null) {
            throw new IllegalArgumentException("No host found with store '" + store + "' and key '" + key + "'");
        }

        return metadata;
    }

    static boolean sameHost(@Nonnull final StreamsMetadata streamsMetadata, @Nonnull final HostInfo hostInfo) {
        return streamsMetadata.hostInfo().equals(hostInfo);
    }

    @Nonnull
    static String getUrl(@Nonnull final StreamsMetadata streamsMetadata, @Nonnull final String path) {
        return String.format("http://%s:%d/%s", streamsMetadata.host(), streamsMetadata.port(), path);
    }
}
