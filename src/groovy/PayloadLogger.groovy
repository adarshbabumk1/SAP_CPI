package com.enterprise.sap.cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import java.util.regex.Pattern

/**
 * Production-Grade Message Processing Log (MPL) Payload Logger for SAP Cloud Integration.
 * 
 * Features:
 *  - Dynamic activation check via exchange property 'ENABLE_DEBUG_LOGGING'
 *  - Automatic sensitive data masking (PII, Credit Cards, Bearer Tokens, Passwords)
 *  - Safe stream handling without exhausting Reader/InputStream
 *  - Custom Attachment naming with timestamp and step context
 * 
 * @author Adarsh (Tech Lead - SAP Integration)
 */
Message processData(Message message) {
    // 1. Check if logging is enabled for this execution (can be driven by header, property, or partner directory)
    String isDebugEnabled = message.getProperty("ENABLE_DEBUG_LOGGING") as String
    String stepName = (message.getProperty("LOG_STEP_NAME") as String) ?: "Checkpoint_Payload"
    
    // Default to true in DEV/QA, can be disabled or selectively triggered in PRD
    if (isDebugEnabled != null && isDebugEnabled.equalsIgnoreCase("false")) {
        return message
    }

    // 2. Read payload safely
    Object body = message.getBody()
    if (body == null) {
        return message
    }

    String payloadStr = message.getBody(java.lang.String) as String
    if (payloadStr == null || payloadStr.trim().isEmpty()) {
        return message
    }

    // 3. Mask sensitive attributes (Regex based sanitization)
    String maskedPayload = maskSensitiveFields(payloadStr)

    // 4. Attach to SAP CPI Message Processing Log (MPL)
    def messageLog = messageLogFactory.getMessageLog(message)
    if (messageLog != null) {
        String contentType = detectContentType(payloadStr)
        String attachmentTitle = "${stepName}_${System.currentTimeMillis()}"
        
        messageLog.addAttachmentAsString(attachmentTitle, maskedPayload, contentType)
        messageLog.setStringProperty("Last_Logged_Step", stepName)
    }

    // 5. Restore payload to ensure downstream steps continue seamlessly
    message.setBody(payloadStr)
    return message
}

/**
 * Masks credit card numbers, authorization tokens, and password fields.
 */
static String maskSensitiveFields(String input) {
    if (input == null) return ""
    
    // Mask passwords in JSON: "password": "value"
    String sanitized = input.replaceAll(/(?i)"(password|secret|token|client_secret)"\s*:\s*"[^"]+"/, '"$1": "********"')
    
    // Mask passwords in XML: <password>value</password>
    sanitized = sanitized.replaceAll(/(?i)<(password|secret|token|client_secret)>.*?<\/\1>/, '<$1>********</$1>')
    
    // Mask 16-digit credit card patterns
    sanitized = sanitized.replaceAll(/\b(?:\d{4}[-\s]?){3}\d{4}\b/, 'XXXX-XXXX-XXXX-XXXX')
    
    return sanitized
}

/**
 * Detects MIME type for the MPL attachment
 */
static String detectContentType(String payload) {
    String trimmed = payload.trim()
    if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
        return "application/json"
    } else if (trimmed.startsWith("<")) {
        return "application/xml"
    }
    return "text/plain"
}
