package com.krokochik.ideasforummfa.model;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.util.HashMap;

@Data
@ToString
@RequiredArgsConstructor
public class Message {

    @NonNull
    HashMap<String, String> content;

    public Message() {
        content = new HashMap<>();
    }

    public Message(Object key, Object value) {
        content = new HashMap<String, String>() {{
            put(key.toString(), value.toString());
        }};
    }

    public void put(Object key, Object value) {
        content.put(key.toString(), value.toString());
    }

    public void remove(Object key) {
        content.remove(key.toString());
    }

    public String get(Object key) {
        return content.get(key.toString());
    }

}

