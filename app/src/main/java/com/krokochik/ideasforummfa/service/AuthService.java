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
            Log.d(TAG, "authenticate() called with: login = [" + login + "], pass = [" + pass + "]");
            // step 1
            clientSession.step1(login, pass);
            webSocket.send(new Message("username", login));
            System.out.println("1 sent");

            Message response = webSocket.waitForMessage(
                    msg -> (msg.getContent().containsKey("B") && msg.getContent().containsKey("s")), 5_000L);
            BigInteger B = new BigInteger(response.get("B"));
            BigInteger salt = new BigInteger(response.get("s"));
            System.out.println("1 rec");

            System.out.println(salt);
            System.out.println(B);
            // step 2
            SRP6ClientCredentials credentials = clientSession.step2(params, salt, B);
            System.out.println("step 2 finished");

            webSocket.send(new Message(new HashMap<String, String>() {{
                put("username", login);
                put("A", credentials.A.toString());
                put("M1", credentials.M1.toString());
            }}));

            sessionKey = clientSession.getSessionKey().toString(16);
            System.out.println("2 sent");

            // final response
            response = webSocket.waitForMessage((msg) -> {
                return msg.getContent().containsKey("keyId") &&
                        msg.getContent().containsKey("ivId");
            }, 5_000L);
            System.out.println("2 rec");

            response = cipher.decrypt(response,
                    TokenService.getHash(login + sessionKey, AESKeys.keys[Integer.parseInt(response.get("ivId"))]),
                    TokenService.getHash(login + sessionKey, AESKeys.keys[Integer.parseInt(response.get("keyId"))]));
            System.out.println(response);

            Log.d(TAG, "authenticate() returned: " + response.get("authenticated").equals("true"));
            return response.get("authenticated").equals("true");
        }
        catch (SRP6Exception | TimeoutException e) {
            e.printStackTrace();
            Log.d(TAG, "authenticate() returned: " + false);
            return false;
        }
    }

}
