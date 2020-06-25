package com.github.luischavez.videodownloader.configuration.validation;

import java.lang.annotation.*;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(Validables.class)
public @interface Validable {

    Class<? extends Validation> value();
    String name() default "";
    String[] params() default {};
}
