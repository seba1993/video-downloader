package com.github.luischavez.videodownloader.configuration.validation;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.configuration.Configuration;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.google.inject.Inject;

import java.util.List;

public class UniqueValidation implements Validation {

    private final Context context;

    @Inject
    public UniqueValidation(Context context) {
        this.context = context;
    }

    @Override
    public ValidationResult validate(Configuration configuration, String name, String prettyName, Object object, String[] params) throws Exception {
        final String value = object.toString();

        ConfigurationManager configurationManager = context.getSystem().getManager(ConfigurationManager.class);

        List<? extends Configuration> configurations = configurationManager.list(configuration.getClass());

        for (Configuration otherConfiguration : configurations) {
            if (otherConfiguration.uid() == configuration.uid()) continue;

            final String otherValue = getFieldValue(otherConfiguration, name).toString();

            if (value.equals(otherValue)) {
                return ValidationResult.fail(String.format("duplicated value for field %s", prettyName));
            }
        }

        return ValidationResult.pass();
    }
}
