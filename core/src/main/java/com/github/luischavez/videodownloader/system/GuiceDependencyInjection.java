package com.github.luischavez.videodownloader.system;

import com.google.inject.Binder;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;

import java.util.function.Consumer;

public class GuiceDependencyInjection extends BaseDependencyInjection {

    private Injector injector;

    public GuiceDependencyInjection() {
    }

    @Override
    public void configure(Consumer<DependencyRegister> registerConsumer) {
        injector = Guice.createInjector(new GuiceDependencyModule(registerConsumer));
    }

    @Override
    public <T> T newInstance(Class<T> objectClass) throws Exception {
        if (injector == null) throw new NullPointerException("please configure DI first");

        try {
            return injector.getInstance(objectClass);
        } catch (Exception ex) {
            throw ex;
        }
    }

    private class GuiceDependencyModule implements Module {

        private final Consumer<DependencyRegister> registerConsumer;

        private GuiceDependencyModule(Consumer<DependencyRegister> registerConsumer) {
            this.registerConsumer = registerConsumer;
        }

        @Override
        public void configure(Binder binder) {
            registerConsumer.accept(new GuiceDependencyRegister(binder));
        }
    }
}
