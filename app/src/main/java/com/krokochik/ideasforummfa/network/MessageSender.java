package com.krokochik.ideasforummfa.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.krokochik.ideasforummfa.R;
import com.krokochik.ideasforummfa.model.Message;
import com.krokochik.ideasforummfa.resources.AESKeys;
import com.krokochik.ideasforummfa.service.crypto.MessageCipher;
import com.krokochik.ideasforummfa.service.crypto.TokenService;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.val;
import lombok.var;

@RequiredArgsConstructor
public class MessageSender {
    private final MessageCipher cipher = new MessageCipher("username", "keyId", "ivId");
    @NonNull
    private final WebSocket webSocket;
    @NonNull
    private final String sessionKey;
    @NonNull
    private final String username;

    public void sendMessage(@NonNull Message message, boolean encrypt) {
        if (encrypt) {
            message = encrypt(message);
        }

        message.put("username", username);
        webSocket.send(message);
    }

    public Bitmap enquireAvatar(@NonNull Context ctx) {
        var request = new Message("get", "avatar");
        request.put("username", username);
        sendMessage(request, false);

        String encodedAvatar;

        try {
            val response = webSocket.waitForMessage(
                    msg -> msg.getContent().containsKey("avatar"),
                    5_000L);
            encodedAvatar = response.get("avatar");
            assert (encodedAvatar != null);
        } catch (Exception e) {
            encodedAvatar = ctx.getResources().getString(R.string.guest_avatar);
        }

        val decodedAvatar = Base64.decode(encodedAvatar, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedAvatar, 0, decodedAvatar.length);
    }

    public Boolean isAuthenticated() {
        Message request = new Message(new HashMap<String, String>() {{
            put("get", "auth");
        }});
        sendMessage(request, false);

        Message response;

        try {
            response = webSocket.waitForMessage(msg -> msg.getContent().containsKey("auth"), 5_000L);
        } catch (TimeoutException e) {
            response = new Message("auth", "false");
        }

        return response.get("auth").equals("true");
    }

    private Message encrypt(@NonNull Message message) {
        int keyId = (int) Math.floor(Math.random() * AESKeys.keys.length);
        int ivId = (int) Math.floor(Math.random() * AESKeys.keys.length);

        message.put("keyId", keyId);
        message.put("ivId", keyId);
        return cipher.encrypt(message,
                TokenService.getHash(username + sessionKey, AESKeys.keys[ivId]),
                TokenService.getHash(username + sessionKey, AESKeys.keys[keyId]));
    }

    private Optional<Message> decrypt(@NonNull Message message) {
        if (!message.getContent().containsKey("keyId") || !message.getContent().containsKey("ivId"))
            return Optional.empty();

        return Optional.of(cipher.decrypt(message,
                TokenService.getHash(username + sessionKey, AESKeys.keys[Integer.parseInt(message.get("ivId"))]),
                TokenService.getHash(username + sessionKey, AESKeys.keys[Integer.parseInt(message.get("keyId"))])));
    }

}
