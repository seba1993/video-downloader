package com.github.luischavez.videodownloader.configuration.validation;

import com.github.luischavez.videodownloader.configuration.Configuration;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;

public interface Validation {

    ValidationResult validate(Configuration configuration, String name, String prettyName, Object object, String[] params) throws Exception;

    default Object getFieldValue(Configuration configuration, String name) throws Exception {
        Field field = configuration.getClass().getDeclaredField(name);
        field.setAccessible(true);

        return field.get(configuration);
    }

    default boolean isEmpty(Object object) {
        if (object == null) return true;

        if (object instanceof Boolean) {
            return !Boolean.class.cast(object).booleanValue();
        }

        if (object instanceof Number) {
            return Number.class.cast(object).doubleValue() == 0;
        }

        if (object instanceof Collection) {
            return Collection.class.cast(object).isEmpty();
        }

        if (object instanceof Map) {
            return Map.class.cast(object).isEmpty();
        }

        return object.toString().trim().isEmpty();
    }
}
