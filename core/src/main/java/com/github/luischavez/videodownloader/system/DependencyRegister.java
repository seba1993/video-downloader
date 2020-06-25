package com.github.luischavez.videodownloader.system;

import java.util.function.Supplier;

public interface DependencyRegister {

    <T> DependencyBinder<T> bindClass(Class<T> baseClass);

    interface DependencyBinder<T> {

        <E extends T>void toClass(Class<E> specificClass);
        <E extends T> void toInstance(E instance);
        <E extends T> void toFactory(Supplier<E> instanceSupplier);
    }
}
