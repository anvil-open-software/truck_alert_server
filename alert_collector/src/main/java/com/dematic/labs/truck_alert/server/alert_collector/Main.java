/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector;

import com.dematic.labs.truck_alert.server.alert_collector.stream.TopTenAlertRestService;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.Topology;
import org.apache.kafka.streams.errors.LogAndContinueExceptionHandler;
import org.apache.kafka.streams.state.HostInfo;

import javax.annotation.Nonnull;
import java.time.temporal.ChronoUnit;
import java.util.Properties;

import static com.dematic.labs.truck_alert.server.alert_collector.stream.TopTenAlertTopology.createTopology;
import static org.apache.kafka.common.utils.Utils.getHost;
import static org.apache.kafka.common.utils.Utils.getPort;

public class Main {

    public static void main(@Nonnull final String[] args) {
        final Config config = ConfigFactory.load();

        final KafkaStreams streams = createStream(config);
        final RestService restService = createRestProxy(streams, config);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            streams.close();
            restService.stop();
        }));

        restService.start();
        streams.start();
    }

    @Nonnull
    private static KafkaStreams createStream(@Nonnull final Config config) {
        final Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "temperature-increase-alert");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, config.getString("broker"));
        props.put(StreamsConfig.APPLICATION_SERVER_CONFIG, config.getString("alert-collector.host"));
        props.put(StreamsConfig.STATE_DIR_CONFIG, "/opt/dlabs/store");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 500);
        props.put(StreamsConfig.DEFAULT_DESERIALIZATION_EXCEPTION_HANDLER_CLASS_CONFIG, LogAndContinueExceptionHandler.class.getName());

        final Topology build = createTopology(
                config.getString("topic-name"),
                config.getInt("alert-collector.minimal-duration"), config.getEnum(ChronoUnit.class, "alert-collector.minimal-duration-unit")
        );
        return new KafkaStreams(build, props);
    }

    @Nonnull
    private static RestService createRestProxy(@Nonnull final KafkaStreams streams, @Nonnull final Config config) {
        final String host = config.getString("alert-collector.host");
        final HostInfo hostInfo = new HostInfo(getHost(host), getPort(host));
        final RestService interactiveQueriesRestService = new RestService(hostInfo);
        interactiveQueriesRestService.configureContext(new Handshake(streams), new TopTenAlertRestService(streams, hostInfo));
        return interactiveQueriesRestService;
    }

}
