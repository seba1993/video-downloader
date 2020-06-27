package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.IOException;

public abstract class ApacheHttpClientSupport<M extends Media> extends BaseSupport<M> {

    protected HttpClient httpClient;

    public ApacheHttpClientSupport(Context context) {
        super(context);

        httpClient = buildHttpClient();
    }

    protected HttpClient buildHttpClient() {
        try {
            SSLContext sslContext = SSLContexts.custom()
                    .loadTrustMaterial((chain, authType) -> true).build();

            SSLConnectionSocketFactory sslConnectionSocketFactory =
                    new SSLConnectionSocketFactory(sslContext, new String[]
                            {"SSLv2Hello", "SSLv3", "TLSv1","TLSv1.1", "TLSv1.2" }, null,
                            NoopHostnameVerifier.INSTANCE);

            return HttpClients.custom()
                    .setSSLSocketFactory(sslConnectionSocketFactory)
                    .build();
        } catch (Exception ex) {
            return HttpClientBuilder.create().build();
        }
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
