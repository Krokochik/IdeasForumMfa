package com.krokochik.ideasforummfa.network;

import com.krokochik.ideasforummfa.model.CallbackTask;
import com.krokochik.ideasforummfa.model.Condition;
import com.krokochik.ideasforummfa.model.Message;
import com.krokochik.ideasforummfa.service.crypto.MessageCipher;
import com.krokochik.ideasforummfa.service.MessageDecoder;
import com.krokochik.ideasforummfa.service.MessageEncoder;
import com.krokochik.ideasforummfa.service.crypto.TokenService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.websocket.ClientEndpoint;
import jakarta.websocket.CloseReason;
import jakarta.websocket.OnClose;
import jakarta.websocket.OnError;
import jakarta.websocket.OnMessage;
import jakarta.websocket.OnOpen;
import jakarta.websocket.Session;
import lombok.SneakyThrows;

@ClientEndpoint(
        encoders = MessageEncoder.class,
        decoders = MessageDecoder.class
)
public class WebSocket {

    private Session session;
    final ArrayList<CallbackTask<Message>> onMessage = new ArrayList<>();

    public boolean isOpened() {
        return session.isOpen();
    }

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        System.out.println("opened");

        // ping server to avoid H15 Heroku Idle Connection error
        new Timer().scheduleAtFixedRate(new TimerTask() {
            public void run() {
                send(new Message("msg", "ping"));
            }
        }, 0, 45_000);
    }

    @OnMessage
    public void onMessage(Session session, Message message) {
        System.out.println(message);


        ArrayList<CallbackTask<Message>> onMessageIterable = new ArrayList<>(onMessage);
        new Thread(() -> onMessageIterable.forEach(task -> {
            try {
                task.run(message);
            } catch (Exception ignored) {
            }
        })).start();
    }

    @OnClose
    public void onClose(Session session, CloseReason closeReason) {
        System.out.println("closed");
    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        System.out.println("ERROR");
        throwable.printStackTrace();
    }

    public void disconnect() {
        if (session.isOpen())
            try {
                session.close(new CloseReason(CloseReason.CloseCodes.NORMAL_CLOSURE, "normal closure"));
            } catch (IOException ignored) { }
    }

    private void setOnMessage(CallbackTask<Message> task) {
        onMessage.add(task);
    }

    public void removeOnMessage(CallbackTask<Message> task) {
        onMessage.remove(task);
    }

    public Message waitForMessage(final Condition<Message> test, final Long timeout) throws TimeoutException {
        while (!session.isOpen()) {
        }
        AtomicReference<Message> msg = new AtomicReference<>();
        AtomicBoolean wait = new AtomicBoolean(true);

        AtomicReference<CallbackTask<Message>> onMessageTask = new AtomicReference<>();
        onMessageTask.set((message) -> {
            for (int i = 0; i < message.getContent().size(); i++) {
                if (test.check(message)) {
                    msg.set(message);
                    wait.set(false);
                    removeOnMessage(onMessageTask.get());
                }
            }
        });

        if (timeout >= 0)
            new Timer().schedule(new TimerTask() {
                public void run() {
                    msg.set(null);
                    wait.set(false);
                }
            }, timeout);

        setOnMessage(onMessageTask.get());

        System.out.println("waiting");

        while (wait.get()) {
        }

        System.out.println("stop waiting");

        if (msg.get() == null)
            throw new TimeoutException();
        return msg.get();
    }

    @SneakyThrows
    public Message waitForMessage(final Condition<Message> test) {
        return waitForMessage(test, -1L);
    }

    @SneakyThrows
    public Message waitForMessage(final Long timeout) {
        return waitForMessage((msg) -> true, timeout);
    }

    @SneakyThrows
    public Message waitForMessage() {
        return waitForMessage((msg) -> true, -1L);
    }


    public void send(Message msg) {
        asyncSend(msg);
    }

    public void asyncSend(Message msg) {
        while (!session.isOpen()) {
        }

        new Thread(() -> {
            if (session.isOpen())
                session.getAsyncRemote().sendObject(msg);
        }).start();
    }

}
