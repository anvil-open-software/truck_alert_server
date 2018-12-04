/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector;

import com.dematic.labs.truck_alert.server.api.ObjectMapperFactory;
import com.fasterxml.jackson.databind.ObjectMapper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

@Provider
public class ObjectMapperProvider implements ContextResolver<ObjectMapper> {

    @Nonnull
    private final ObjectMapper objectMapper;

    public ObjectMapperProvider() {
        objectMapper = ObjectMapperFactory.getObjectMapper();
    }

    @Override
    @Nonnull
    public ObjectMapper getContext(@Nullable final Class<?> type) {
        return objectMapper;
    }
}
