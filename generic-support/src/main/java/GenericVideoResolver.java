import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.support.M3U8VideoResolver;
import com.github.luischavez.videodownloader.support.MediaDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GenericVideoResolver extends M3U8VideoResolver {

    private static final Pattern[] M3U8_OPTION_PATTERNS = {
            Pattern.compile("(?<descriptor>(?<info>#EXT.*BANDWIDTH=(?<bandwidth>\\d+).*RESOLUTION=(?<resolution>(?<width>\\d+)x(?<height>\\d+)).*)\\s*\\n\\s*(?<m3u8>.+))", Pattern.MULTILINE),
    };

    public GenericVideoResolver(Context context) {
        super(context);
    }

    @Override
    protected List<MediaDescriptor> getMediaDescriptors(String location, String parentLink, String content) {
        if (!location.contains("rainews.it")) {
            return super.getMediaDescriptors(location, parentLink, content);
        }

        if (content.toUpperCase().contains("#EXTINF")) {
            MediaDescriptor descriptor = new MediaDescriptor();
            descriptor.put("info", "");
            descriptor.put("m3u8", parentLink);
            descriptor.put("bandwidth", "0");
            descriptor.put("width", "0");
            descriptor.put("height", "0");

            return Collections.singletonList(descriptor);
        }

        ArrayList<MediaDescriptor> descriptors = new ArrayList<>();

        for (Pattern pattern : M3U8_OPTION_PATTERNS) {
            Matcher matcher = pattern.matcher(content);

            while (matcher.find()) {
                final String info = matcher.group("info");
                final String m3u8 = matcher.group("m3u8");
                final String bandwidth = matcher.group("bandwidth");

                String width = "0";
                String height = "0";

                try {
                    width = matcher.group("width");
                    height = matcher.group("height");
                } catch (Exception ex) {
                    // NO RESOLUTION DATA FOUND.
                }

                MediaDescriptor descriptor = new MediaDescriptor();
                descriptor.put("info", info);
                descriptor.put("m3u8", m3u8);
                descriptor.put("bandwidth", bandwidth);
                descriptor.put("width", width);
                descriptor.put("height", height);

                descriptors.add(descriptor);
            }

            if (!descriptors.isEmpty()) break;
        }

        return descriptors;
    }
}
