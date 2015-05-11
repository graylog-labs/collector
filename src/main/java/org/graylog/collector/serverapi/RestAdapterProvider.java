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
package org.graylog.collector.serverapi;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.collector.AgentVersion;
import org.graylog.collector.annotations.GraylogServerURL;
import retrofit.RequestInterceptor;
import retrofit.RestAdapter;
import retrofit.converter.JacksonConverter;

import javax.inject.Inject;
import javax.inject.Provider;

public class RestAdapterProvider implements Provider<RestAdapter> {
    private final String graylogServerURL;

    @Inject
    public RestAdapterProvider(@GraylogServerURL String graylogServerURL) {
        this.graylogServerURL = graylogServerURL;
    }

    @Override
    public RestAdapter get() {
        return new RestAdapter.Builder()
                .setEndpoint(graylogServerURL)
                .setConverter(new JacksonConverter(new ObjectMapper()))
                .setRequestInterceptor(new RequestInterceptor() {
                    @Override
                    public void intercept(RequestFacade request) {
                        request.addHeader("User-Agent", "Graylog Agent " + AgentVersion.CURRENT);
                        request.addHeader("X-Graylog-Agent-Version", AgentVersion.CURRENT.version());
                    }
                })
                .build();
    }
}
