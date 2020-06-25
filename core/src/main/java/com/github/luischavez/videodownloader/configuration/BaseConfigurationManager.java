package com.github.luischavez.videodownloader.configuration;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.configuration.validation.*;
import com.github.luischavez.videodownloader.manager.BaseManager;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public abstract class BaseConfigurationManager extends BaseManager implements ConfigurationManager {

    protected final List<Configuration> configurations;

    public BaseConfigurationManager(Context context) {
        super(context);

        configurations = new CopyOnWriteArrayList<>();
    }

    protected Validation newValidationInstance(Class<? extends Validation> validationClass) {
        return getSystem().getDependencyInjection().make(validationClass);
    }

    private void removeAllByClass(Class<? extends Configuration> configurationClass) {
        List<? extends Configuration> configurations = list(configurationClass);
        this.configurations.removeAll(configurations);
    }

    @Override
    public <C extends Configuration> C get(Class<C> configurationClass) {
        return configurations.stream()
                .filter(configuration -> configuration.getClass().isAssignableFrom(configurationClass))
                .map(configuration -> configurationClass.cast(configuration))
                .findFirst()
                .orElse(null);
    }

    @Override
    public <C extends Configuration> List<C> list(Class<C> configurationClass) {
        List<C> list = configurations.stream()
                .filter(configuration -> configuration.getClass().isAssignableFrom(configurationClass))
                .map(configuration -> configurationClass.cast(configuration))
                .collect(Collectors.toList());

        return list == null ? Collections.emptyList() : list;
    }

    @Override
    public Configuration find(long uid) {
        return configurations.stream()
                .filter(configuration -> configuration.uid() == uid)
                .findFirst()
                .orElse(null);
    }

    @Override
    public void add(Configuration configuration, boolean append) {
        if (!append) removeAllByClass(configuration.getClass());
        if (configurations.contains(configuration)) remove(configuration);

        configurations.add(configuration);

        getListeners(ConfigurationListener.class).stream()
                .forEach(configurationListener -> configurationListener.onConfigurationChange(configuration));
    }

    @Override
    public void remove(Configuration configuration) {
        configurations.remove(configuration);
    }

    @Override
    public ValidationResults validate(Configuration configuration) throws ValidationException {
        Class<? extends Configuration> configurationClass = configuration.getClass();

        ValidationResults validationResults = new ValidationResults();

        Field[] fields = configurationClass.getDeclaredFields();
        for (Field field : fields) {
            try {
                if (!field.isAnnotationPresent(Validable.class) && !field.isAnnotationPresent(Validables.class)) continue;

                field.setAccessible(true);

                String name = field.getName();

                Object object = field.get(configuration);

                if (field.isAnnotationPresent(Validable.class)) {
                    Validable validable = field.getDeclaredAnnotation(Validable.class);
                    Validation validation = newValidationInstance(validable.value());
                    ValidationResult validationResult = validation.validate(configuration, name, validable.name().isEmpty() ? name : validable.name(), object, validable.params());

                    validationResults.add(validationResult);
                } else {
                    Validables validables = field.getDeclaredAnnotation(Validables.class);
                    for (Validable validable : validables.value()) {
                        Validation validation = newValidationInstance(validable.value());
                        ValidationResult validationResult = validation.validate(configuration, name, validable.name().isEmpty() ? name : validable.name(), object, validable.params());

                        validationResults.add(validationResult);
                    }
                }
            } catch (Exception ex) {
                throw new ValidationException("can't validate " + configuration.getClass().getName(), ex);
            }
        }

        return validationResults;
    }
}
