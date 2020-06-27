package com.github.luischavez.videodownloader.manager.support;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.*;
import com.github.luischavez.videodownloader.system.Injected;
import org.xeustechnologies.jcl.JarClassLoader;

import java.io.File;
import java.io.FileInputStream;
import java.lang.reflect.Constructor;
import java.util.*;

public class PluggableSupportManager extends BaseSupportManager {

    public static final String DEFAULT_SUPPORT_FOLDER_NAME = "supports";

    private final Map<String, Long> jarFileMap;
    private final List<String> newJars;

    private String supportFolderName;

    private JarClassLoader classLoader;

    @Injected
    public PluggableSupportManager(Context context) {
        super(context);

        supportFolderName = DEFAULT_SUPPORT_FOLDER_NAME;

        jarFileMap = new HashMap<>();
        newJars = new ArrayList<>();
    }

    @Override
    public Support get(String location) {
        Support support = super.get(location);

        if (support == null) return null;

        try {
            return newInstance(support.getClass());
        } catch (Exception ex) {
            return null;
        }
    }

    private void addNewJar(String jarFilePath, long lastModified) {
        jarFileMap.put(jarFilePath, lastModified);
        if (!newJars.contains(jarFilePath)) newJars.add(jarFilePath);
    }

    private void addOrUpdateJarFile(File jarFile) {
        String jarFilePath = jarFile.getPath();
        long lastModified = jarFile.lastModified();

        if (jarFileMap.containsKey(jarFilePath)) {
            long currentLastModified = jarFileMap.get(jarFilePath);

            if (lastModified != currentLastModified) addNewJar(jarFilePath, lastModified);
        } else {
            addNewJar(jarFilePath, lastModified);
        }
    }

    private void findNewJars() {
        String supportPath = buildPath(getWorkingDir(), supportFolderName);
        File supportDirectory = new File(supportPath);

        if (!supportDirectory.exists()) supportDirectory.mkdirs();

        File[] jarFiles = supportDirectory.listFiles((dir, name) -> name.endsWith(".jar"));

        for (File jarFile : jarFiles) {
            addOrUpdateJarFile(jarFile);
        }
    }

    private boolean hasNewJars() {
        return !newJars.isEmpty();
    }

    private String getSupportClassName(String jarFilePath) {
        String fileName = new File(jarFilePath).getName();

        return fileName.replace(".jar", "");
    }

    private Support newInstance(Class<?> supportClass) throws Exception {
        if (Support.class.isAssignableFrom(supportClass)) {
            Constructor  constructor = supportClass.getDeclaredConstructor(Context.class);

            Support support = (Support) constructor.newInstance(getWrappedContext());

            return support;
        }

        return null;
    }

    private void loadSupport(String jarFilePath) throws Exception {
        String supportClassName = getSupportClassName(jarFilePath);

        Class supportClass = classLoader.loadClass(supportClassName);

        Support support = newInstance(supportClass);
        if (support != null) {
            add(support);

            getListeners(SupportListener.class).stream()
                    .forEach(supportListener -> supportListener.onNewSupport(support));
        }
    }

    private void loadJars() throws Exception {
        classLoader = new JarClassLoader();
        clear();

        for (String jarFilePath : newJars) {
            classLoader.add(new FileInputStream(jarFilePath));
            loadSupport(jarFilePath);
        }

        newJars.clear();
    }

    @Override
    protected boolean doWork() throws Exception {
        findNewJars();

        if (hasNewJars()) {
            loadJars();
        }

        return true;
    }
}
