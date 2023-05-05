/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.account;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;
import java.util.Base64;

public class AccountManager {
    private static final String ENCRYPTION_ALGORITHM = "AES";
    private static final String HASH_ALGORITHM = "SHA-256";
    private static final String SECRET_KEY = "d'&?,(U&b[m098-Qngm96)>$7w7aA=";

    public static String encrypt(String plaintext) throws Exception {
        SecretKey secretKey = generateSecretKey();
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey);
        byte[] encryptedBytes = cipher.doFinal(plaintext.getBytes());
        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    public static String decrypt(String ciphertext) throws Exception {
        SecretKey secretKey = generateSecretKey();
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey);
        byte[] decryptedBytes = cipher.doFinal(Base64.getDecoder().decode(ciphertext));
        return new String(decryptedBytes);
    }

    private static SecretKey generateSecretKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance(HASH_ALGORITHM);
        byte[] keyBytes = digest.digest(AccountManager.SECRET_KEY.getBytes());
        return new SecretKeySpec(keyBytes, ENCRYPTION_ALGORITHM);
    }
}
