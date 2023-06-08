package com.krokochik.ideasforummfa.model;

import java.util.HashMap;

import lombok.Data;
import lombok.NonNull;

@Data
public class Request {
    public enum Method {
        GET,
        POST
    }

    private HashMap<String, String> body;

    @NonNull
    Method method;
    String endpoint = "";

    public Request(@NonNull Method method, String endpoint, @NonNull HashMap<String, String> body) {
        this.body = body;
        this.method = method;
        this.endpoint = endpoint;
    }

    public Request( @NonNull Method method, @NonNull HashMap<String, String> body) {
        this.body = body;
        this.method = method;
    }

    public Request(@NonNull Method method, String endpoint) {
        this.method = method;
        this.endpoint = endpoint;
    }

    public Request(@NonNull Method method) {
        this.method = method;
    }

    public Object get(String key) {
        return body.get(key);
    }

    public Request put(String key, String value) {
        body.put(key, value);
        return this;
    }

    public Object remove(String key) {
        return body.remove(key);
    }
}
