package com.carpeso.carpeso_backend.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public class EncryptionUtil {

    @Value("${encryption.secret.key}")
    private String secretKey;

    private static final String ALGORITHM = "AES";

    public String encrypt(String data) {
        if (data == null) return null;
        try {
            SecretKeySpec key = new SecretKeySpec(
                    secretKey.substring(0, 32).getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, key);
            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return data;
        }
    }

    public String decrypt(String encryptedData) {
        if (encryptedData == null) return null;
        try {
            SecretKeySpec key = new SecretKeySpec(
                    secretKey.substring(0, 32).getBytes(), ALGORITHM);
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, key);
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            return encryptedData;
        }
    }
}