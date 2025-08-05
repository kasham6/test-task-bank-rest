package com.example.bankcards.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "encryption")
public record EncryptionProperties(
        AesGcmProperties aesGcm
) {
};