package com.krokochik.ideasforummfa.model;

import org.jetbrains.annotations.Nullable;

import java.net.URL;
import java.util.HashMap;

import lombok.Data;
import lombok.NonNull;

@Data
public class Request {

    public enum Method {
        GET,
        POST
    }

    @Nullable
    HashMap<String, String> body;
    Method method = Method.GET;
    URL url;

    public Request(@NonNull Method method, URL endpoint, @Nullable HashMap<String, String> body) {
        this.body = body;
        this.method = method;
        this.url = endpoint;
    }

    public Request(@NonNull Method method, @Nullable HashMap<String, String> body) {
        this.body = body;
        this.method = method;
    }

    public Request(@NonNull Method method, URL endpoint) {
        this.method = method;
        this.url = endpoint;
    }

    public Request(@NonNull Method method) {
        this.method = method;
    }

    public Request() {}

    public Object get(String key) {
        return body.get(key);
    }

    public Request put(String key, String value) {
        if (body == null)
            body = new HashMap<>();
        body.put(key, value);
        return this;
    }

    public Object remove(String key) {
        return body.remove(key);
    }
}
