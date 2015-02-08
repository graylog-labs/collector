package com.graylog.agent.guice;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.google.inject.multibindings.MapBinder;
import com.graylog.agent.annotations.AgentConfigurationFactory;
import com.graylog.agent.annotations.AgentInputConfiguration;
import com.graylog.agent.annotations.AgentInputFactory;
import com.graylog.agent.config.Configuration;
import com.graylog.agent.inputs.Input;
import com.graylog.agent.inputs.InputConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;

public abstract class AgentModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(AgentModule.class);

    private MapBinder<String, InputConfiguration.Factory<? extends InputConfiguration>> inputsMapBinder = null;

    public void registerInput(Class<? extends Input> inputClass) {
        @SuppressWarnings("unchecked")
        final Class<? extends Input.Factory<? extends Input, ? extends InputConfiguration>> factoryClass =
                (Class<? extends Input.Factory<? extends Input, ? extends InputConfiguration>>)
                        findInnerClassAnnotatedWith(AgentInputFactory.class, inputClass, Input.Factory.class);

        if (factoryClass != null) {
            install(new FactoryModuleBuilder().implement(Input.class, inputClass).build(factoryClass));
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
                    new TypeLiteral<InputConfiguration.Factory<? extends InputConfiguration>>() {});
        }

        @SuppressWarnings("unchecked")
        final Class<? extends InputConfiguration.Factory<? extends InputConfiguration>> factoryClass =
                (Class<? extends InputConfiguration.Factory<? extends InputConfiguration>>)
                        findInnerClassAnnotatedWith(AgentConfigurationFactory.class, configClass, InputConfiguration.Factory.class);

        if (factoryClass == null) {
            LOG.error("No configuration factory found for {}. Make sure to create an inner Factory interface annotated with @{}.",
                    configClass, AgentConfigurationFactory.class.getSimpleName());
            return;
        }

        LOG.info("type={} configClass={}", type, configClass);

        install(new FactoryModuleBuilder().implement(Configuration.class, configClass).build(factoryClass));

        inputsMapBinder.addBinding(type).to(factoryClass);
    }

    @Nullable
    protected Class<?> findInnerClassAnnotatedWith(Class<? extends Annotation> annotationClass,
                                                   Class<?> containingClass,
                                                   Class<?> targetClass) {
        final Class<?>[] declaredClasses = containingClass.getDeclaredClasses();
        Class<?> annotatedClass = null;
        for (final Class<?> declaredClass : declaredClasses) {
            if (!declaredClass.isAnnotationPresent(annotationClass)) {
                continue;
            }
            if (targetClass.isAssignableFrom(declaredClass)) {
                if (annotatedClass != null) {
                    LOG.error("Multiple annotations for {} found in {}. This is invalid.", annotatedClass.getSimpleName(), containingClass);
                    return null;
                }
                annotatedClass = declaredClass;
            } else {
                LOG.error("{} annotated as {} is not extending the expected {}. Did you forget to implement the correct interface?",
                        declaredClass, annotationClass.getSimpleName(), targetClass);
                return null;
            }
        }
        return annotatedClass;
    }
}
