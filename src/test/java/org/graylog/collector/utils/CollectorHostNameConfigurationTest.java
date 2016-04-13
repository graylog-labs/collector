/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.collector.utils;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CollectorHostNameConfigurationTest {
    @Test
    public void getHostNameReturnsNullIfHostNameNotOverridden() {
        final Config config = ConfigFactory.empty();
        final CollectorHostNameConfiguration hostNameConfiguration = new CollectorHostNameConfiguration(config);
        assertNull(hostNameConfiguration.getHostName());
    }

    @Test
    public void getHostNameReturnsHostNameIfHostNameOverridden() {
        final Config config = ConfigFactory.parseMap(Collections.singletonMap("host-name", "foobar.example.net"));
        final CollectorHostNameConfiguration hostNameConfiguration = new CollectorHostNameConfiguration(config);
        assertEquals("foobar.example.net", hostNameConfiguration.getHostName());
    }
}