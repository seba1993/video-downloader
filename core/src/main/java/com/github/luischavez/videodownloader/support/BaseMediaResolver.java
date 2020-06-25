package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.ContextWrapper;

public abstract class BaseMediaResolver<M extends Media> extends ContextWrapper implements MediaResolver<M> {

    public BaseMediaResolver(Context context) {
        super(context);
    }
}
