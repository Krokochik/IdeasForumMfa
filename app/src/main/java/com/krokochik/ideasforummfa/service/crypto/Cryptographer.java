package com.krokochik.ideasforummfa.service.crypto;

import org.apache.commons.net.util.Base64;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import at.favre.lib.crypto.SingleStepKdf;
import dev.samstevens.totp.code.HashingAlgorithm;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;

/**
 * Functional class containing static methods that are working with cryptography.
 * */
public class Cryptographer {

    private static final String AES_ALGORITHM = "AES/CBC/PKCS5PADDING";
    @Getter
    private static final String HASHING_ALGORITHM = HashingAlgorithm.SHA512.name();

    /**
     * Encrypts a string with AES algorithm.
     *
     * @param str        any nonempty string to be encrypted.
     * @param secretKey  any-length non-blank string to be used by AES as a key.
     * @param initVector any-length string to be used by AES as a parameter.
     * @return encrypted string.
     */
    public static String encrypt(@NonNull String str,
                                 @NonNull String secretKey,
                                 @NonNull String initVector) {
        if (str.isEmpty()) throw new IllegalArgumentException("This parameter cannot be empty: str");
        if (secretKey.trim().isEmpty()) throw new IllegalArgumentException("This parameter cannot be blank: secretKey");

        try {
            IvParameterSpec iv = new IvParameterSpec(
                    SingleStepKdf.fromSha256().derive(initVector
                            .getBytes(StandardCharsets.UTF_8), 16));
            SecretKeySpec key = new SecretKeySpec(
                    SingleStepKdf.fromSha256().derive(secretKey
                            .getBytes(StandardCharsets.UTF_8), 16), "AES");
            byte[] subject = str.getBytes();

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key, iv);

            return Base64.encodeBase64String(cipher.doFinal(subject));
        } catch (Exception unreachable) {
            unreachable.printStackTrace();
            return "";
        }
    }

    /**
     * Decrypts a string with AES algorithm.
     *
     * @param str        any nonempty string to be encrypted.
     * @param secretKey  any-length non-blank string to be used by AES as a key.
     * @param initVector any-length string to be used by AES as a parameter.
     * @return decrypted string.
     * @throws IllegalBlockSizeException inherited from {@link javax.crypto.Cipher}.
     */
    public static String decrypt(@NonNull String str,
                                 @NonNull String secretKey,
                                 @NonNull String initVector) throws IllegalBlockSizeException {
        try {
            IvParameterSpec iv = new IvParameterSpec(
                    SingleStepKdf.fromSha256()
                            .derive(initVector.getBytes(StandardCharsets.UTF_8), 16));
            SecretKeySpec key = new SecretKeySpec(
                    SingleStepKdf.fromSha256().derive(secretKey
                            .getBytes(StandardCharsets.UTF_8), 16), "AES");
            byte[] subject = str.getBytes();

            Cipher cipher = Cipher.getInstance(AES_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key, iv);

            return new String(cipher.doFinal(Base64.decodeBase64(subject)));
        } catch (IllegalBlockSizeException e) {
            throw new IllegalBlockSizeException(e.getMessage());
        } catch (Exception unreachable) {
            unreachable.printStackTrace();
            return "";
        }
    }

    /**
     * Computes SHA-512 hash of the string.
     */
    @SneakyThrows
    public static String getHash(String str, String salt) {
        str += salt;
        MessageDigest crypt = MessageDigest.getInstance(HASHING_ALGORITHM);
        crypt.update(str.getBytes(StandardCharsets.UTF_8));

        byte[] bytes = crypt.digest();
        BigInteger bi = new BigInteger(1, bytes);
        return String.format("%0" + (bytes.length << 1) + "x", bi);
    }

    /**
     * Computes SHA-512 hash of the string.
     */
    public static String getHash(String str) {
        return getHash(str, "");
    }
}
