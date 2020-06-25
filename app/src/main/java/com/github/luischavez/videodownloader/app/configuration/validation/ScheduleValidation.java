package com.github.luischavez.videodownloader.app.configuration.validation;

import com.github.luischavez.videodownloader.configuration.Configuration;
import com.github.luischavez.videodownloader.configuration.validation.MinValidation;
import com.github.luischavez.videodownloader.configuration.validation.Validation;
import com.github.luischavez.videodownloader.configuration.validation.ValidationResult;
import com.github.luischavez.videodownloader.schedule.Schedule;

import java.util.List;

public class ScheduleValidation implements Validation {

    @Override
    public ValidationResult validate(Configuration configuration, String name, String prettyName, Object object, String[] params) throws Exception {
        if (object instanceof List) {
            final List<Schedule> schedules = List.class.cast(object);

            final MinValidation minValidation = new MinValidation();

            for (Schedule schedule : schedules) {
                ValidationResult validationResult = minValidation.validate(configuration, "duration", "Duration", schedule.getDuration(), new String[]{"1"});

                if (!validationResult.isSuccess()) return validationResult;
            }
        }

        return ValidationResult.pass();
    }
}
