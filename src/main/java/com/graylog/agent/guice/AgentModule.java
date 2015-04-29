package com.graylog.agent.guice;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.graylog.agent.buffer.BufferConsumer;
import com.graylog.agent.config.Configuration;
import com.graylog.agent.inputs.Input;
import com.graylog.agent.inputs.InputConfiguration;
import com.graylog.agent.outputs.Output;
import com.graylog.agent.outputs.OutputConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AgentModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(AgentModule.class);

    private MapBinder<String, InputConfiguration.Factory<? extends InputConfiguration>> inputsMapBinder = null;
    private MapBinder<String, OutputConfiguration.Factory<? extends OutputConfiguration>> outputsMapBinder = null;

    private Multibinder<Service> services = null;
    private Multibinder<BufferConsumer> bufferConsumers = null;

    public void registerService(Class<? extends Service> serviceClass) {
        if (services == null) {
            services = Multibinder.newSetBinder(binder(), Service.class);
        }
        services.addBinding().to(serviceClass);
    }

    public void registerBufferConsumer(Class<? extends BufferConsumer> consumerClass) {
        if (bufferConsumers == null) {
            bufferConsumers = Multibinder.newSetBinder(binder(), BufferConsumer.class);
        }
        bufferConsumers.addBinding().to(consumerClass);
    }

    public void registerInput(String type,
                              Class<? extends Input> inputClass,
                              Class<? extends Input.Factory<? extends Input, ? extends InputConfiguration>> inputFactoryClass,
                              Class<? extends InputConfiguration> inputConfigurationClass,
                              Class<? extends InputConfiguration.Factory<? extends InputConfiguration>> inputConfigurationFactoryClass) {
        if (inputsMapBinder == null) {
            this.inputsMapBinder = MapBinder.newMapBinder(binder(),
                    TypeLiteral.get(String.class),
                    new TypeLiteral<InputConfiguration.Factory<? extends InputConfiguration>>() {
                    });
        }

        install(new FactoryModuleBuilder().implement(Input.class, inputClass).build(inputFactoryClass));
        install(new FactoryModuleBuilder().implement(Configuration.class, inputConfigurationClass).build(inputConfigurationFactoryClass));

        inputsMapBinder.addBinding(type).to(inputConfigurationFactoryClass);
    }

    public void registerOutput(String type,
                               Class<? extends Output> outputClass,
                               Class<? extends Output.Factory<? extends Output, ? extends OutputConfiguration>> outputFactoryClass,
                               Class<? extends OutputConfiguration> outputConfigurationClass,
                               Class<? extends OutputConfiguration.Factory<? extends OutputConfiguration>> outputConfigurationFactoryClass) {

        if (outputsMapBinder == null) {
            this.outputsMapBinder = MapBinder.newMapBinder(binder(),
                    TypeLiteral.get(String.class),
                    new TypeLiteral<OutputConfiguration.Factory<? extends OutputConfiguration>>() {
                    });
        }

        install(new FactoryModuleBuilder().implement(Output.class, outputClass).build(outputFactoryClass));
        install(new FactoryModuleBuilder().implement(Configuration.class, outputConfigurationClass).build(outputConfigurationFactoryClass));

        outputsMapBinder.addBinding(type).to(outputConfigurationFactoryClass);
    }
}
