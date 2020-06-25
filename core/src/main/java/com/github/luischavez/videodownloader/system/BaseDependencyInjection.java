package com.github.luischavez.videodownloader.system;

import com.github.luischavez.videodownloader.util.ReflectionUtils;

public abstract class BaseDependencyInjection implements DependencyInjection {

    protected abstract <T> T newInstance(Class<T> objectClass) throws Exception;

    @Override
    public <T> T make(Class<T> objectClass) throws ObjectCreationException {
        if (ReflectionUtils.isInstantiable(objectClass)) {
            if (!ReflectionUtils.hasOneEmptyConstructor(objectClass) && !ReflectionUtils.hasConstructorAnnotated(objectClass, Injected.class)) {
                throw new ObjectCreationException("Missing Injected annotation in class " + objectClass.getName());
            }
        }

        try {
            return newInstance(objectClass);
        } catch (Exception ex) {
            throw new ObjectCreationException("can't instantiate class " + objectClass.getName(), ex);
        }
    }
}
