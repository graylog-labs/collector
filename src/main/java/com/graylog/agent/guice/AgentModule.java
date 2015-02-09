package com.graylog.agent.guice;

import com.google.common.util.concurrent.Service;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;
import com.graylog.agent.annotations.AgentConfigurationFactory;
import com.graylog.agent.annotations.AgentInputConfiguration;
import com.graylog.agent.annotations.AgentInputFactory;
import com.graylog.agent.annotations.AgentOutputConfiguration;
import com.graylog.agent.annotations.AgentOutputFactory;
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

    public void registerInput(Class<? extends Input> inputClass, Class<? extends InputConfiguration> configClass) {
        registerInputConfiguration(configClass);

        @SuppressWarnings("unchecked")
        final Class<? extends Input.Factory<? extends Input, ? extends InputConfiguration>> factoryClass =
                (Class<? extends Input.Factory<? extends Input, ? extends InputConfiguration>>)
                        GuiceUtils.findInnerClassAnnotatedWith(AgentInputFactory.class, inputClass, Input.Factory.class);

        if (factoryClass != null) {
            install(new FactoryModuleBuilder().implement(Input.class, inputClass).build(factoryClass));
        }
    }

    public void registerOutput(Class<? extends Output> outputClass, Class<? extends OutputConfiguration> configClass) {
        registerOutputConfiguration(configClass);

        @SuppressWarnings("unchecked")
        final Class<? extends Output.Factory<? extends Output, ? extends OutputConfiguration>> factoryClass =
                (Class<? extends Output.Factory<? extends Output, ? extends OutputConfiguration>>)
                        GuiceUtils.findInnerClassAnnotatedWith(AgentOutputFactory.class, outputClass, Output.Factory.class);

        if (factoryClass != null) {
            install(new FactoryModuleBuilder().implement(Output.class, outputClass).build(factoryClass));
        }
    }

    public void registerInputConfiguration(Class<? extends InputConfiguration> configClass) {
        if (configClass.isAnnotationPresent(AgentInputConfiguration.class)) {
            final AgentInputConfiguration annotation = configClass.getAnnotation(AgentInputConfiguration.class);
            registerInputConfiguration(annotation.type(), configClass);
        } else {
            LOG.error("{} not annotated with {}. Cannot determine its type. This is a bug, please use that annotation, this configuration will not be available",
                    configClass, AgentInputConfiguration.class);
        }
    }

    private void registerInputConfiguration(String type, Class<? extends InputConfiguration> configClass) {
        if (inputsMapBinder == null) {
            this.inputsMapBinder = MapBinder.newMapBinder(binder(),
                    TypeLiteral.get(String.class),
                    new TypeLiteral<InputConfiguration.Factory<? extends InputConfiguration>>() {
                    });
        }

        @SuppressWarnings("unchecked")
        final Class<? extends InputConfiguration.Factory<? extends InputConfiguration>> factoryClass =
                (Class<? extends InputConfiguration.Factory<? extends InputConfiguration>>)
                        GuiceUtils.findInnerClassAnnotatedWith(AgentConfigurationFactory.class, configClass, InputConfiguration.Factory.class);

        if (factoryClass == null) {
            LOG.error("No configuration factory found for {}. Make sure to create an inner Factory interface annotated with @{}.",
                    configClass, AgentConfigurationFactory.class.getSimpleName());
            return;
        }

        install(new FactoryModuleBuilder().implement(Configuration.class, configClass).build(factoryClass));

        inputsMapBinder.addBinding(type).to(factoryClass);
    }

    private void registerOutputConfiguration(Class<? extends OutputConfiguration> configClass) {
        if (configClass.isAnnotationPresent(AgentOutputConfiguration.class)) {
            final AgentOutputConfiguration annotation = configClass.getAnnotation(AgentOutputConfiguration.class);
            registerOutputConfiguration(annotation.type(), configClass);
        } else {
            LOG.error("{} not annotated with {}. Cannot determine its type. This is a bug, please use that annotation, this configuration will not be available",
                    configClass, AgentOutputConfiguration.class);
        }
    }

    private void registerOutputConfiguration(String type, Class<? extends OutputConfiguration> configClass) {
        if (inputsMapBinder == null) {
            this.outputsMapBinder = MapBinder.newMapBinder(binder(),
                    TypeLiteral.get(String.class),
                    new TypeLiteral<OutputConfiguration.Factory<? extends OutputConfiguration>>() {
                    });
        }

        @SuppressWarnings("unchecked")
        final Class<? extends OutputConfiguration.Factory<? extends OutputConfiguration>> factoryClass =
                (Class<? extends OutputConfiguration.Factory<? extends OutputConfiguration>>)
                        GuiceUtils.findInnerClassAnnotatedWith(AgentConfigurationFactory.class, configClass, OutputConfiguration.Factory.class);

        if (factoryClass == null) {
            LOG.error("No configuration factory found for {}. Make sure to create an inner Factory interface annotated with @{}.",
                    configClass, AgentConfigurationFactory.class.getSimpleName());
            return;
        }

        install(new FactoryModuleBuilder().implement(Configuration.class, configClass).build(factoryClass));

        outputsMapBinder.addBinding(type).to(factoryClass);
    }
}
