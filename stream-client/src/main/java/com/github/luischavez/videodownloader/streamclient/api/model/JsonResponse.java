package com.github.luischavez.videodownloader.streamclient.api.model;

import com.google.gson.annotations.SerializedName;

public class JsonResponse {

    @SerializedName("message")
    private String message;

    @SerializedName("status")
    private int status;

    public String getMessage() {
        return message;
    }

    public int getStatus() {
        return status;
    }

    @Override
    public String toString() {
        return "JsonResponse {" +
               "message='" + message + '\'' +
               ", status=" + status +
               '}';
    }
}
