package com.krokochik.ideasforummfa.model;

import java.util.HashMap;

import lombok.Data;

@Data
public class Response {
    short code;
    private HashMap<String, Object> body;

    public Object get(String key) {
        return body.get(key);
    }

    public Response put(String key, Object value) {
        body.put(key, value);
        return this;
    }

    public Object remove(String key) {
        return body.remove(key);
    }
}
