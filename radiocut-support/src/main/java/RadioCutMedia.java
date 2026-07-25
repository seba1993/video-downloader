import com.github.luischavez.videodownloader.support.Audio;
import com.github.luischavez.videodownloader.support.Quality;

public class RadioCutMedia extends Audio {

    private final String station;
    private final String audioBaseUrl;
    private final double startTimestamp;

    public RadioCutMedia(String location, String station, String audioBaseUrl, double startTimestamp) {
        super(
                "RadioCut MP3 chunks for " + station,
                location,
                new AudioQuality(Quality.Type.HIGH),
                "mp3",
                true
        );

        this.station = station;
        this.audioBaseUrl = audioBaseUrl;
        this.startTimestamp = startTimestamp;
    }

    public String getStation() {
        return station;
    }

    public String getAudioBaseUrl() {
        return audioBaseUrl;
    }

    public double getStartTimestamp() {
        return startTimestamp;
    }
}
