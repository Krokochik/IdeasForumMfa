package com.krokochik.ideasforummfa.model;

import java.util.HashMap;

import lombok.Data;

@Data
public class Response {
    int code;
    private HashMap<String, String> body;

    public Object get(String key) {
        return body.get(key);
    }

    public Response put(String key, String value) {
        body.put(key, value);
        return this;
    }

    public Object remove(String key) {
        return body.remove(key);
    }
}
