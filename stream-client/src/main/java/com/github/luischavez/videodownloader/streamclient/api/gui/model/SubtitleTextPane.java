package com.github.luischavez.videodownloader.streamclient.api.gui.model;

import javax.swing.*;
import javax.swing.text.*;
import java.util.ArrayList;
import java.util.List;

public class SubtitleTextPane extends JTextPane {

    private List<Subtitle> subtitles;

    public SubtitleTextPane() {
        subtitles = new ArrayList<>();
    }

    public void addAll(List<Subtitle> subtitles) {
        this.subtitles.addAll(subtitles);
    }

    @Override
    public Document getDocument() {
        return super.getDocument();
    }
}
