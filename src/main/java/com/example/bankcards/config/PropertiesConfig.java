package com.example.bankcards.config;

import com.example.bankcards.config.properties.EncryptionProperties;
import com.example.bankcards.config.properties.JwtProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        EncryptionProperties.class,
        JwtProperties.class
})
public class PropertiesConfig {
}
