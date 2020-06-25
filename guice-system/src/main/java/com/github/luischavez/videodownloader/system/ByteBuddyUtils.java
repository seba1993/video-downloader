package com.github.luischavez.videodownloader.system;

import com.github.luischavez.videodownloader.util.ReflectionUtils;
import com.google.inject.Inject;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.asm.MemberAttributeExtension;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import net.bytebuddy.implementation.Implementation;

public class ByteBuddyUtils {

    static {
        ByteBuddyAgent.install();
    }

    public static void annotate(Class<?> objectClass) {
        if (!ReflectionUtils.isInstantiable(objectClass)) return;
        if (ReflectionUtils.hasConstructorAnnotated(objectClass, Inject.class));

        boolean oneEmptyConstructor = ReflectionUtils.hasOneEmptyConstructor(objectClass);
        boolean constructorAnnotated = ReflectionUtils.hasConstructorAnnotated(objectClass, Injected.class);

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
                .load(ByteBuddyUtils.class.getClassLoader(), ClassReloadingStrategy.fromInstalledAgent());
    }
}
