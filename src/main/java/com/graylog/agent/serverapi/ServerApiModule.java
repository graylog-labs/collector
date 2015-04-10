package com.graylog.agent.serverapi;

import com.graylog.agent.annotations.GraylogServerURL;
import com.graylog.agent.guice.AgentModule;
import retrofit.RestAdapter;

public class ServerApiModule extends AgentModule {
    private final String serverUrl;

    public ServerApiModule(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    @Override
    protected void configure() {
        bind(String.class).annotatedWith(GraylogServerURL.class).toInstance(serverUrl);
        bind(RestAdapter.class).toProvider(RestAdapterProvider.class).asEagerSingleton();
    }
}
