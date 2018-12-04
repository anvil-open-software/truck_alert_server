/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector;

import org.apache.kafka.streams.state.HostInfo;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;

import javax.annotation.Nonnull;

class RestService {
    @Nonnull
    private final Server jettyServer;

    RestService(@Nonnull final HostInfo hostInfo) {
        jettyServer = new Server(hostInfo.port());
    }

    void configureContext(@Nonnull final Object... components) {
        final ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");

        jettyServer.setHandler(context);

        ResourceConfig rc = new ResourceConfig();
        rc.register(JacksonFeature.class).register(ObjectMapperProvider.class);
        for (Object component : components) {
            rc.register(component);
        }

        context.addServlet(new ServletHolder(new ServletContainer(rc)), "/*");
    }

    void start() {
        try {
            jettyServer.start();
        } catch (Exception e) {
            ExceptionSoftener.softenException(e);
        }
    }

    /**
     * Stop the Jetty Server
     *
     * @throws Exception
     */
    void stop() {
        try {
            jettyServer.stop();
        } catch (Exception e) {
            ExceptionSoftener.softenException(e);
        }
    }
}
