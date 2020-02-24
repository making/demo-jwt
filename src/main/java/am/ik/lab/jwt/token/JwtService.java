package am.ik.lab.jwt.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.Base64Utils;
import org.springframework.util.StreamUtils;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.EncodedKeySpec;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtService {
    private JWSSigner signer;
    private JWSVerifier verifier;
    private String keyId;
    private Resource signingKey;
    private Resource verifierKey;

    public String getKeyId() {
        return keyId;
    }

    public void setKeyId(String keyId) {
        this.keyId = keyId;
    }

    public void setSigningKey(Resource resource) throws Exception {
        this.signingKey = resource;
        final String key = resourceToString(resource);
        final byte[] decoded = Base64Utils.decodeFromString(key
                .replace("fake", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .trim()
                .replace("\r\n", "")
                .replace("\n", ""));
        final EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decoded);
        final KeyFactory kf = KeyFactory.getInstance("RSA");
        final PrivateKey privateKey = kf.generatePrivate(keySpec);
        this.signer = new RSASSASigner(privateKey);
    }

    public Resource getSigningKey() {
        return signingKey;
    }

    public void setVerifierKey(Resource resource) {
        this.verifierKey = resource;
    }

    public Resource getVerifierKey() {
        return verifierKey;
    }

    public SignedJWT sign(JWTClaimsSet claimsSet) {
        final JWSHeader header = new JWSHeader.Builder(JWSAlgorithm.RS256) //
                .keyID(this.keyId)//
                .type(JOSEObjectType.JWT) //
                .build();
        final SignedJWT signedJWT = new SignedJWT(header, claimsSet);
        try {
            signedJWT.sign(signer);
        } catch (JOSEException e) {
            throw new IllegalStateException(e);
        }
        return signedJWT;
    }

    public boolean isValid(SignedJWT jwt) {
        try {
            return jwt.verify(this.verifier);
        } catch (JOSEException e) {
            throw new IllegalArgumentException(e);
        }
    }

    @PostConstruct
    public void afterPropertiesSet() throws Exception {
        if (this.verifier != null) {
            return;
        }
        final String key = resourceToString(this.verifierKey)
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .trim()
                .replace("\r\n", "")
                .replace("\n", "");
        final byte[] decode = Base64Utils.decodeFromString(key);
        final X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decode);
        final KeyFactory kf = KeyFactory.getInstance("RSA");
        final RSAPublicKey publicKey = (RSAPublicKey) kf.generatePublic(keySpec);
        final JWSVerifier verifier = new RSASSAVerifier(publicKey);
        final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder().subject("test").build();
        final SignedJWT signedJWT = sign(claimsSet);
        if (!signedJWT.verify(verifier)) {
            throw new IllegalStateException(
                    "The pair of verifierKey and signingKey is wrong.");
        }
        this.verifier = verifier;
    }

    public Map<String, String> getKey() {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("kid", this.keyId);
        result.put("alg", "RS256");
        result.put("value", resourceToString(this.verifierKey));
        return result;
    }

    private static String resourceToString(Resource resource) {
        try (InputStream stream = resource.getInputStream()) {
            return StreamUtils.copyToString(stream, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
