package com.github.luischavez.videodownloader.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

public final class ReflectionUtils {

    public static boolean isInstantiable(Class<?> objectClass) {
        int modifiers = objectClass.getModifiers();

        return !Modifier.isInterface(modifiers) && !Modifier.isAbstract(modifiers);
    }

    public static boolean hasOneEmptyConstructor(Class<?> objectClass) {
        Constructor<?>[] constructors = objectClass.getDeclaredConstructors();

        if (constructors.length == 0) return true;

        for (Constructor<?> constructor : constructors) {
            if (constructor.getParameterCount() == 0) return true;
        }

        return false;
    }

    public static boolean hasConstructorAnnotated(Class<?> objectClass, Class<? extends Annotation> annotationClass) {
        Constructor<?>[] constructors = objectClass.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(annotationClass)) return true;
        }

        return false;
    }
}
