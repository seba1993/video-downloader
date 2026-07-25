package com.github.luischavez.videodownloader.app.task;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.manager.support.PluggableSupportManager;
import com.github.luischavez.videodownloader.support.FFMPEGTask;
import com.github.luischavez.videodownloader.support.Media;
import com.github.luischavez.videodownloader.support.Support;

import java.io.File;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

public class AudioSupportCommandTest {

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("usage: AudioSupportCommandTest URL");
        }

        String location = args[0];
        PluggableSupportManager supportManager = new PluggableSupportManager(new TestContext());
        Method doWork = PluggableSupportManager.class.getDeclaredMethod("doWork");
        doWork.setAccessible(true);
        doWork.invoke(supportManager);

        Support support = supportManager.get(location);
        if (support == null) {
            throw new IllegalStateException("support not found for " + location);
        }

        List<Media> media = support.getMedia(location);
        media.sort(Comparable::compareTo);
        if (media.isEmpty()) {
            throw new IllegalStateException("media not found for " + location);
        }

        FFMPEGTask task = (FFMPEGTask) support.generateTask(
                location,
                media.get(0),
                Map.of(
                        "base_file_name", "audio-support-test",
                        "destination_path", java.lang.System.getProperty("java.io.tmpdir"),
                        "output_type", "Audio"
                )
        );

        java.lang.System.out.println("support=" + support.getClass().getName());
        java.lang.System.out.println("media=" + media.get(0).getUrl());
        java.lang.System.out.println("output=" + task.getFileName());
        java.lang.System.out.println("command=" + task.getCommand());
    }

    private static class TestContext implements Context {

        @Override
        public com.github.luischavez.videodownloader.system.System getSystem() {
            return null;
        }

        @Override
        public String getWorkingDir() {
            return java.lang.System.getProperty("user.dir");
        }

        @Override
        public String getFileSeparator() {
            return File.separator;
        }

        @Override
        public String buildPath(String... elements) {
            File result = null;

            for (String element : elements) {
                if (element == null || element.isEmpty()) {
                    continue;
                }
                result = result == null ? new File(element) : new File(result, element);
            }

            return result == null ? "" : result.getPath();
        }

        @Override
        public void log(Class<?> caller, String level, String message, Throwable cause) {
            java.lang.System.out.println(level + " " + caller.getSimpleName() + ": " + message);
            if (cause != null) {
                cause.printStackTrace(java.lang.System.out);
            }
        }
    }
}
