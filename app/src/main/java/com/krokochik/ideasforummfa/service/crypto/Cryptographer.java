package com.krokochik.ideasforummfa.service.crypto;

import at.favre.lib.crypto.SingleStepKdf;

import lombok.SneakyThrows;
import org.apache.commons.net.util.Base64;
import org.jetbrains.annotations.NotNull;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class Cryptographer {

    ArrayList<String> skipKeys;

    public Cryptographer(@NotNull String... skippedKeys) {
        this.skipKeys = new ArrayList<>();

        skipKeys.addAll(Arrays.stream(skippedKeys)
                .map(String::toLowerCase)
                .collect(Collectors.toList()));
    }


    @SneakyThrows
    public static String encrypt(String str, String initVector, String secretKey) {
        IvParameterSpec ivParameter = new IvParameterSpec(SingleStepKdf.fromSha256().derive(initVector.getBytes(StandardCharsets.UTF_8), 16));
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5PADDING");
        SecretKeySpec keySpec = new SecretKeySpec(SingleStepKdf.fromSha256().derive(secretKey.getBytes(StandardCharsets.UTF_8), 16), "AES");
        cipher.init(javax.crypto.Cipher.ENCRYPT_MODE, keySpec, ivParameter);

        return Base64.encodeBase64String(cipher.doFinal(str.getBytes()));
    }

    @SneakyThrows
    public static String decrypt(String str, String initVector, String secretKey) {
        IvParameterSpec ivParameter = new IvParameterSpec(SingleStepKdf.fromSha256().derive(initVector.getBytes(StandardCharsets.UTF_8), 16));
        javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/CBC/PKCS5PADDING");
        SecretKeySpec keySpec = new SecretKeySpec(SingleStepKdf.fromSha256().derive(secretKey.getBytes(StandardCharsets.UTF_8), 16), "AES");
        cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, ivParameter);

        return new String(cipher.doFinal(Base64.decodeBase64(str)));
    }

}

