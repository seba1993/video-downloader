package com.github.luischavez.videodownloader.system;

import com.github.luischavez.videodownloader.manager.Manager;
import com.github.luischavez.videodownloader.manager.ManagerStateException;

import java.util.List;

public interface System {

    List<Manager> getManagers();
    <M extends Manager> M getManager(Class<M> managerClass) throws ObjectCreationException;
    <M extends Manager> void registerManager(Class<M> managerClass) throws ObjectCreationException;
    void startAllManagers(boolean wait) throws ManagerStateException;
    void stopAllManagers(boolean wait) throws ManagerStateException;
    DependencyInjection getDependencyInjection();
}
