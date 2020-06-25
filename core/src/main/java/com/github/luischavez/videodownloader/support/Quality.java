package com.github.luischavez.videodownloader.support;

public interface Quality extends Comparable<Quality> {

    Type getType();

    enum Type {
        LOW(0), MEDIUM(1), HIGH(2);

        int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
}
