package io.medev.httpclient;

import io.medev.httpclient.request.Request;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

public class Endpoint {

    public static final String HTTP = "http";
    public static final String HTTPS = "https";

    private final URL url;

    public Endpoint(URL url) {
        this.url = Objects.requireNonNull(url);
    }

    public static Endpoint forHost(String protocol, String host) {
        URL url;
        try {
            url = new URL(protocol, host, "");
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        return new Endpoint(url);
    }

    public static Endpoint forHost(String host) {
        return forHost(HTTPS, host);
    }

    public URL getURL() {
        return this.url;
    }

    public Endpoint resolve(Charset charset, String... children) {
        String file = this.url.getPath();
        String query = this.url.getQuery();

        if (!file.startsWith("/")) {
            file += "/";
        }

        file += Arrays.stream(children)
                .map((v) -> encode(v, charset))
                .collect(Collectors.joining("/"));

        if (query != null) {
            file += "?" + query;
        }

        URL url;
        try {
            url = new URL(this.url.getProtocol(), this.url.getHost(), this.url.getPort(), file);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        return new Endpoint(url);
    }

    public Endpoint resolve(String... children) {
        return resolve(Charset.defaultCharset(), children);
    }

    public Request.Builder<?> buildHeadRequest() {
        return new Request.BuilderImpl(this, RequestMethod.HEAD);
    }

    public Request.Builder<?> buildGetRequest() {
        return new Request.BuilderImpl(this, RequestMethod.GET);
    }

    public Request.BuilderWithBody<?> buildPostRequest() {
        return new Request.BuilderWithBodyImpl(this, RequestMethod.POST);
    }

    public Request.BuilderWithBody<?> buildPutRequest() {
        return new Request.BuilderWithBodyImpl(this, RequestMethod.PUT);
    }

    public Request.BuilderWithBody<?> buildPatchRequest() {
        return new Request.BuilderWithBodyImpl(this, RequestMethod.PATCH);
    }

    public Request.Builder<?> buildDeleteRequest() {
        return new Request.BuilderImpl(this, RequestMethod.DELETE);
    }

    private static String encode(String str, Charset charset) {
        try {
            return URLEncoder.encode(str, charset.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}
