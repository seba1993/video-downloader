import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.Media;
import com.github.luischavez.videodownloader.task.LocalProcessTask;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class YouTubeDLTask extends LocalProcessTask {

    private final Media media;
    private final String executable;
    private final List<String> arguments;
    private final String destinationPath;

    public YouTubeDLTask(Context context, Media media, String executable, List<String> arguments, String destinationPath) {
        super(context);

        this.media = media;
        this.executable = executable;
        this.arguments = arguments;
        this.destinationPath = destinationPath;
    }

    @Override
    public String details() {
        return media.getQuality().toString();
    }

    protected String getDestinationPath() {
        return destinationPath;
    }

    protected List<String> getArguments() {
        return arguments;
    }

    @Override
    protected long getStopTimeoutMillis() {
        return 15000L;
    }

    @Override
    protected ProcessBuilder buildCommand() throws Exception {
        File folderFile = new File(getDestinationPath());
        if (!folderFile.exists()) Files.createDirectories(folderFile.toPath());

        final String workingDirectory = getWrappedContext().buildPath(System.getProperty("user.dir"), "youtube");

        ArrayList<String> command = new ArrayList<>();
        command.add(new File(workingDirectory, executable).getPath());
        command.addAll(getArguments());

        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command.toArray(new String[0]));
        processBuilder.directory(new File(workingDirectory));

        return processBuilder;
    }
}
