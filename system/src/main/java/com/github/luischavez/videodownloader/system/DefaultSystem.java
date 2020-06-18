package com.github.luischavez.videodownloader.system;

import com.github.luischavez.videodownloader.manager.Manager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultSystem implements System {

    private final DependencyInjection dependencyInjection;

    private final Map<Class<? extends Manager>, Manager> managers;

    public DefaultSystem(DependencyInjection dependencyInjection) {
        this.dependencyInjection = dependencyInjection;

        managers = new HashMap<>();
    }

    public List<Manager> getManagers() {
        return new ArrayList<>(managers.values());
    }

    public <M extends Manager> M getManager(Class<M> managerClass) throws ObjectCreationException {
        registerManager(managerClass);

        try {
            return (M) managers.get(managerClass);
        } catch (ClassCastException ex) {
            throw new ObjectCreationException("can't cast manager to type " + managerClass.getName(), ex);
        }
    }

    public <M extends Manager> void registerManager(Class<M> managerClass) throws ObjectCreationException {
        if (!managers.containsKey(managerClass)) {
            managers.put(managerClass, dependencyInjection.make(managerClass));
        }
    }

    @Override
    public DependencyInjection getDependencyInjection() {
        return dependencyInjection;
    }
}
