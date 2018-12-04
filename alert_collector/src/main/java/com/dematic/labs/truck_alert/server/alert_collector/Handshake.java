/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector;

import org.apache.kafka.streams.KafkaStreams;

import javax.annotation.Nonnull;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("handshakes")
public class Handshake {
    @Nonnull
    private final KafkaStreams streams;

    public Handshake(@Nonnull final KafkaStreams streams) {
        this.streams = streams;
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response doGet() {
        if (streams.state().isRunning()) {
            return Response.ok(streams.allMetadata()).build();
        }
        return Response.ok("Streams not running (" + streams.state() + ")").build();
    }
}
