package com.example.bankcards.service;

import com.example.bankcards.config.JwtConfig;
import com.example.bankcards.entity.User;
import com.example.bankcards.util.PemUtils;
import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.*;
import com.nimbusds.jwt.*;
import com.nimbusds.jwt.proc.BadJWTException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class JwtService {
    private final RSAPrivateKey signPriv;
    private final RSAPublicKey  signPub;
    private final RSAPrivateKey encPriv;
    private final RSAPublicKey  encPub;
    private final long          ACCESS_EXP;
    private final long          REFRESH_EXP;

    public JwtService(JwtConfig prop) {
        if (prop == null) throw new IllegalStateException("JwtConfig not provided");
        this.signPriv  = (RSAPrivateKey) PemUtils.readPrivateKey(prop.getSign().getPrivateKey(), "RSA");
        this.signPub   = (RSAPublicKey)  PemUtils.readPublicKey(prop.getSign().getPublicKey(),  "RSA");
        this.encPriv   = (RSAPrivateKey) PemUtils.readPrivateKey(prop.getEnc().getPrivateKey(),  "RSA");
        this.encPub    = (RSAPublicKey)  PemUtils.readPublicKey(prop.getEnc().getPublicKey(),   "RSA");
        this.ACCESS_EXP  = prop.getAccessExp();
        this.REFRESH_EXP = prop.getRefreshExp();
    }

    public String generateAccessToken(User user) throws JOSEException {
        return generateToken(user, ACCESS_EXP, "access");
    }

    public String generateRefreshToken(User user) throws JOSEException {
        return generateToken(user, REFRESH_EXP, "refresh");
    }

    private String generateToken(User user, long expMillis, String type) throws JOSEException {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expMillis);

        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(user.getId().toString())
                .claim("role", user.getRole().name())
                .claim("typ", type)
                .issueTime(now)
                .expirationTime(exp)
                .jwtID(UUID.randomUUID().toString())
                .build();

        SignedJWT signedJWT = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256)
                        .type(JOSEObjectType.JWT)
                        .build(),
                claims);
        signedJWT.sign(new RSASSASigner(signPriv));

        JWEHeader jweHeader = new JWEHeader.Builder(JWEAlgorithm.RSA_OAEP_256, EncryptionMethod.A256GCM)
                .contentType("JWT")
                .build();
        JWEObject jwe = new JWEObject(jweHeader, new Payload(signedJWT));
        jwe.encrypt(new RSAEncrypter(encPub));

        return jwe.serialize();
    }

    public JWTClaimsSet parseAccessToken(String token) throws Exception {
        return parseToken(token, "access");
    }

    public JWTClaimsSet parseRefreshToken(String token) throws Exception {
        return parseToken(token, "refresh");
    }

    private JWTClaimsSet parseToken(String token, String expectedType) throws Exception {
        SignedJWT signed;

        long dots = token.chars().filter(ch -> ch == '.').count();
        if (dots == 4) {
            try {
                JWEObject jwe = JWEObject.parse(token);
                jwe.decrypt(new RSADecrypter(encPriv));
                signed = SignedJWT.parse(jwe.getPayload().toString());
            } catch (Exception e) {
                log.debug("Failed to parse as JWE, trying as JWS: {}", e.getMessage());
                signed = SignedJWT.parse(token);
            }
        } else {
            signed = SignedJWT.parse(token);
        }

        if (!signed.verify(new RSASSAVerifier(signPub))) {
            throw new BadJWTException("Invalid signature");
        }

        JWTClaimsSet claims = signed.getJWTClaimsSet();

        Date exp = claims.getExpirationTime();
        if (exp == null || new Date().after(exp)) {
            throw new BadJWTException("Token expired");
        }

        String typ = claims.getStringClaim("typ");
        if (typ == null || !typ.equals(expectedType)) {
            throw new BadJWTException("Unexpected token type: " + typ);
        }

        return claims;
    }
}
