package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.task.Task;

import java.util.List;
import java.util.Map;

public interface Support extends Context {

    boolean canHandle(String location);

    List<Media> getMedia(String location) throws MediaNotFoundException, MediaOfflineException;

    Task generateTask(String location, Media media, Map<String, Object> params);
}
