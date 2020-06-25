package com.github.luischavez.videodownloader.app.task;

import com.github.luischavez.videodownloader.util.PlatformUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class RunningPids implements Serializable {

    static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(RunningPids.class);

    private static RunningPids INSTANCE;

    private final List<Long> pids;

    public RunningPids() {
        pids = new ArrayList<>();
    }

    public List<Long> pids() {
        return pids;
    }

    public void add(long pid) {
        pids.add(pid);
        store();
    }

    public void remove(long pid) {
        pids.remove(pid);
        PlatformUtils.kill(pid);
        store();
    }

    public void clear() {
        pids.clear();
        store();
    }

    public void store() {
        String userDir = System.getProperty("user.dir");
        String fileName = "running_pids.obj";

        File file = new File(userDir, fileName);

        if (file.exists()) {
            file.delete();
        }

        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(file))) {
            outputStream.writeObject(this);
            outputStream.flush();
        } catch (IOException ex) {
            LOGGER.error("can't store running_pids.obj file", ex);
        }
    }

    public void read() {
        String userDir = System.getProperty("user.dir");
        String fileName = "running_pids.obj";

        File file = new File(userDir, fileName);

        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(file))) {
            INSTANCE = (RunningPids) inputStream.readObject();
        } catch (IOException | ClassNotFoundException ex) {
            LOGGER.error("can't read running_pids.obj file", ex);
            INSTANCE = new RunningPids();
        }
    }

    public static RunningPids load() {
        if (INSTANCE == null) {
            INSTANCE = new RunningPids();
        }

        return INSTANCE;
    }

    public static void killAll() {
        RunningPids.load().read();
        RunningPids.load().pids().stream().forEach(pid -> {
            PlatformUtils.kill(pid);
        });
        RunningPids.load().clear();
    }
}
