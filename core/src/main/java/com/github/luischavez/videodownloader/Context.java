package com.github.luischavez.videodownloader;

import com.github.luischavez.videodownloader.system.System;

import java.util.ArrayList;
import java.util.Arrays;

public interface Context extends Logger {

    System getSystem();

    String getWorkingDir();

    String getFileSeparator();

    String buildPath(String... elements);

    default <T> T[] merge(T item, T... array) {
        ArrayList<T> list = new ArrayList<>(Arrays.asList(array));
        list.add(item);

        return list.toArray(array);
    }
}
