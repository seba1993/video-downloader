package com.github.luischavez.videodownloader.system;

import com.google.inject.Binder;
import java.util.function.Supplier;

public class GuiceDependencyRegister implements DependencyRegister {

    private final Binder binder;

    public GuiceDependencyRegister(Binder binder) {
        this.binder = binder;
    }

    @Override
    public <T, E extends T> void single(Class<T> baseClass, Class<E> specificClass) {
        ClassUtils.annotate(specificClass);
        binder.bind(baseClass).to(specificClass).asEagerSingleton();
    }

    @Override
    public <T, E extends T> void bind(Class<T> baseClass, E instance) {
        binder.bind(baseClass).toInstance(instance);
    }

    @Override
    public <T, E extends T> void factory(Class<T> baseClass, Supplier<E> instanceSupplier) {
        binder.bind(baseClass).toProvider(() -> instanceSupplier.get());
    }
}
