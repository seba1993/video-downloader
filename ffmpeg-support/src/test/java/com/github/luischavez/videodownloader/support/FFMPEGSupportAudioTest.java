package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.task.Task;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;

public class FFMPEGSupportAudioTest {

    public static void main(String[] args) {
        TestSupport support = new TestSupport(new TestContext());
        Media media = new Video(
                "test",
                "https://example.com/live.m3u8",
                new Video.VideoQuality(Quality.Type.LOW, 426, 240, 300_000),
                "avc1,mp4a",
                true
        );

        FFMPEGTask audioTask = (FFMPEGTask) support.generateTask(
                "https://example.com/watch",
                media,
                Map.of(
                        "base_file_name", "audio-test",
                        "destination_path", "output",
                        "output_type", "Audio"
                )
        );

        assertContains(audioTask.getCommand(), "-map 0:a:0");
        assertContains(audioTask.getCommand(), "-vn");
        assertContains(audioTask.getCommand(), "-c:a libmp3lame -b:a 128k");
        assertEndsWith(audioTask.getFileName(), ".mp3");

        FFMPEGTask videoTask = (FFMPEGTask) support.generateTask(
                "https://example.com/watch",
                media,
                Map.of(
                        "base_file_name", "video-test",
                        "destination_path", "output",
                        "output_type", "Video"
                )
        );

        assertContains(videoTask.getCommand(), "-c:v copy -c:a copy");
        assertEndsWith(videoTask.getFileName(), ".mkv");

        java.lang.System.out.println("audio_command=" + audioTask.getCommand());
        java.lang.System.out.println("video_command=" + videoTask.getCommand());
    }

    private static void assertContains(String value, String expected) {
        if (!value.contains(expected)) {
            throw new AssertionError("expected '" + expected + "' in '" + value + "'");
        }
    }

    private static void assertEndsWith(String value, String expected) {
        if (!value.endsWith(expected)) {
            throw new AssertionError("expected '" + value + "' to end with '" + expected + "'");
        }
    }

    private static class TestSupport extends FFMPEGSupport {

        private TestSupport(Context context) {
            super(context);
        }

        @Override
        protected Pattern[] getLocationPatterns() {
            return patterns(Pattern.compile(".*"));
        }

        @Override
        protected MediaResolver[] getMediaResolvers() {
            return new MediaResolver[0];
        }

        @Override
        protected String resolveContent(String location) {
            return "";
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
