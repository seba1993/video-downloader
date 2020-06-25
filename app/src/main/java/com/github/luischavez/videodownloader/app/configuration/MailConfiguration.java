package com.github.luischavez.videodownloader.app.configuration;

import com.github.luischavez.videodownloader.configuration.BaseConfiguration;
import com.github.luischavez.videodownloader.configuration.validation.NotEmptyValidation;
import com.github.luischavez.videodownloader.configuration.validation.Validable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MailConfiguration extends BaseConfiguration {

    static final long serialVersionUID = 1L;

    @Validable(value = NotEmptyValidation.class, name = "Email")
    private String email;

    @Validable(value = NotEmptyValidation.class, name = "Password")
    private String password;

    @Validable(value = NotEmptyValidation.class, name = "Distribution List")
    private List<String> distributionList;

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getDistributionList() {
        return distributionList;
    }

    public void setDistributionList(String... distributionList) {
        this.distributionList = new ArrayList<>(Arrays.asList(distributionList));
    }
}
