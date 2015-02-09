package com.graylog.agent.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

public class AgentInjector {
    public static Injector createInjector(Module... modules) {
        final Injector injector = Guice.createInjector(new AgentModule() {
            @Override
            protected void configure() {
                binder().requireExplicitBindings();
            }
        });

        return injector.createChildInjector(modules);
    }
}
