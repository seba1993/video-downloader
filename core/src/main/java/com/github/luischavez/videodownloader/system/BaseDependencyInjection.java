package com.github.luischavez.videodownloader.system;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;

public abstract class BaseDependencyInjection implements DependencyInjection {

    protected abstract <T> T newInstance(Class<T> objectClass) throws Exception;

    protected <T, A extends Annotation> boolean hasAnnotation(Class<T> objectClass, Class<A> annotationClass) {
        Constructor<?>[] constructors = objectClass.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(annotationClass)) return true;
        }

        return false;
    }

    @Override
    public <T> boolean isAnnotated(Class<T> objectClass) {
        return hasAnnotation(objectClass, Injected.class);
    }

    @Override
    public <T> T make(Class<T> objectClass) throws ObjectCreationException {
        if (!objectClass.isInterface() && !isAnnotated(objectClass)) throw new ObjectCreationException("Missing Injected annotation in class " + objectClass.getName());

        try {
            return newInstance(objectClass);
        } catch (Exception ex) {
            throw new ObjectCreationException("can't instantiate class " + objectClass.getName(), ex);
        }
    }
}
