package com.github.luischavez.videodownloader.system;

import com.google.inject.Inject;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.MemberAttributeExtension;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.Implementation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;

public class ClassUtils {

    static {
        ByteBuddyAgent.install();
    }

    public static boolean isInstantiable(Class<?> objectClass) {
        int modifiers = objectClass.getModifiers();

        return !Modifier.isInterface(modifiers) && !Modifier.isAbstract(modifiers);
    }

    public static boolean hasOneEmptyConstructor(Class<?> objectClass) {
        Constructor<?>[] constructors = objectClass.getDeclaredConstructors();

        if (constructors.length > 1) return false;

        return constructors[0].getParameterCount() == 0;
    }

    public static boolean hasConstructorAnnotated(Class<?> objectClass, Class<? extends Annotation> annotationClass) {
        Constructor<?>[] constructors = objectClass.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            if (constructor.isAnnotationPresent(annotationClass)) return true;
        }

        return false;
    }

    public static void annotate(Class<?> objectClass) {
        if (!isInstantiable(objectClass)) return;
        if (hasConstructorAnnotated(objectClass, Inject.class));

        boolean oneEmptyConstructor = hasOneEmptyConstructor(objectClass);
        boolean constructorAnnotated = hasConstructorAnnotated(objectClass, Injected.class);

        if (!oneEmptyConstructor && !constructorAnnotated) return;

        AnnotationDescription annotationDescription = AnnotationDescription.Builder.ofType(Inject.class).build();

        new ByteBuddy()
                .with(Implementation.Context.Disabled.Factory.INSTANCE)
                .redefine(objectClass)
                .visit(new MemberAttributeExtension.ForMethod()
                        .annotateMethod(annotationDescription)
                        .on(target -> {
                            if (!target.isConstructor()) return false;

                            if (oneEmptyConstructor) {
                                return target.getParameters().isEmpty();
                            }

                            return target.getDeclaredAnnotations().isAnnotationPresent(Injected.class);
                        })
                )
                .make()
                .load(ClassUtils.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
    }
}
