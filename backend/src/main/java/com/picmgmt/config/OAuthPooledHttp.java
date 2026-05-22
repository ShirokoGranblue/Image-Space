package com.picmgmt.config;

import com.xkcoding.http.HttpUtil;
import com.xkcoding.http.config.HttpConfig;
import com.xkcoding.http.support.AbstractHttp;
import com.xkcoding.http.support.HttpHeader;
import com.xkcoding.http.support.SimpleHttpResponse;
import jakarta.annotation.PostConstruct;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Component
public class OAuthPooledHttp extends AbstractHttp {

    private static OkHttpClient sharedClient;

    public OAuthPooledHttp(
            @Value("${oauth.proxy.enabled:false}") boolean proxyEnabled,
            @Value("${oauth.proxy.host:}") String proxyHost,
            @Value("${oauth.proxy.port:0}") int proxyPort,
            @Value("${oauth.timeout:30000}") int timeout) {
        super(HttpConfig.builder().timeout(timeout).build());

        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(timeout, TimeUnit.MILLISECONDS)
                .readTimeout(timeout, TimeUnit.MILLISECONDS)
                .connectionPool(new ConnectionPool(5, 5, TimeUnit.MINUTES))
                .protocols(List.of(Protocol.HTTP_2, Protocol.HTTP_1_1));

        if (proxyEnabled && proxyHost != null && !proxyHost.isEmpty()) {
            builder.proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort)));
        }

        this.sharedClient = builder.build();
    }

    @PostConstruct
    public void register() {
        HttpUtil.setHttp(this);
    }

    public static OkHttpClient getClient() {
        return sharedClient;
    }

    @Override
    public SimpleHttpResponse get(String url) {
        return exec(buildRequest(url).get().build());
    }

    @Override
    public SimpleHttpResponse get(String url, Map<String, String> headers, boolean followRedirects) {
        Request.Builder rb = buildRequest(url);
        addHeaders(rb, headers);
        return exec(rb.get().build());
    }

    @Override
    public SimpleHttpResponse get(String url, Map<String, String> headers, HttpHeader httpHeader, boolean followRedirects) {
        Request.Builder rb = buildRequest(url);
        addHeaders(rb, headers);
        addHttpHeader(rb, httpHeader);
        return exec(rb.get().build());
    }

    @Override
    public SimpleHttpResponse post(String url) {
        return exec(buildRequest(url).post(RequestBody.create("", null)).build());
    }

    @Override
    public SimpleHttpResponse post(String url, String body) {
        MediaType mt = body != null && body.trim().startsWith("{") ?
                MediaType.parse("application/json; charset=utf-8") : null;
        return exec(buildRequest(url).post(RequestBody.create(body, mt)).build());
    }

    @Override
    public SimpleHttpResponse post(String url, String body, HttpHeader httpHeader) {
        MediaType mt = body != null && body.trim().startsWith("{") ?
                MediaType.parse("application/json; charset=utf-8") : null;
        Request.Builder rb = buildRequest(url).post(RequestBody.create(body, mt));
        addHttpHeader(rb, httpHeader);
        return exec(rb.build());
    }

    @Override
    public SimpleHttpResponse post(String url, Map<String, String> params, boolean followRedirects) {
        FormBody.Builder fb = new FormBody.Builder();
        if (params != null) params.forEach(fb::add);
        return exec(buildRequest(url).post(fb.build()).build());
    }

    @Override
    public SimpleHttpResponse post(String url, Map<String, String> params, HttpHeader httpHeader, boolean followRedirects) {
        FormBody.Builder fb = new FormBody.Builder();
        if (params != null) params.forEach(fb::add);
        Request.Builder rb = buildRequest(url).post(fb.build());
        addHttpHeader(rb, httpHeader);
        return exec(rb.build());
    }

    private Request.Builder buildRequest(String url) {
        return new Request.Builder().url(url);
    }

    private void addHeaders(Request.Builder rb, Map<String, String> headers) {
        if (headers != null) headers.forEach(rb::addHeader);
    }

    private void addHttpHeader(Request.Builder rb, HttpHeader header) {
        if (header != null) {
            header.getHeaders().forEach(rb::addHeader);
        }
    }

    private SimpleHttpResponse exec(okhttp3.Request request) {
        SimpleHttpResponse resp = new SimpleHttpResponse();
        try (Response okResp = sharedClient.newCall(request).execute()) {
            resp.setSuccess(okResp.isSuccessful());
            resp.setCode(okResp.code());
            ResponseBody body = okResp.body();
            resp.setBody(body != null ? body.string() : "");
            Map<String, List<String>> headers = new HashMap<>();
            okResp.headers().toMultimap().forEach((k, v) -> headers.put(k, v));
            resp.setHeaders(headers);
            resp.setError(okResp.isSuccessful() ? "" : okResp.message());
        } catch (IOException e) {
            resp.setSuccess(false);
            resp.setCode(0);
            resp.setError(e.getMessage());
            resp.setBody("");
        }
        return resp;
    }
}
