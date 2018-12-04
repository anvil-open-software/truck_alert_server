/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_generator;

import com.dematic.labs.truck_alert.server.api.AlertCount;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.Test;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;

import static com.dematic.labs.toolkit.helpers.test_util.DockerHelper.getBaseUrl;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class MainIT {
    private static final Logger LOGGER = getLogger(MainIT.class);

    @Test
    public void alertCountIncreases() throws InterruptedException {
        final AlertCount firstAlertsCount = getAlertsCount();
        LOGGER.info(firstAlertsCount.toString());
        SECONDS.sleep(15);
        final AlertCount secondAlertsCount = getAlertsCount();
        LOGGER.info(secondAlertsCount.toString());
        assertThat(firstAlertsCount.getCount(), lessThan(secondAlertsCount.getCount()));
    }

    @Nonnull
    private AlertCount getAlertsCount() {
        return ClientBuilder
                .newBuilder()
                .register(JacksonFeature.class)
                .build()
                .target(getBaseUrl("server.port", "alerts/temperature/raise/count"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(AlertCount.class);
    }

}
