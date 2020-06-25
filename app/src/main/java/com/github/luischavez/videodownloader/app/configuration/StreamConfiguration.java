package com.github.luischavez.videodownloader.app.configuration;

import com.github.luischavez.videodownloader.app.configuration.validation.ScheduleValidation;
import com.github.luischavez.videodownloader.configuration.BaseConfiguration;
import com.github.luischavez.videodownloader.configuration.validation.*;
import com.github.luischavez.videodownloader.schedule.Schedule;

import java.util.ArrayList;
import java.util.List;

public class StreamConfiguration extends BaseConfiguration {

    static final long serialVersionUID = 1L;

    private boolean enabled;

    @Validable(value = NotEmptyValidation.class, name = "Alias")
    @Validable(value = UniqueValidation.class, name = "Alias")
    private String alias;

    @Validable(value = UrlValidation.class, name = "URL")
    @Validable(value = UniqueValidation.class, name = "URL")
    private String url;

    @Validable(value = NotEmptyValidation.class, name = "Type")
    @Validable(value = StringInValidation.class, name = "Type", params = {"Video", "Audio"})
    private String type;

    @Validable(value = NotEmptyValidation.class, name = "Country")
    private String country;

    private int preferredQuality;

    @Validable(value = NotEmptyValidation.class, name = "File Name")
    private String baseFileName;

    @Validable(value = NotEmptyValidation.class, name = "Destination")
    private String destinationPath;

    private boolean scheduleWhenAvailable;

    @Validable(value = ScheduleValidation.class, name = "Schedules")
    private List<Schedule> schedules;

    private boolean concatenate;

    @Validable(value = NotEmptyValidation.class, name = "Concatenation Destination", params = {"concatenate", "true"})
    private String concatenationPath;

    private boolean sub;

    @Validable(value = NotEmptyValidation.class, name = "Languages", params = {"sub", "true"})
    private List<String> languages;

    public StreamConfiguration(long uid) {
        super(uid);
        schedules = new ArrayList<>();
        languages = new ArrayList<>();
    }

    public StreamConfiguration() {
        super();
        schedules = new ArrayList<>();
        languages = new ArrayList<>();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getPreferredQuality() {
        return preferredQuality;
    }

    public void setPreferredQuality(int preferredQuality) {
        this.preferredQuality = preferredQuality;
    }

    public String getBaseFileName() {
        return baseFileName;
    }

    public void setBaseFileName(String baseFileName) {
        this.baseFileName = baseFileName;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public boolean isScheduleWhenAvailable() {
        return scheduleWhenAvailable;
    }

    public void setScheduleWhenAvailable(boolean scheduleWhenAvailable) {
        this.scheduleWhenAvailable = scheduleWhenAvailable;
    }

    public List<Schedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<Schedule> schedules) {
        this.schedules = schedules;
    }

    public boolean isConcatenate() {
        return concatenate;
    }

    public void setConcatenate(boolean concatenate) {
        this.concatenate = concatenate;
    }

    public String getConcatenationPath() {
        return concatenationPath;
    }

    public void setConcatenationPath(String concatenationPath) {
        this.concatenationPath = concatenationPath;
    }

    public boolean isSub() {
        return sub;
    }

    public void setSub(boolean sub) {
        this.sub = sub;
    }

    public List<String> getLanguages() {
        return languages;
    }

    public void setLanguages(List<String> languages) {
        this.languages = languages;
    }

    public void copy(StreamConfiguration streamConfiguration) {
        setEnabled(streamConfiguration.isEnabled());
        setCountry(streamConfiguration.getCountry());
        setAlias(streamConfiguration.getAlias());
        setUrl(streamConfiguration.getUrl());
        setType(streamConfiguration.getType());
        setPreferredQuality(streamConfiguration.getPreferredQuality());
        setBaseFileName(streamConfiguration.getBaseFileName());
        setDestinationPath(streamConfiguration.getDestinationPath());
        setScheduleWhenAvailable(streamConfiguration.isScheduleWhenAvailable());
        setSchedules(streamConfiguration.getSchedules());
        setConcatenate(streamConfiguration.isConcatenate());
        setConcatenationPath(streamConfiguration.getConcatenationPath());
        setSub(streamConfiguration.isSub());
        setLanguages(streamConfiguration.getLanguages());
    }
}
