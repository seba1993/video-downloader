package com.github.luischavez.videodownloader.streamclient.api.gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

public class VideoPanel extends JPanel implements KeyListener {

    private JPanel surfaceHolder;
    private Canvas surface;
    private JTextArea subtitleArea;

    private SubtitleListener subtitleListener;

    public VideoPanel() {
        initialize();
    }

    public Canvas getSurface() {
        return surface;
    }

    public JTextArea getSubtitleArea() {
        return subtitleArea;
    }

    public void setSubtitleListener(SubtitleListener subtitleListener) {
        this.subtitleListener = subtitleListener;
    }

    private void initialize() {
        /**
         * Create instances.
         */
        surfaceHolder = new JPanel();
        surface = new Canvas();
        subtitleArea = new JTextArea();

        /**
         * Set layout.
         */
        setLayout(new OverlayLayout(this));
        surfaceHolder.setLayout(new CardLayout());

        /**
         * Configure colors.
         */
        setBackground(Color.BLACK);
        surface.setBackground(Color.BLACK);
        subtitleArea.setBackground(new Color(50, 50, 50, 250));
        subtitleArea.setForeground(Color.WHITE);
        subtitleArea.setCaretColor(Color.WHITE);
        subtitleArea.setBorder(null);

        /**
         * Configure line wrap.
         */
        subtitleArea.setLineWrap(true);
        subtitleArea.setWrapStyleWord(true);
        subtitleArea.setRows(4);

        /**
         * Configure size.
         */
        Dimension subtitleDimension = new Dimension(800, 80);
        subtitleArea.setSize(subtitleDimension);
        subtitleArea.setPreferredSize(subtitleDimension);
        subtitleArea.setMinimumSize(subtitleDimension);
        subtitleArea.setMaximumSize(subtitleDimension);

        /**
         * Add components.
         */
        surfaceHolder.add(surface);
        subtitleArea.setAlignmentX(0.5f);
        subtitleArea.setAlignmentY(0.9f);
        surfaceHolder.setAlignmentX(0.5f);
        surfaceHolder.setAlignmentY(0.9f);
        add(subtitleArea);
        add(surfaceHolder);

        subtitleArea.setVisible(false);
        subtitleArea.addKeyListener(this);
    }

    @Override
    public void keyTyped(KeyEvent e) {
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_ENTER) {
            String subtitle = subtitleArea.getText();

            subtitleArea.setText("");
            subtitleArea.setVisible(false);

            if (subtitleListener != null) {
                subtitleListener.onSubtitleFinished(subtitle);
            }
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
    }

    interface SubtitleListener {

        void onSubtitleFinished(String subtitle);
    }
}
