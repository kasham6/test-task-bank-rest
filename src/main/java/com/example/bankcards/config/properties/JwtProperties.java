package com.example.bankcards.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
        KeyPair sign,
        KeyPair enc,
        long accessExp,
        long refreshExp
) {
    public record KeyPair(
            String privateKey,
            String publicKey
    ) {
    }
}
