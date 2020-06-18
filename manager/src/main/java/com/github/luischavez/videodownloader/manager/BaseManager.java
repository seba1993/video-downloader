package com.github.luischavez.videodownloader.manager;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.ContextWrapper;

public abstract class BaseManager extends ContextWrapper implements Manager {

    public BaseManager(Context context) {
        super(context);
    }

    @Override
    protected Class<?>[] getListenerClasses() {
        return new Class[]{ManagerListener.class};
    }
}
