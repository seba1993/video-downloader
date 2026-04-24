package com.github.luischavez.videodownloader.streamclient.api.gui.model;

import com.github.luischavez.videodownloader.streamclient.Main;
import com.github.luischavez.videodownloader.streamclient.api.model.Video;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;

public class VideoListModel extends AbstractListModel<String> {

    private final String status;

    private long lastRefresh;
    private List<Video> videos;

    public VideoListModel(String status) {
        this.status = status;

        lastRefresh = 0;
        videos = new ArrayList<>();
    }

    public void reload() {
        lastRefresh = 0;
        videos();
        fireContentsChanged(this, 0, videos.size());
    }

    public List<Video> videos() {
        if (lastRefresh == 0 || System.currentTimeMillis() - lastRefresh > 5_000) {
            videos = Main.getVideos(status);
            lastRefresh = System.currentTimeMillis();
        }

        return videos;
    }

    public Video video(int index) {
        List<Video> videos = videos();

        if (index < videos.size()) {
            return videos.get(index);
        }

        return null;
    }

    @Override
    public int getSize() {
        return videos().size();
    }

    @Override
    public String getElementAt(int index) {
        Video video = video(index);

        return video != null ? video.getName() : null;
    }
}
