/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector;

import org.junit.rules.ExternalResource;

import javax.annotation.Nonnull;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.ZoneId;

public class ClockProviderRule extends ExternalResource {
    @Override
    protected void after() {
        ClockProvider.reset();
        super.after();
    }

    public void setClock(@Nonnull final Clock clock) {
        ClockProvider.setClock(clock);
    }

    public void setInstant(@Nonnull final OffsetDateTime instant) {
        ClockProvider.setClock(Clock.fixed(instant.toInstant(), ZoneId.of(instant.getOffset().getId())));
    }

    @Nonnull
    public ClockProviderRule withInstant(@Nonnull final OffsetDateTime instant) {
        setInstant(instant);
        return this;
    }
}
