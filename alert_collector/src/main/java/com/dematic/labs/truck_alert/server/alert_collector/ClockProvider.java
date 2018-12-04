/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector;

import javax.annotation.Nonnull;
import java.time.Clock;

public class ClockProvider {
    private Clock clock;
    private static final ClockProvider INSTANCE = new ClockProvider();

    private ClockProvider() {
        clock = defaultClock();
    }

    @Nonnull
    private static Clock defaultClock() {
        return Clock.systemUTC();
    }

    @Nonnull
    public static Clock getClock() {
        return INSTANCE.clock;
    }

    public static void setClock(@Nonnull final Clock clock) {
        INSTANCE.clock = clock;
    }

    public static void reset() {
        INSTANCE.clock = defaultClock();
    }
}
