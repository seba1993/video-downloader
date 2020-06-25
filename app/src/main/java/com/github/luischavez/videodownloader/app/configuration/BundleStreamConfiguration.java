package com.github.luischavez.videodownloader.app.configuration;

import com.github.luischavez.videodownloader.app.AppContext;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BundleStreamConfiguration implements Serializable {

    static final long serialVersionUID = 1L;

    private List<StreamConfiguration> streamConfigurations;

    public BundleStreamConfiguration(List<StreamConfiguration> streamConfigurations) {
        this.streamConfigurations = new ArrayList<>(streamConfigurations);
    }

    public List<StreamConfiguration> getStreamConfigurations() {
        return streamConfigurations;
    }

    public static BundleStreamConfiguration read(String filePath) throws Exception {
        ObjectInputStream inputStream = null;
        try {
            inputStream = new ObjectInputStream(new FileInputStream(new File(filePath)));
            return (BundleStreamConfiguration) inputStream.readObject();
        } finally {
            if (inputStream != null) inputStream.close();
        }
    }

    public static void store(String destinationFolderPath, String fileName) throws Exception {
        List<StreamConfiguration> streamConfigurations = AppContext.instance().getSystem().getManager(ConfigurationManager.class).list(StreamConfiguration.class);
        BundleStreamConfiguration bundleStreamConfiguration = new BundleStreamConfiguration(streamConfigurations);

        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(new FileOutputStream(new File(destinationFolderPath, fileName)));
            objectOutputStream.writeObject(bundleStreamConfiguration);
            objectOutputStream.flush();
        } finally {
            if (objectOutputStream != null) objectOutputStream.close();
        }
    }
}
