package com.github.luischavez.videodownloader.system;

import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;

import java.util.function.Supplier;

public class GuiceDependencyRegister implements DependencyRegister {

    private final Binder binder;

    public GuiceDependencyRegister(Binder binder) {
        this.binder = binder;
    }

    @Override
    public <T> DependencyBinder<T> bindClass(Class<T> baseClass) {
        return new GuiceDependencyBinder<>(binder.bind(baseClass));
    }

    public static class GuiceDependencyBinder<T> implements DependencyBinder<T> {

        private final LinkedBindingBuilder<T> bindingBuilder;

        public GuiceDependencyBinder(LinkedBindingBuilder<T> bindingBuilder) {
            this.bindingBuilder = bindingBuilder;
        }

        @Override
        public <E extends T> void toClass(Class<E> specificClass) {
            ByteBuddyUtils.annotate(specificClass);
            bindingBuilder.to(specificClass);
        }

        @Override
        public <E extends T> void toInstance(E instance) {
            bindingBuilder.toInstance(instance);
        }

        @Override
        public <E extends T> void toFactory(Supplier<E> instanceSupplier) {
            bindingBuilder.toProvider(() -> instanceSupplier.get());
        }
    }
}
