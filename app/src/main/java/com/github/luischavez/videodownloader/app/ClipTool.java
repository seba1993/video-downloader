package com.github.luischavez.videodownloader.app;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.gui.ClipFrame;
import com.github.luischavez.videodownloader.task.DefaultTaskManager;
import com.github.luischavez.videodownloader.task.TaskManager;
import org.pushingpixels.substance.api.skin.SubstanceNightShadeLookAndFeel;

import javax.swing.*;

public class ClipTool {

    private final DummyContext context;

    private ClipFrame clipFrame;

    public ClipTool() {
        context = DummyContext.instance();
    }

    public void start() {
        context.getSystem().getDependencyInjection().configure(dependencyRegister -> {
            dependencyRegister.bindClass(Context.class).toInstance(context);
            dependencyRegister.bindClass(TaskManager.class).toClass(DefaultTaskManager.class);
        });

        context.getSystem().registerManager(TaskManager.class);
        context.getSystem().startAllManagers(true);

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(new SubstanceNightShadeLookAndFeel());
            } catch (Exception ex) {
                // IGNORE
            }

            clipFrame = new ClipFrame(context);
            clipFrame.setVisible(true);
        });
    }

    public static void main(String... args) {
        new ClipTool().start();
    }
}
