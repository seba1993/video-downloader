package com.github.luischavez.videodownloader.app.configuration;

import com.github.luischavez.videodownloader.configuration.BaseConfiguration;
import com.github.luischavez.videodownloader.configuration.validation.NotEmptyValidation;
import com.github.luischavez.videodownloader.configuration.validation.Validable;

public class PathConfiguration extends BaseConfiguration {

    static final long serialVersionUID = 1L;

    @Validable(value = NotEmptyValidation.class, name = "Autosub Path")
    private String autosubPath;

    public String getAutosubPath() {
        return autosubPath;
    }

    public void setAutosubPath(String autosubPath) {
        this.autosubPath = autosubPath;
    }
}
