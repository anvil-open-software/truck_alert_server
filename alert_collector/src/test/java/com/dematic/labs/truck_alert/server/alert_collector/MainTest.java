/*
 * Copyright 2018 Dematic, Corp.
 * Licensed under the MIT Open Source License: https://opensource.org/licenses/MIT
 */

package com.dematic.labs.truck_alert.server.alert_collector;

import com.dematic.labs.toolkit.helpers.test_util.SystemPropertyRule;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

public class MainTest {
    @Rule
    public final SystemPropertyRule systemPropertyRule = new SystemPropertyRule();

    @Test
    public void ensureHostInResourceConf() {
        systemPropertyRule.put("HOST_IP", "resolvedValueOfHOST");
        final Config config = ConfigFactory.load();
        assertThat(config.getString("alert-collector.host"), startsWith("resolvedValueOfHOST"));
    }

}
