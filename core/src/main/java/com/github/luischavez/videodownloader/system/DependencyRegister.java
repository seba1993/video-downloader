package com.github.luischavez.videodownloader.system;

import java.util.function.Supplier;

public interface DependencyRegister {

    <T, E extends T> void single(Class<T> baseClass, Class<E> specificClass);
    <T, E extends T> void bind(Class<T> baseClass, E instance);
    <T, E extends T> void factory(Class<T> baseClass, Supplier<E> instanceSupplier);
}
