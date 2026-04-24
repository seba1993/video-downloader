package com.github.luischavez.videodownloader;

import com.github.luischavez.videodownloader.listener.InvalidListenerClassException;
import com.github.luischavez.videodownloader.listener.Listenable;
import com.github.luischavez.videodownloader.listener.Listener;
import com.github.luischavez.videodownloader.system.System;
import com.google.inject.Inject;
import org.apache.commons.lang3.ClassUtils;

import java.util.*;

public class ContextWrapper implements Context, Listenable {

    private final Context context;
    private final Map<Class<?>, List<Object>> listenersMap;

    private Class<?>[] listenerClasses;

    @Inject
    public ContextWrapper(Context context) {
        this.context = context;

        listenersMap = new HashMap<>();

        verifyListeners();
    }

    protected final Context getWrappedContext() {
        return context;
    }

    private Listener getListenerFromClass(Class<?> objectClass) {
        if (objectClass.isAnnotationPresent(Listener.class)) {
            return objectClass.getAnnotation(Listener.class);
        }

        return null;
    }

    private Class<?>[] getListenerClasses() {
        if (listenerClasses != null) return listenerClasses;

        ArrayList<Class<?>> list = new ArrayList<>();

        List<Class<?>> classes = ClassUtils.getAllSuperclasses(getClass());
        List<Class<?>> interfaces = ClassUtils.getAllInterfaces(getClass());

        for (Class<?> objectClass : classes) {
            Listener listener = getListenerFromClass(objectClass);

            if (listener != null && !list.contains(listener.value())) list.add(listener.value());
        }
        for (Class<?> objectClass : interfaces) {
            Listener listener = getListenerFromClass(objectClass);

            if (listener != null && !list.contains(listener.value())) list.add(listener.value());
        }

        listenerClasses = list.toArray(new Class[0]);

        return listenerClasses;
    }

    private final void verifyListeners() {
        Class<?>[] listenerClasses = getListenerClasses();

        if (listenerClasses == null) return;

        for (Class<?> listenerClass : listenerClasses) {
            registerContextListener(context, listenerClass);
        }
    }

    public System getSystem() {
        return context.getSystem();
    }

    public String getWorkingDir() {
        return context.getWorkingDir();
    }

    @Override
    public String getFileSeparator() {
        return context.getFileSeparator();
    }

    @Override
    public String buildPath(String... elements) {
        return context.buildPath(elements);
    }

    @Override
    public void log(Class<?> caller, String level, String message, Throwable cause) {
        context.log(caller, level, message, cause);
    }

    @Override
    public <L> List<L> getListeners(Class<L> listenerClass) throws InvalidListenerClassException {
        if (!listenerClass.isInterface()) throw new InvalidListenerClassException("class is not a valid interface " + listenerClass.getName());

        List<L> listeners = (List<L>) listenersMap.get(listenerClass);

        return listeners == null ? Collections.emptyList() : listeners;
    }

    @Override
    public <L> void addListener(Class<L> listenerClass, Object listener) {
        if (!listenersMap.containsKey(listenerClass)) {
            listenersMap.put(listenerClass, new ArrayList<>());
        }

        listenersMap.get(listenerClass).add(listener);
    }

    @Override
    public <L> void removeListener(Class<L> listenerClass, Object listener) {
        if (!listenersMap.containsKey(listenerClass)) return;

        listenersMap.get(listenerClass).remove(listener);
    }

    @Override
    public <L> void registerContextListener(Context context, Class<L> listenerClass) throws InvalidListenerClassException {
        if (!listenerClass.isInterface()) throw new InvalidListenerClassException("class is not a valid interface " + listenerClass.getName());

        if (listenerClass.isAssignableFrom(context.getClass())) {
            addListener(listenerClass, context);
        }
    }
}
