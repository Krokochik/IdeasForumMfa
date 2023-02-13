package com.krokochik.ideasforummfa.service;

import com.google.gson.Gson;
import com.krokochik.ideasforummfa.model.Message;

import jakarta.websocket.Decoder;
import jakarta.websocket.EndpointConfig;


public class MessageDecoder implements Decoder.Text<Message> {

    private static final Gson gson = new Gson();

    @Override
    public Message decode(String s) {
        return gson.fromJson(s, Message.class);
    }

    @Override
    public boolean willDecode(String s) {
        return (s != null);
    }

    @Override
    public void init(EndpointConfig endpointConfig) {}

    @Override
    public void destroy() {}
}
