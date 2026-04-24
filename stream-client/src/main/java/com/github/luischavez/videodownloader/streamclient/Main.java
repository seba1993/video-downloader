package com.github.luischavez.videodownloader.streamclient;

import com.github.luischavez.videodownloader.streamclient.api.RemoteService;
import com.github.luischavez.videodownloader.streamclient.api.gui.LoginDialog;
import com.github.luischavez.videodownloader.streamclient.api.gui.MainFrame;
import com.github.luischavez.videodownloader.streamclient.api.model.JsonResponse;
import com.github.luischavez.videodownloader.streamclient.api.model.User;
import com.github.luischavez.videodownloader.streamclient.api.model.Video;
import okhttp3.OkHttpClient;
import org.pushingpixels.substance.api.skin.*;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import javax.swing.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

public class Main {

    private static RemoteService remoteService;

    public static Properties readProperties() {
        Properties properties = new Properties();

        String workingDir = System.getProperty("user.dir");
        try (FileInputStream fileInputStream = new FileInputStream(new File(workingDir, "config.props"))) {
            properties.load(fileInputStream);
        } catch (Exception ex) {
            // HANDLE.
        }

        return properties;
    }

    public static void storeProperties(Properties properties) {
        String workingDir = System.getProperty("user.dir");
        try (FileOutputStream fileOutputStream = new FileOutputStream(new File(workingDir, "config.props"))) {
            properties.store(fileOutputStream, "");
        } catch (Exception ex) {
            // HANDLE.
        }
    }

    public static RemoteService remoteService() {
        Properties properties = readProperties();

        String apiServer = properties.getOrDefault("api_server", "http://stream-admin.test").toString();

        if (!apiServer.isEmpty() && !apiServer.endsWith("/")) {
            apiServer += "/";
        }

        apiServer += "api/";

        if (remoteService == null) {
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(apiServer)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(new OkHttpClient.Builder().build())
                    .build();

            remoteService = retrofit.create(RemoteService.class);
        }

        return remoteService;
    }

    public static User getCurrentUser() {
        remoteService();

        Properties properties = readProperties();

        String authCode = properties.getOrDefault("auth_code", "").toString();

        System.out.println(authCode);

        try {
            Response<User> userResponse = remoteService.user(authCode).execute();

            if (userResponse.isSuccessful()) {
                return userResponse.body();
            }

            System.out.println(new String(userResponse.errorBody().bytes()));
        } catch (Exception ex) {
            // HANDLE.
            ex.printStackTrace();
        }

        return null;
    }

    public static List<Video> getVideos(String status) {
        remoteService();

        Properties properties = readProperties();

        String authCode = properties.getOrDefault("auth_code", "").toString();

        try {
            Response<List<Video>> videoResponse = remoteService.video(authCode, status).execute();

            if (videoResponse.isSuccessful()) {
                return videoResponse.body();
            }

            System.out.println(new String(videoResponse.errorBody().bytes()));
        } catch (Exception ex) {
            // HANDLE.
            ex.printStackTrace();
        }

        return Collections.emptyList();
    }

    public static Video setStatus(Video video, String status) {
        remoteService();

        Properties properties = readProperties();

        String authCode = properties.getOrDefault("auth_code", "").toString();

        try {
            Response<Video> changeStatusResponse = remoteService.changeStatus(authCode, video.getId(), status).execute();

            if (changeStatusResponse.isSuccessful()) {
                return changeStatusResponse.body();
            }

            System.out.println(new String(changeStatusResponse.errorBody().bytes()));
        } catch (Exception ex) {
            // HANDLE.
            ex.printStackTrace();
        }

        return null;
    }

    public static Video save(Video video, String srt, String transcription) {
        remoteService();

        Properties properties = readProperties();

        String authCode = properties.getOrDefault("auth_code", "").toString();

        try {
            Response<Video> updateResponse = remoteService.update(authCode, video.getId(), srt, transcription).execute();

            if (updateResponse.isSuccessful()) {
                return updateResponse.body();
            }

            System.out.println(new String(updateResponse.errorBody().bytes()));
        } catch (Exception ex) {
            // HANDLE.
            ex.printStackTrace();
        }

        return null;
    }

    public static void main(String... args) throws Exception {
        Properties properties = readProperties();

        boolean notLogged = !properties.containsKey("api_server") || getCurrentUser() == null;

        if (notLogged) {
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(new SubstanceBusinessLookAndFeel());
                } catch (Exception ex) {
                    // IGNORE
                }

                JFrame.setDefaultLookAndFeelDecorated(true);

                new LoginDialog(null).setVisible(true);
            });
        } else {
            SwingUtilities.invokeLater(() -> {
                try {
                    UIManager.setLookAndFeel(new SubstanceCeruleanLookAndFeel());
                } catch (Exception ex) {
                    // IGNORE
                }

                JFrame.setDefaultLookAndFeelDecorated(true);

                new MainFrame().setVisible(true);
            });
        }
    }
}
