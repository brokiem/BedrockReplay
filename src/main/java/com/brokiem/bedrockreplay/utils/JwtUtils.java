/*
 * Copyright (c) 2023 brokiem
 * This project is licensed under the MIT License
 */

package com.brokiem.bedrockreplay.utils;

import com.alibaba.fastjson.JSONObject;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;

import java.net.URI;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.text.ParseException;
import java.util.Base64;

public class JwtUtils {

    public static SignedJWT encodeJWT(KeyPair keyPair, JSONObject payload) {
        String publicKeyBase64 = Base64.getEncoder().encodeToString(keyPair.getPublic().getEncoded());
        URI x5u = URI.create(publicKeyBase64);
        JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.ES384).x509CertURL(x5u).build();

        try {
            SignedJWT jwt = new SignedJWT(header, JWTClaimsSet.parse(payload.toJSONString()));
            EncryptionUtils.signJwt(jwt, (ECPrivateKey) keyPair.getPrivate());
            return jwt;
        } catch (JOSEException | ParseException e) {
            throw new RuntimeException(e);
        }
    }
}
