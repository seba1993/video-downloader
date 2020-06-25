package com.github.luischavez.videodownloader.support;

import java.util.List;

public interface SupportListener {

    void onNewSupport(Support support);

    void onSupportNotFound(String location);

    void onMediaOffline(String location, Throwable throwable);

    void onMediaNotFound(String location);

    void onMediaFound(String location, List<? extends Media> medias);
}
