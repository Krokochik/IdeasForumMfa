package com.krokochik.ideasforummfa.service;

import static android.content.ContentValues.TAG;

import android.util.Log;

import com.krokochik.ideasforummfa.model.Message;
import com.krokochik.ideasforummfa.network.WebSocket;
import com.krokochik.ideasforummfa.resources.AESKeys;
import com.krokochik.ideasforummfa.service.crypto.MessageCipher;
import com.krokochik.ideasforummfa.service.crypto.TokenService;
import com.nimbusds.srp6.BigIntegerUtils;
import com.nimbusds.srp6.SRP6ClientCredentials;
import com.nimbusds.srp6.SRP6ClientSession;
import com.nimbusds.srp6.SRP6CryptoParams;
import com.nimbusds.srp6.SRP6Exception;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;
import java.util.logging.LogRecord;

import lombok.Getter;

public class AuthService {

    private final WebSocket webSocket;
    private final SRP6ClientSession clientSession = new SRP6ClientSession();
    private final SRP6CryptoParams params = SRP6CryptoParams.getInstance(2048, "SHA-512");
    private final MessageCipher cipher = new MessageCipher("username", "keyId", "ivId");
    @Getter
    private String sessionKey;

    public AuthService(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    public boolean authenticate(String login, String pass) {
        try {
            // step 1
            clientSession.step1(login, pass);
            webSocket.send(new Message("username", login));

            Message response = webSocket.waitForMessage(
                    msg -> (msg.getContent().containsKey("B") && msg.getContent().containsKey("s")), 5_000L);
            BigInteger B = new BigInteger(response.get("B"));
            BigInteger salt = new BigInteger(response.get("s"));

            // step 2
            SRP6ClientCredentials credentials = clientSession.step2(params, salt, B);

            webSocket.send(new Message(new HashMap<String, String>() {{
                put("username", login);
                put("A", credentials.A.toString());
                put("M1", credentials.M1.toString());
            }}));

            sessionKey = clientSession.getSessionKey().toString(16);

            // successful session key calculation check
            response = webSocket.waitForMessage((msg) -> {
                return msg.getContent().containsKey("keyId") &&
                        msg.getContent().containsKey("ivId");
            }, 5_000L);

            response = cipher.decrypt(response,
                    TokenService.getHash(AESKeys.keys[Integer.parseInt(response.get("ivId"))], login + sessionKey),
                    TokenService.getHash(AESKeys.keys[Integer.parseInt(response.get("keyId"))], login + sessionKey));
            System.out.println(response);

            return response.get("test").equals("test");
        }
        catch (SRP6Exception | TimeoutException e) {
            e.printStackTrace();
            return false;
        }
    }

}
