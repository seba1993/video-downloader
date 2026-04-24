package com.github.luischavez.videodownloader.manager.configuration;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.configuration.BaseConfigurationManager;
import com.github.luischavez.videodownloader.configuration.Configuration;
import com.google.inject.Inject;

import java.io.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class SerializerConfigurationManager extends BaseConfigurationManager {

    public static final String DEFAULT_CONFIGURATION_FOLDER = "configurations";

    private long lastChangeTimestamp;
    private long lastStoredTimestamp;

    private String configurationFolder;

    private final AtomicBoolean firstRun = new AtomicBoolean(true);

    @Inject
    public SerializerConfigurationManager(Context context) {
        super(context);

        setExecutionInterval(5_000L);
        setConfigurationFolder(DEFAULT_CONFIGURATION_FOLDER);
    }

    public String getConfigurationFolder() {
        return configurationFolder;
    }

    public void setConfigurationFolder(String configurationFolder) {
        this.configurationFolder = configurationFolder;
    }

    private String buildConfigurationFileName(Configuration configuration) {
        return String.format("config_%s_%d.bin", configuration.getClass().getSimpleName().toLowerCase(), configuration.uid());
    }

    private void serialize(Configuration configuration, String configurationFolder) throws Exception {
        final String configurationFileName = buildConfigurationFileName(configuration);
        final String configurationFilePath = buildPath(configurationFolder, configurationFileName);

        ObjectOutputStream outputStream = null;
        try {
            outputStream = new ObjectOutputStream(new FileOutputStream(configurationFilePath));
            outputStream.writeObject(configuration);
            outputStream.flush();
        } finally {
            if (outputStream != null) outputStream.close();
        }
    }

    private void deserialize() throws Exception {
        final String configurationFolder = buildPath(getWorkingDir(), getConfigurationFolder());
        final File configurationFolderFile = new File(configurationFolder);

        if (!configurationFolderFile.exists()) return;

        File[] configurationFiles = configurationFolderFile.listFiles((dir, name) -> name.endsWith(".bin"));

        for (File configurationFile : configurationFiles) {
            ObjectInputStream inputStream = null;
            try {
                inputStream = new ObjectInputStream(new FileInputStream(configurationFile));
                Object object = inputStream.readObject();

                if (object instanceof Configuration) {
                    configurations.add(Configuration.class.cast(object));
                }
            } finally {
                if (inputStream != null) inputStream.close();
            }
        }
    }

    private void storeConfiguration() throws Exception {
        lastStoredTimestamp = System.currentTimeMillis();

        final String configurationFolder = buildPath(getWorkingDir(), getConfigurationFolder());
        final File configurationFolderFile = new File(configurationFolder);

        if (configurationFolderFile.exists() && configurationFolderFile.list().length > 0) {
            for (File file : configurationFolderFile.listFiles()) file.delete();
        }

        if (!configurationFolderFile.exists()) configurationFolderFile.mkdirs();

        for (Configuration configuration : configurations) {
            serialize(configuration, configurationFolder);
        }
    }

    @Override
    public void add(Configuration configuration, boolean append) {
        super.add(configuration, append);

        lastChangeTimestamp = System.currentTimeMillis();
    }

    @Override
    public void remove(Configuration configuration) {
        super.remove(configuration);

        lastChangeTimestamp = System.currentTimeMillis();
    }

    @Override
    protected boolean doWork() throws Exception {
        if (firstRun.get()) {
            firstRun.set(false);
            deserialize();
            lastStoredTimestamp = System.currentTimeMillis();
            lastChangeTimestamp = lastStoredTimestamp;
        } else {
            if (lastChangeTimestamp > lastStoredTimestamp) {
                storeConfiguration();
            }
        }

        return true;
    }
}
