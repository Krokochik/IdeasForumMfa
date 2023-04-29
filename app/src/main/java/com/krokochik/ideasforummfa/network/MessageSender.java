package com.krokochik.ideasforummfa.network;

import android.content.Context;
import android.content.SharedPreferences;
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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.val;
import lombok.var;

@RequiredArgsConstructor
@AllArgsConstructor
public class MessageSender {

    private final MessageCipher cipher = new MessageCipher("username", "keyId", "ivId", "content");

    @Setter
    @NonNull
    private WebSocket webSocket;
    private String sessionKey;
    private String username;

    public void sendMessage(@NonNull Message message, boolean encrypt) {
        if (encrypt) {
            message = encrypt(message);
        }

        message.put("username", username);
        webSocket.send(message);
    }

    public Bitmap enquireAvatar(@NonNull Context ctx) {
        var request = new Message("get", "avatar");
        sendMessage(request, false);

        String encodedAvatar;

        try {
            val response = webSocket.waitForMessage(
                    msg -> msg.getContent().containsKey("avatar"),
                    -1L);
            encodedAvatar = response.get("avatar");
            System.out.println(encodedAvatar);
            assert (encodedAvatar != null);
        } catch (Exception e) {
            e.printStackTrace();
            encodedAvatar = ctx.getResources().getString(R.string.guest_avatar);
        }

        val decodedAvatar = Base64.decode(encodedAvatar, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedAvatar, 0, decodedAvatar.length);
    }

    public Boolean isAuthenticated() {
        Message request = new Message(new HashMap<String, String>() {{
            put("get", "authCheckingMessage");
        }});
        sendMessage(request, false);

        Message response;

        try {
            response = webSocket.waitForMessage(msg -> {
                return msg.getContent().containsKey("content") && msg.get("content").equals("authCheckingMessage");
            }, 5_000L);
        } catch (TimeoutException e) {
            System.out.println("timeout");
            return false;
        }

        val decryptedResponse = decrypt(response);
        if (decryptedResponse.isPresent())
            response = decryptedResponse.get();
        else return false;

        System.out.println("last return");
        return response.getContent().containsKey("test") && response.get("test").equals("test");
    }

    private Message encrypt(@NonNull Message message) {
        int keyId = (int) Math.floor(Math.random() * AESKeys.keys.length);
        int ivId = (int) Math.floor(Math.random() * AESKeys.keys.length);

        message.put("keyId", keyId);
        message.put("ivId", keyId);
        return cipher.encrypt(message,
                TokenService.getHash(AESKeys.keys[ivId], username + sessionKey),
                TokenService.getHash(AESKeys.keys[keyId], username + sessionKey));
    }

    private Optional<Message> decrypt(@NonNull Message message) {
        if (!message.getContent().containsKey("keyId") || !message.getContent().containsKey("ivId"))
            return Optional.empty();

        return Optional.of(cipher.decrypt(message,
                TokenService.getHash(AESKeys.keys[Integer.parseInt(message.get("ivId"))], username + sessionKey),
                TokenService.getHash(AESKeys.keys[Integer.parseInt(message.get("keyId"))], username + sessionKey)));
    }

}
