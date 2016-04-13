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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CollectorHostNameSupplierTest {
    @Mock
    public CollectorId collectorId;

    @Before
    public void setUp() throws Exception {
        when(collectorId.toString()).thenReturn("cafebabedeadbeef");
    }

    @Test
    public void getReturnsDefaultHostName() throws Exception {
        final CollectorHostNameSupplier supplier = new CollectorHostNameSupplier("foobar.example.net", collectorId);
        assertEquals("foobar.example.net", supplier.get());
    }

    @Test
    public void getDetectsHostNameIfDefaultIsMissing() throws Exception {
        final CollectorHostNameSupplier supplier = new CollectorHostNameSupplier(null, collectorId);
        final String hostName = supplier.get();
        assertNotNull(hostName);
    }
}