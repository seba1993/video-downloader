package com.github.luischavez.videodownloader.streamclient.api.gui.model;

import java.util.Date;

public class Subtitle {

    private Date startAt;
    private Date endAt;
    private String text;

    public Subtitle(Date startAt, Date endAt, String text) {
        this.startAt = startAt;
        this.endAt = endAt;
        this.text = text;
    }

    public Date getStartAt() {
        return startAt;
    }

    public void setStartAt(Date startAt) {
        this.startAt = startAt;
    }

    public Date getEndAt() {
        return endAt;
    }

    public void setEndAt(Date endAt) {
        this.endAt = endAt;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
