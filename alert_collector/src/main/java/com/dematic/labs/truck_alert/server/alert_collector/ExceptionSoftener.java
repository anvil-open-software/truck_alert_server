/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector;

import javax.annotation.Nonnull;

public class ExceptionSoftener {
    private ExceptionSoftener() {
    }

    @Nonnull
    static RuntimeException softenException(@Nonnull final Exception e) {
        return checkednessRemover(e);
    }

    @Nonnull
    private static <T extends Exception> T checkednessRemover(@Nonnull final Exception e) throws T {
        //noinspection unchecked
        throw (T) e;
    }
}
