package com.github.luischavez.videodownloader;

import com.github.luischavez.videodownloader.system.System;

public interface Context extends Logger {

    String getWorkingDir();

    System getSystem();
}
