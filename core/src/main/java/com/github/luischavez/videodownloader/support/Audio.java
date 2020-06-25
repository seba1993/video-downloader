package com.github.luischavez.videodownloader.support;

public class Audio implements Media {

    private final String info;
    private final String url;
    private final AudioQuality quality;
    private final String codec;

    public Audio(String info, String url, AudioQuality quality, String codec) {
        this.info = info;
        this.url = url;
        this.quality = quality;
        this.codec = codec;
    }

    @Override
    public String getInfo() {
        return info;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public Quality getQuality() {
        return quality;
    }

    @Override
    public String getCodec() {
        return codec;
    }

    @Override
    public int compareTo(Media media) {
        return quality.compareTo(media.getQuality());
    }

    public static class AudioQuality implements Quality {

        private final Type type;

        public AudioQuality(Type type) {
            this.type = type;
        }

        @Override
        public Type getType() {
            return type;
        }

        @Override
        public int compareTo(Quality quality) {
            return Integer.compare(type.getValue(), quality.getType().getValue());
        }

        @Override
        public String toString() {
            return type.name();
        }
    }
}
