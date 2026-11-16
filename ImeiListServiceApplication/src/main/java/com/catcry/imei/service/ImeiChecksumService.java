package com.catcry.imei.service;

import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;

@Service
public class ImeiChecksumService {

    /**
     * Builds the canonical string that is hashed.
     * CHANGE THIS if the agreed rule is different (separator, sorting, JSON form, ...).
     */
    String canonicalForm(List<String> imeis) {
        return String.join(",", imeis);
    }

    /** Returns the SHA-256 hex (lowercase) of the canonical form. */
    public String compute(List<String> imeis) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(canonicalForm(imeis).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }

    /** True if the received checksum equals the one computed on the server. */
    public boolean matches(List<String> imeis, String receivedChecksum) {
        return receivedChecksum != null
                && compute(imeis).equalsIgnoreCase(receivedChecksum.trim());
    }
}
