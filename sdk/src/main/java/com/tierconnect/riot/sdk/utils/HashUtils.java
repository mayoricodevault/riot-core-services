package com.tierconnect.riot.sdk.utils;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @author agutierrez
 */
public class HashUtils {

    private static String hashString(String string, String algorithm) {
        try {
            MessageDigest sha256 = MessageDigest.getInstance(algorithm);
            sha256.update(string.getBytes("UTF-8"));
            byte[] digest = sha256.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                sb.append(String.format("%02x", digest[i]));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }


    public static String hashSHA256(String string) {
        return hashString(string, "SHA-256");
    }

    public static String hashMD5(String string) {
        return hashString(string, "MD5");
    }

}
