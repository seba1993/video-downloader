package com.github.luischavez.videodownloader.app.manager;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.app.configuration.MonitorConfiguration;
import com.github.luischavez.videodownloader.app.configuration.StreamConfiguration;
import com.github.luischavez.videodownloader.app.gui.MainFrame;
import com.github.luischavez.videodownloader.app.gui.model.StreamTableModel;
import com.github.luischavez.videodownloader.configuration.ConfigurationManager;
import com.github.luischavez.videodownloader.manager.BaseManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.inject.Inject;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class MonitorManager extends BaseManager {

    private final HttpClient httpClient = HttpClientBuilder.create().build();
    private final Gson gson = new GsonBuilder().create();

    private MainFrame mainFrame;

    @Inject
    public MonitorManager(Context context) {
        super(context);

        setExecutionInterval(5_000L);
    }

    public MainFrame getMainFrame() {
        return mainFrame;
    }

    public void setMainFrame(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
    }

    @Override
    protected boolean doWork() throws Exception {
        final MonitorConfiguration monitorConfiguration
                = getSystem().getManager(ConfigurationManager.class).get(MonitorConfiguration.class);

        if (monitorConfiguration == null || !monitorConfiguration.isEnabled()) return true;

        if (mainFrame == null) return true;

        StreamTableModel model = mainFrame.streamTableModel;

        if (model == null) return true;

        setExecutionInterval(monitorConfiguration.getRefreshInterval() * 1_000L);

        ArrayList<Map<String, Object>> streams = new ArrayList<>();

        for (int i = 0; i < model.getRowCount(); i++) {
            HashMap<String, Object> stream = new HashMap<>();

            StreamConfiguration streamConfiguration = model.getConfigurationAt(i);

            stream.put("alias", streamConfiguration.getAlias());
            stream.put("country", streamConfiguration.getCountry());
            stream.put("status", model.getValueAt(i, 3).toString());
            stream.put("percent", Float.valueOf(model.getValueAt(i, 4).toString()).intValue());
            stream.put("time", model.getValueAt(i, 5).toString());
            stream.put("url", streamConfiguration.getUrl());

            streams.add(stream);
        }

        String json = gson.toJson(streams);

        HttpPost post = new HttpPost(monitorConfiguration.getUrl());

        try {
            post.setEntity(new UrlEncodedFormEntity(
                    Arrays.asList(
                            new BasicNameValuePair("code", monitorConfiguration.getCode()),
                            new BasicNameValuePair("streams", json))));

            httpClient.execute(post);
        } finally {
            post.releaseConnection();
        }

        return true;
    }
}
