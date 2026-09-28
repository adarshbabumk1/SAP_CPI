package com.enterprise.sap.cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Enterprise Cryptographic HMAC-SHA256 Signing Script for SAP Cloud Integration.
 * 
 * Commonly required when integrating SAP S/4HANA with modern cloud SaaS APIs
 * and Webhooks (e.g. AWS Signature v4, Stripe, Shopify, GitHub, Banking APIs).
 * 
 * Generates both Hex and Base64 encoded HMAC signatures from body payload.
 * 
 * @author Adarsh (SAP Integration Architect)
 */
Message processData(Message message) {
    // 1. Read secret key from Secure Parameter or Keystore (simulated via Property)
    String secretKey = message.getProperty("WEBHOOK_SECRET_KEY") as String
    if (secretKey == null || secretKey.trim().isEmpty()) {
        throw new IllegalStateException("Security Error: 'WEBHOOK_SECRET_KEY' property is not configured.")
    }

    // 2. Fetch raw payload to sign
    byte[] bodyBytes = message.getBody(byte[].class)
    if (bodyBytes == null) {
        bodyBytes = "".getBytes(StandardCharsets.UTF_8)
    }

    // 3. Initialize HMAC-SHA256 Mac instance
    String algorithm = "HmacSHA256"
    Mac mac = Mac.getInstance(algorithm)
    SecretKeySpec secretKeySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), algorithm)
    mac.init(secretKeySpec)

    byte[] rawHmac = mac.doFinal(bodyBytes)

    // 4. Encode as Base64 and Hexadecimal string
    String base64Signature = Base64.getEncoder().encodeToString(rawHmac)
    String hexSignature = bytesToHex(rawHmac)

    // 5. Inject into standard headers for HTTP outbound adapter
    message.setHeader("X-Signature-SHA256", "sha256=" + hexSignature)
    message.setHeader("X-Hub-Signature-256", hexSignature)
    message.setHeader("X-Authorization-HMAC", base64Signature)

    return message
}

/**
 * Converts byte array to lowercase hexadecimal string
 */
static String bytesToHex(byte[] bytes) {
    StringBuilder hexString = new StringBuilder(2 * bytes.length)
    for (byte b : bytes) {
        String hex = Integer.toHexString(0xff & b)
        if (hex.length() == 1) {
            hexString.append('0')
        }
        hexString.append(hex)
    }
    return hexString.toString()
}
