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

    public void put(String key, String value) {
        content.put(key, value);
    }

    public void remove(String key) {
        content.remove(key);
    }

    public String get(String key) {
        return content.get(key);
    }

}

