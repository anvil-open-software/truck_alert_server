/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector.stream;

import com.dematic.labs.truck_alert.server.api.Alert;
import com.dematic.labs.truck_alert.server.api.AlertCount;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.state.HostInfo;
import org.apache.kafka.streams.state.ReadOnlyKeyValueStore;
import org.apache.kafka.streams.state.ReadOnlyWindowStore;
import org.apache.kafka.streams.state.StreamsMetadata;
import org.apache.kafka.streams.state.WindowStoreIterator;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.dematic.labs.truck_alert.server.alert_collector.ClockProvider.getClock;
import static com.dematic.labs.truck_alert.server.alert_collector.stream.MetadataService.getUrl;
import static com.dematic.labs.truck_alert.server.alert_collector.stream.MetadataService.sameHost;
import static com.dematic.labs.truck_alert.server.alert_collector.stream.TopTenAlertTopology.ACTIVE_TRUCKS_KEY;
import static com.dematic.labs.truck_alert.server.alert_collector.stream.TopTenAlertTopology.ACTIVE_TRUCKS_STORE;
import static com.dematic.labs.truck_alert.server.alert_collector.stream.TopTenAlertTopology.MOST_RECENT_ALERTS_KEY;
import static com.dematic.labs.truck_alert.server.alert_collector.stream.TopTenAlertTopology.MOST_RECENT_ALERTS_STORE;
import static java.time.Duration.ofHours;
import static java.time.Instant.now;
import static org.apache.kafka.streams.state.QueryableStoreTypes.keyValueStore;
import static org.apache.kafka.streams.state.QueryableStoreTypes.windowStore;

@Path("alerts/temperature/raise")
public class TopTenAlertRestService {
    public static final GenericType<List<Alert>> ALERT_LIST = new GenericType<List<Alert>>() {
    };
    private final MetadataService metadataService;
    private final HostInfo hostInfo;
    private final KafkaStreams streams;
    private final Client client = ClientBuilder.newBuilder().register(JacksonFeature.class).build();

    public TopTenAlertRestService(@Nonnull final KafkaStreams streams, @Nonnull final HostInfo hostInfo) {
        this.metadataService = new MetadataService(streams);
        this.hostInfo = hostInfo;
        this.streams = streams;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Nonnull
    @Path("count")
    public AlertCount getCount() {
        final StreamsMetadata
                streamsMetadata =
                metadataService.streamsMetadataForStoreAndKey(ACTIVE_TRUCKS_STORE, ACTIVE_TRUCKS_KEY, new StringSerializer());
        if (StreamsMetadata.NOT_AVAILABLE == streamsMetadata) {
            return new AlertCount(0);
        }
        if (!sameHost(streamsMetadata, hostInfo)) {
            return client
                    .target(getUrl(streamsMetadata, "alerts/temperature/raise/count"))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(AlertCount.class);
        }

        return getAlertCount(streams.store(ACTIVE_TRUCKS_STORE, windowStore()));
    }

    @Nonnull
    static AlertCount getAlertCount(@Nonnull final ReadOnlyWindowStore<String, ActiveTrucks> store) {
        final Instant now = now(getClock());
        try (final WindowStoreIterator<ActiveTrucks> activeTrucks = store
                .fetch(ACTIVE_TRUCKS_KEY, now.minus(ofHours(1)).toEpochMilli(), now.toEpochMilli())
        ) {
            if (activeTrucks.hasNext()) {
                return new AlertCount(activeTrucks.next().value.size());
            }
            return new AlertCount(0);
        }
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Nonnull
    @Path("top-ten")
    public List<Alert> getTopTen() {
        final StreamsMetadata
                streamsMetadata =
                metadataService.streamsMetadataForStoreAndKey(MOST_RECENT_ALERTS_STORE, MOST_RECENT_ALERTS_KEY, new StringSerializer());
        if (StreamsMetadata.NOT_AVAILABLE == streamsMetadata) {
            return Collections.emptyList();
        }
        if (!sameHost(streamsMetadata, hostInfo)) {
            return client
                    .target(getUrl(streamsMetadata, "alerts/temperature/raise/top-ten"))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(ALERT_LIST);
        }

        final ReadOnlyKeyValueStore<String, MostRecentAlerts> store = streams.store(MOST_RECENT_ALERTS_STORE, keyValueStore());
        final MostRecentAlerts mostRecentAlerts = getMostRecentAlerts(store);
        if (mostRecentAlerts == null) {
            return Collections.emptyList();
        }

        final List<Alert> alerts = new ArrayList<>();
        for (final Alert alert : mostRecentAlerts) {
            alerts.add(alert);
        }
        return alerts;
    }

    @Nullable
    static MostRecentAlerts getMostRecentAlerts(@Nonnull final ReadOnlyKeyValueStore<String, MostRecentAlerts> store) {
        return store.get(MOST_RECENT_ALERTS_KEY);
    }

}
