package com.github.luischavez.videodownloader.app.configuration;

import com.github.luischavez.videodownloader.configuration.BaseConfiguration;
import com.github.luischavez.videodownloader.configuration.validation.NotEmptyValidation;
import com.github.luischavez.videodownloader.configuration.validation.UniqueValidation;
import com.github.luischavez.videodownloader.configuration.validation.Validable;

public class ScheduleFolderConfiguration extends BaseConfiguration {

    @Validable(value = NotEmptyValidation.class, name = "Folder")
    @Validable(value = UniqueValidation.class, name = "Folder")
    private String folder;

    @Validable(value = NotEmptyValidation.class, name = "Language")
    private String language;

    private boolean sub;

    public ScheduleFolderConfiguration(long uid) {
        super(uid);
    }

    public ScheduleFolderConfiguration() {
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isSub() {
        return sub;
    }

    public void setSub(boolean sub) {
        this.sub = sub;
    }
}
