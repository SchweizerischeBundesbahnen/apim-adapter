package ch.sbb.integration.api.adapter.util;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.RandomStringUtils;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.List;

public class TokenIssuerKeys {

    private TokenIssuerKeys() {
    }

    private static KeyPairGenerator kpg;
    private static List<Key> keys = new ArrayList<>();

    static {
        initKeyPairGenerator();
        initKeyPair();
    }

    private static void initKeyPairGenerator() {
        try {
            kpg = KeyPairGenerator.getInstance("RSA");
            kpg.initialize(2048);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("failed to create Keypair for token generator", e);
        }
    }

    private static void initKeyPair() {
        for (int i = 1; i <= 3; i++) {
            keys.add(createKey());
        }
    }

    static Key createKey() {
        final String keyId = RandomStringUtils.randomAlphanumeric(27);
        final KeyPair kp = kpg.generateKeyPair();
        return new Key(keyId, (RSAPublicKey) kp.getPublic(), (RSAPrivateKey) kp.getPrivate());
    }

    public static List<Key> getKeys() {
        return keys;
    }

    public static Key getKey(int idx) {
        return keys.get(idx);
    }

    public static class Key {
        private String keyId;
        private RSAPublicKey pubKey;
        private RSAPrivateKey privKey;

        public Key(String keyId, RSAPublicKey pubKey, RSAPrivateKey privKey) {
            this.keyId = keyId;
            this.pubKey = pubKey;
            this.privKey = privKey;
        }

        public String getKeyId() {
            return keyId;
        }

        public void setKeyId(String keyId) {
            this.keyId = keyId;
        }

        public RSAPublicKey getPublicKey() {
            return pubKey;
        }

        public String getPublicKeyModulusBase64() {
            return Base64.encodeBase64String(getPublicKey().getModulus().toByteArray());
        }

        public String getPublicKeyExponentBase64() {
            return Base64.encodeBase64String(getPublicKey().getPublicExponent().toByteArray());
        }

        public RSAPrivateKey getPrivateKey() {
            return privKey;
        }
    }
}
