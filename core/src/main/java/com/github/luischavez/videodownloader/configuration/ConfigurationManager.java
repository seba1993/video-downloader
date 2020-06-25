package com.github.luischavez.videodownloader.configuration;

import com.github.luischavez.videodownloader.configuration.validation.ValidationResults;
import com.github.luischavez.videodownloader.listener.Listener;
import com.github.luischavez.videodownloader.manager.Manager;

import java.util.List;

@Listener(ConfigurationListener.class)
public interface ConfigurationManager extends Manager {

    <C extends Configuration> C get(Class<C> configurationClass);

    <C extends Configuration> List<C> list(Class<C> configurationClass);

    Configuration find(long uid);

    void add(Configuration configuration, boolean append);

    default void add(Configuration configuration) {
        add(configuration, false);
    }

    void remove(Configuration configuration);

    ValidationResults validate(Configuration configuration) throws ValidationException;
}
