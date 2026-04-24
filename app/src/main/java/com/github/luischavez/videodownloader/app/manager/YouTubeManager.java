package com.github.luischavez.videodownloader.app.manager;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.task.YouTubeTask;
import com.github.luischavez.videodownloader.manager.BaseManager;
import com.github.luischavez.videodownloader.manager.ManagerStateException;
import com.google.inject.Inject;

public class YouTubeManager extends BaseManager {

    private YouTubeTask task;

    @Inject
    public YouTubeManager(Context context) {
        super(context);

        setExecutionInterval(1_000 * 60 * 5); // each 5 minutes
    }

    public void restart() {
        try {
            task.kill();
            task.start();
        } catch (Exception ex) {
            getWrappedContext().error(YouTubeManager.class, ex.getMessage(), ex);
        }
    }

    @Override
    public void stop() throws ManagerStateException {
        try {
            task.kill();
        } catch (Exception ex) {
            getWrappedContext().error(YouTubeManager.class, ex.getMessage(), ex);
        }

        super.stop();
    }

    @Override
    protected boolean doWork() throws Exception {
        if (task == null) {
            task = new YouTubeTask(getWrappedContext());
            task.setDebugger(message -> {
                getWrappedContext().debug(YouTubeManager.class, message);
            });
        }

        if (task.isRunning()) {
            return false;
        }

        task.start();

        return false;
    }
}
