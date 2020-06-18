package com.github.luischavez.videodownloader.listener;

import com.github.luischavez.videodownloader.Context;

import java.util.List;

public interface Listenable {

    <L> List<L> getListeners(Class<L> listenerClass) throws InvalidListenerClassException;
    <L> void addListener(Class<L> listenerClass, Object listener);
    <L> void removeListener(Class<L> listenerClass, Object listener);
    <L> void registerContextListener(Context context, Class<L> listenerClass) throws InvalidListenerClassException;
}
