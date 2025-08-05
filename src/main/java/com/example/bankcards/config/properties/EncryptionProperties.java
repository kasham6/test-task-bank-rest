package com.example.bankcards.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "encryption")
public record EncryptionProperties(
        AesGcmProperties aesGcm
) {
}