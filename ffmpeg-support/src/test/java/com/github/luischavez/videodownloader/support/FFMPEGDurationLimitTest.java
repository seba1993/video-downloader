package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;

import java.io.File;
import java.util.regex.Pattern;

public class FFMPEGDurationLimitTest {

    public static void main(String[] args) {
        TestSupport support = new TestSupport(new TestContext());
        String command = "ffmpeg -nostdin -i \"input.m3u8\" -c copy \"D:\\output\\stream.mkv\"";
        String limited = support.limit(command, "D:\\output\\stream.mkv", 42L);

        if (!limited.contains("-c copy -t 42 \"D:\\output\\stream.mkv\"")) {
            throw new AssertionError("duration was not inserted before the output: " + limited);
        }

        java.lang.System.out.println("FFMPEGDurationLimitTest passed");
    }

    private static class TestSupport extends FFMPEGSupport {

        private TestSupport(Context context) {
            super(context);
        }

        private String limit(String command, String outputFile, long seconds) {
            return addDurationLimit(command, outputFile, seconds);
        }

        @Override
        protected String resolveContent(String location) {
            return "";
        }

        @Override
        protected MediaResolver[] getMediaResolvers() {
            return new MediaResolver[0];
        }

        @Override
        protected Pattern[] getLocationPatterns() {
            return new Pattern[0];
        }
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
            return String.join(File.separator, elements);
        }

        @Override
        public void log(Class<?> caller, String level, String message, Throwable cause) {
        }
    }
}
