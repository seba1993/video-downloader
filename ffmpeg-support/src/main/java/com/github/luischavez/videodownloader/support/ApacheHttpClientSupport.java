package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

public abstract class ApacheHttpClientSupport<M extends Media> extends BaseSupport<M> {

    private HttpClient httpClient;

    public ApacheHttpClientSupport(Context context) {
        super(context);

        httpClient = buildHttpClient();
    }

    protected HttpClient buildHttpClient() {
        return HttpClientBuilder.create().build();
    }

    protected HttpGet buildGet(String location) {
        return new HttpGet(location);
    }

    @Override
    public String getContent(String location) throws MediaOfflineException {
        try {
            HttpGet httpGet = buildGet(location);

            HttpResponse httpResponse = httpClient.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (!String.valueOf(statusCode).startsWith("2")) {
                throw new MediaOfflineException("invalid request " + location + " status " + statusCode);
            }

            return EntityUtils.toString(httpResponse.getEntity());
        } catch (IOException ex) {
            throw new MediaOfflineException("request failed, may be the site is offline or you don't have internet connection " + location, ex);
        }
    }
}
