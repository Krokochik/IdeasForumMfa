package com.krokochik.ideasforummfa.service;

import com.google.gson.Gson;
import com.krokochik.ideasforummfa.model.Message;

import jakarta.websocket.EncodeException;
import jakarta.websocket.Encoder;
import jakarta.websocket.EndpointConfig;


public class MessageEncoder implements Encoder.Text<Message> {

    private static final Gson gson = new Gson();

    @Override
    public String encode(Message msg) throws EncodeException {
        return gson.toJson(msg);
    }

    @Override
    public void init(EndpointConfig endpointConfig) {}

    @Override
    public void destroy() {}
}
