package com.krokochik.ideasforummfa.net;

import com.krokochik.ideasforummfa.AuthActivity;
import com.krokochik.ideasforummfa.model.Message;
import com.krokochik.ideasforummfa.service.MessageCipher;
import com.krokochik.ideasforummfa.service.MessageDecoder;
import com.krokochik.ideasforummfa.service.MessageEncoder;
import com.krokochik.ideasforummfa.service.TokenService;

import java.io.IOException;
import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;

@ClientEndpoint(
        encoders = MessageEncoder.class,
        decoders = MessageDecoder.class
)
public class WebSocket {

    Session session;

    final short IV_LENGTH = 203;
    final short SECRET_KEY_LENGTH = 5460;

    public WebSocket() {
        AuthActivity.webSocket = this;
    }

    MessageCipher cipher = new MessageCipher(
            new TokenService().generateToken((long) SECRET_KEY_LENGTH),
            new TokenService().generateToken((long) IV_LENGTH), "username", "trash");

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;

        // ping server to avoid H15 Heroku Idle Connection error
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() {
                if (session.isOpen())
                    session.getAsyncRemote().sendObject(new Message(new HashMap<String, String>() {{
                        put("msg", "ping");
                    }}));
            }
        }, 45_000, 45_000);
    }

    @OnMessage
    public void onMessage(Session session, Message message) {
        System.out.println(message.getContent().get("msg"));
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println("ERROR");
        throwable.printStackTrace();
    }

    public void disconnect() {
        try {
            session.close();
            System.gc();
        } catch (IOException ignored) {}
    }
}
