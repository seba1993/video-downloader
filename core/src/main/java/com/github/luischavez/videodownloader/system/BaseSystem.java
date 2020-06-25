package com.github.luischavez.videodownloader.system;

import com.github.luischavez.videodownloader.manager.Manager;
import com.github.luischavez.videodownloader.manager.ManagerStateException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class BaseSystem implements System {

    private final DependencyInjection dependencyInjection;

    private final Map<Class<? extends Manager>, Manager> managers;

    public BaseSystem(DependencyInjection dependencyInjection) {
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
    public void startAllManagers(boolean wait) throws ManagerStateException {
        List<Manager> managers = getManagers();
        for (Manager manager : managers) {
            if (!manager.isRunning()) manager.start();
        }

        if (wait) {
            while (!managers.isEmpty()) {
                List<Manager> runningManager = managers.stream()
                        .filter(manager -> manager.isRunning())
                        .collect(Collectors.toList());

                managers.removeAll(runningManager);

                try {
                    Thread.sleep(1_000);
                } catch (Exception ex) {
                    // IGNORE
                }
            }
        }
    }

    @Override
    public void stopAllManagers(boolean wait) throws ManagerStateException {
        List<Manager> managers = getManagers();
        for (Manager manager : managers) {
            if (manager.isRunning()) manager.stop();
        }

        if (wait) {
            while (!managers.isEmpty()) {
                List<Manager> stoppedManagers = managers.stream()
                        .filter(manager -> !manager.isRunning())
                        .collect(Collectors.toList());

                managers.removeAll(stoppedManagers);

                try {
                    Thread.sleep(1_000);
                } catch (Exception ex) {
                    // IGNORE
                }
            }
        }
    }

    @Override
    public DependencyInjection getDependencyInjection() {
        return dependencyInjection;
    }
}
