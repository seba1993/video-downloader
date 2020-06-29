import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.*;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;

public class WebRadAudioResolver extends BaseMediaResolver {

    public WebRadAudioResolver(Context context) {
        super(context);
    }

    @Override
    protected Media parseMediaDescriptor(String location, String parentLink, MediaDescriptor mediaDescriptor, MediaLinkBuilder mediaLinkBuilder) {
        final String info = mediaDescriptor.get("info");
        final String url = mediaDescriptor.get("url");
        final String type = mediaDescriptor.get("type");
        final String mime = mediaDescriptor.get("mime");

        return new Audio(info, url, new Audio.AudioQuality(Quality.Type.HIGH), type, true);
    }

    @Override
    protected List<MediaDescriptor> getMediaDescriptors(String location, String parentLink, String content) {
        ArrayList<MediaDescriptor> descriptors = new ArrayList<>();

        final List<Stream> streams = new GsonBuilder().create().fromJson(content, new TypeToken<List<Stream>>(){}.getType());

        for (Stream stream : streams) {
            if (!stream.mediaType.toUpperCase().contains("HLS") && !stream.url.toUpperCase().contains(".M3U8")) {
                continue;
            }

            MediaDescriptor descriptor = new MediaDescriptor();
            descriptor.put("info", stream.toString());
            descriptor.put("url", stream.url);
            descriptor.put("type", stream.mediaType);
            descriptor.put("mime", stream.mime);

            descriptors.add(descriptor);
        }

        return descriptors;
    }

    private static class Stream {

        private int id;
        private boolean isContainer;
        private String mediaType;
        private String mime;
        private int posX;
        private int posY;
        private String url;

        @Override
        public String toString() {
            return "Stream {" +
                   "id=" + id +
                   ", isContainer=" + isContainer +
                   ", mediaType='" + mediaType + '\'' +
                   ", mime='" + mime + '\'' +
                   ", posX=" + posX +
                   ", posY=" + posY +
                   ", url='" + url + '\'' +
                   '}';
        }
    }
}
