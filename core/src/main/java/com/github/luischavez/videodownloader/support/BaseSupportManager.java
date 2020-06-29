package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.manager.BaseManager;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseSupportManager extends BaseManager implements SupportManager {

    protected final List<Support> supports;

    public BaseSupportManager(Context context) {
        super(context);

        this.supports = new ArrayList<>();
    }

    protected void clear() {
        supports.clear();
    }

    protected void add(Support support) {
        supports.add(support);
    }

    protected void remove(Support support) {
        supports.remove(support);
    }

    @Override
    public Support get(String location) {
        return supports.stream()
                .filter(support -> support.canHandle(location))
                .findFirst()
                .orElse(null);
    }
}
