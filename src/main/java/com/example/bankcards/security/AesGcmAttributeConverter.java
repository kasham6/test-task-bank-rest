package com.example.bankcards.security;


import com.example.bankcards.config.EncryptionProperties;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;

@Converter
@Component
public class AesGcmAttributeConverter implements AttributeConverter<String, String> {
    private static final String ALGO = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;
    private static final int TAG_BIT_LENGTH = 128;
    private final SecretKey key;

    @Autowired
    public AesGcmAttributeConverter(EncryptionProperties encryptionProperties) {
        String base64Key = encryptionProperties.aesGcm().key();
        if (base64Key == null) throw new IllegalStateException("ENCRYPTION_KEY not set");
        byte[] decoded = Base64.getDecoder().decode(base64Key);
        key = new SecretKeySpec(decoded, "AES");
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) return null;
        try {
            Cipher cipher = Cipher.getInstance(ALGO);
            byte[] iv = new byte[IV_SIZE];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            byte[] cipherText = cipher.doFinal(attribute.getBytes());
            ByteBuffer buf = ByteBuffer.allocate(iv.length + cipherText.length);
            buf.put(iv);
            buf.put(cipherText);
            return Base64.getEncoder().encodeToString(buf.array());
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            byte[] all = Base64.getDecoder().decode(dbData);
            ByteBuffer buf = ByteBuffer.wrap(all);
            byte[] iv = new byte[IV_SIZE];
            buf.get(iv);
            byte[] cipherText = new byte[buf.remaining()];
            buf.get(cipherText);
            Cipher cipher = Cipher.getInstance(ALGO);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_BIT_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            byte[] plain = cipher.doFinal(cipherText);
            return new String(plain);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }
}
