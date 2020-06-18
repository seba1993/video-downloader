package com.github.luischavez.videodownloader.system;

import com.github.luischavez.videodownloader.manager.Manager;

import java.util.List;

public interface System {

    List<Manager> getManagers();
    <M extends Manager> M getManager(Class<M> managerClass) throws ObjectCreationException;
    <M extends Manager> void registerManager(Class<M> managerClass) throws ObjectCreationException;
    DependencyInjection getDependencyInjection();
}
