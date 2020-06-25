package com.github.luischavez.videodownloader.support;

public class Video implements Media {

    private final String info;
    private final String url;
    private final VideoQuality quality;
    private final String codec;

    public Video(String info, String url, VideoQuality quality, String codec) {
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

    public static class VideoQuality implements Quality {

        private final Type type;
        private final int width;
        private final int height;
        private final int bandwidth;

        public VideoQuality(Type type, int width, int height, int bandwidth) {
            this.type = type;
            this.width = width;
            this.height = height;
            this.bandwidth = bandwidth;
        }

        @Override
        public Type getType() {
            return type;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getBandwidth() {
            return bandwidth;
        }

        @Override
        public int compareTo(Quality quality) {
            if (quality instanceof VideoQuality) {
                return Integer.compare(getHeight(), VideoQuality.class.cast(quality).getHeight());
            }

            return quality.compareTo(this);
        }

        @Override
        public String toString() {
            return String.format("%dx%d", width, height);
        }
    }
}
