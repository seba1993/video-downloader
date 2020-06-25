package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.ContextWrapper;

public abstract class BaseSupport<M extends Media> extends ContextWrapper implements Support<M> {

    public BaseSupport(Context context) {
        super(context);
    }
}
