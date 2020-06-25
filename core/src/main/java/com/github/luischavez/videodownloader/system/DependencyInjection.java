package com.github.luischavez.videodownloader.system;

import java.util.function.Consumer;

public interface DependencyInjection {

    void configure(Consumer<DependencyRegister> registerConsumer);
    <T> T make(Class<T> objectClass) throws ObjectCreationException;
}
