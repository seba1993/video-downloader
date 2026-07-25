import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.Media;
import com.github.luischavez.videodownloader.task.Task;

import java.io.File;
import java.util.List;
import java.util.Map;

public class RadioCutSupportSmokeTest {

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("usage: RadioCutSupportSmokeTest URL OUTPUT_DIRECTORY");
        }

        String location = args[0];
        File outputDirectory = new File(args[1]);
        outputDirectory.mkdirs();

        RadioCutSupport support = new RadioCutSupport(new TestContext());
        assertSupported(support, "https://radiocut.fm/radiostation/radio10/");
        assertSupported(support, "https://radiocut.fm/radiostation/radio10/listen/");
        List<Media> media = support.getMedia(location);

        if (media.size() != 1 || !(media.get(0) instanceof RadioCutMedia)) {
            throw new IllegalStateException("RadioCut media was not resolved");
        }

        Task task = support.generateTask(
                location,
                media.get(0),
                Map.of(
                        "base_file_name", "radiocut-smoke-test",
                        "destination_path", outputDirectory.getAbsolutePath()
                )
        );

        task.start();
        Thread.sleep(20_000L);
        task.kill();

        File output = new File(outputDirectory, "radiocut-smoke-test.mp3");
        if (!output.isFile() || output.length() == 0L) {
            throw new IllegalStateException("RadioCut task did not write audio");
        }

        java.lang.System.out.println("support=" + support.getClass().getName());
        java.lang.System.out.println("media=" + media.get(0));
        java.lang.System.out.println("output=" + output.getAbsolutePath());
        java.lang.System.out.println("bytes=" + output.length());
    }

    private static void assertSupported(RadioCutSupport support, String location) {
        if (!support.canHandle(location)) {
            throw new AssertionError("RadioCut URL was not accepted: " + location);
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
