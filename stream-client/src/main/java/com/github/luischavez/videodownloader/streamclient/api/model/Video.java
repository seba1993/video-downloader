package com.github.luischavez.videodownloader.streamclient.api.model;

import com.google.gson.annotations.SerializedName;

public class Video {

    @SerializedName("id")
    private long id;

    @SerializedName("name")
    private String name;

    @SerializedName("language")
    private String language;

    @SerializedName("size")
    private long size;

    @SerializedName("subtitle")
    private String subtitle;

    @SerializedName("transcription")
    private String transcription;

    @SerializedName("status")
    private String status;

    @SerializedName("link")
    private String link;

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getLanguage() {
        return language;
    }

    public long getSize() {
        return size;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getTranscription() {
        return transcription;
    }

    public String getStatus() {
        return status;
    }

    public String getLink() {
        return link;
    }

    @Override
    public String toString() {
        return "Video {" +
               "id=" + id +
               ", name='" + name + '\'' +
               ", language='" + language + '\'' +
               ", size=" + size +
               ", status='" + status + '\'' +
               ", link='" + link + '\'' +
               '}';
    }
}
