package com.github.luischavez.videodownloader.app.configuration;

import com.github.luischavez.videodownloader.configuration.BaseConfiguration;
import com.github.luischavez.videodownloader.configuration.validation.NotEmptyValidation;
import com.github.luischavez.videodownloader.configuration.validation.UrlValidation;
import com.github.luischavez.videodownloader.configuration.validation.Validable;

public class MonitorConfiguration extends BaseConfiguration {

    private boolean enabled;

    @Validable(value = NotEmptyValidation.class, name = "URL")
    @Validable(value = UrlValidation.class, name = "URL")
    private String url;

    @Validable(value = NotEmptyValidation.class, name = "Code")
    private String code;

    @Validable(value = NotEmptyValidation.class, name = "Refresh Interval")
    private int refreshInterval;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getRefreshInterval() {
        return refreshInterval;
    }

    public void setRefreshInterval(int refreshInterval) {
        this.refreshInterval = refreshInterval;
    }
}
