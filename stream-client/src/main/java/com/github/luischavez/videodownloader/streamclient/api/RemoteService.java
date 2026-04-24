package com.github.luischavez.videodownloader.streamclient.api;

import com.github.luischavez.videodownloader.streamclient.api.model.User;
import com.github.luischavez.videodownloader.streamclient.api.model.Video;
import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;

public interface RemoteService {

    @GET("client/user")
    Call<User> user(@Header("auth-code") String authCode);

    @GET("client/video/{status}")
    Call<List<Video>> video(@Header("auth-code") String authCode, @Path("status") String status);

    @FormUrlEncoded
    @POST("client/update/{video}")
    Call<Video> update(@Header("auth-code") String authCode, @Path("video") long videoId, @Field("subtitle") String subtitle, @Field("transcription") String transcription);

    @POST("client/change/status/{video}/{status}")
    Call<Video> changeStatus(@Header("auth-code") String authCode, @Path("video") long videoId, @Path("status") String status);
}
