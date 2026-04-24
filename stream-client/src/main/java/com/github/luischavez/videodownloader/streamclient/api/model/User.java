package com.github.luischavez.videodownloader.streamclient.api.model;

import com.google.gson.annotations.SerializedName;

public class User {

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("email")
    private String email;

    @SerializedName("role")
    private String role;

    @SerializedName("code")
    private String code;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getCode() {
        return code;
    }

    public String getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "User {" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", email='" + email + '\'' +
               ", role='" + role + '\'' +
               ", code='" + code + '\'' +
               '}';
    }
}
