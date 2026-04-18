package com.github.luischavez.videodownloader.support;

import com.github.luischavez.videodownloader.Context;
import com.github.luischavez.videodownloader.ContextWrapper;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public abstract class BaseSupport extends ContextWrapper implements Support, MediaLinkBuilder {

    protected final HttpClient httpClient;

    public BaseSupport(Context context) {
        super(context);

        httpClient = buildHttpClient();
    }

    protected HttpClient buildHttpClient() {
        return HttpClientBuilder.create().build();
    }

    protected HttpGet buildGet(String location) {
        return new HttpGet(LocationRequestUtils.sanitize(location));
    }

    protected String getContent(String url, Map<String, String> headers) throws MediaOfflineException {
        try {
            HttpGet httpGet = buildGet(url);
            Map<String, String> mergedHeaders = LocationRequestUtils.mergeHeaders(headers, LocationRequestUtils.extractHeaders(url));

            if (mergedHeaders != null) {
                mergedHeaders.entrySet().stream()
                        .forEach(entry -> httpGet.addHeader(entry.getKey(), entry.getValue()));
            }

            HttpResponse httpResponse = httpClient.execute(httpGet);
            int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (!String.valueOf(statusCode).startsWith("2")) {
                throw new MediaOfflineException("invalid request " + url + " status " + statusCode);
            }

            return EntityUtils.toString(httpResponse.getEntity());
        } catch (Exception ex) {
            throw new MediaOfflineException("request failed, may be the site is offline or you don't have internet connection " + url, ex);
        }
    }

    protected String getContent(String url) throws MediaOfflineException {
        return getContent(url, null);
    }

    protected abstract Pattern[] getLocationPatterns();

    protected abstract MediaResolver[] getMediaResolvers();

    protected abstract String[] getLinks(String location) throws MediaOfflineException;

    protected Map<String, String> resolveLinkHeaders(String location, String link) {
        return Map.of();
    }

    @Override
    public String buildMediaLink(String location, String parentLink, String mediaLink) {
        return mediaLink;
    }

    protected Pattern[] patterns(Pattern... patterns) {
        return patterns;
    }

    protected MediaResolver[] resolvers(MediaResolver... resolvers) {
        return resolvers;
    }

    @Override
    public boolean canHandle(String location) {
        final Pattern[] locationPatterns = getLocationPatterns();

        if (locationPatterns == null || locationPatterns.length == 0) return false;

        for (Pattern locationPattern : locationPatterns) {
            if (locationPattern.matcher(location).matches()) return true;
        }

        return false;
    }

    @Override
    public List<Media> getMedia(String location) throws MediaNotFoundException, MediaOfflineException {
        final String[] links = getLinks(location);

        if (links == null || links.length == 0) throw new MediaOfflineException(String.format("media offline or connection issue %s", location));

        final MediaResolver[] mediaResolvers = getMediaResolvers();

        if (mediaResolvers == null || mediaResolvers.length == 0) throw new MediaNotFoundException(String.format("media resolvers not found for location %s", location));

        ArrayList<Media> allMedias = new ArrayList<>();

        for (String link : links) {
            final Map<String, String> requestHeaders = LocationRequestUtils.mergeHeaders(
                    resolveLinkHeaders(location, link),
                    LocationRequestUtils.extractHeaders(location));
            final String content = getContent(link, requestHeaders);

            for (MediaResolver mediaResolver : mediaResolvers) {
                List<Media> medias = mediaResolver.findMedia(location, link, content, this::buildMediaLink);

                if (medias == null || medias.isEmpty()) continue;

                allMedias.addAll(medias);
            }
        }

        if (allMedias.isEmpty()) throw new MediaNotFoundException(String.format("media not found for location %s", location));

        return allMedias;
    }
}
