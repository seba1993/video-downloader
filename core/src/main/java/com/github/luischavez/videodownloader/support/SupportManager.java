package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.listener.Listener;
import com.github.luischavez.videodownloader.manager.Manager;

import java.util.Collections;
import java.util.List;

@Listener(SupportListener.class)
public interface SupportManager extends Manager {

    Support get(String location);

    default List<Media> media(String location) {
        Support support = get(location);

        if (support == null) {
            getListeners(SupportListener.class).stream()
                    .forEach(supportListener -> supportListener.onSupportNotFound(location));

            return Collections.emptyList();
        }

        try {
            List<Media> medias = support.getMedia(location);

            getListeners(SupportListener.class).stream()
                    .forEach(supportListener -> supportListener.onMediaFound(location, medias));

            return medias;
        } catch (MediaOfflineException ex) {
            getListeners(SupportListener.class).stream()
                    .forEach(supportListener -> supportListener.onMediaOffline(location, ex));
        } catch (MediaNotFoundException ex) {
            getListeners(SupportListener.class).stream()
                    .forEach(supportListener -> supportListener.onMediaNotFound(location));
        }

        return Collections.emptyList();
    }
}
