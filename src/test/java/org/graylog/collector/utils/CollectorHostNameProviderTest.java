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

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CollectorHostNameProviderTest {
    @Test
    public void providerMemoizesHostName() throws Exception {
        final CollectorHostNameSupplier supplier = mock(CollectorHostNameSupplier.class);
        when(supplier.get()).thenReturn("foobar.example.net");

        final CollectorHostNameProvider provider = new CollectorHostNameProvider(supplier);

        assertEquals("foobar.example.net", provider.get());
        assertEquals("foobar.example.net", provider.get());
        verify(supplier, only()).get();
    }
}