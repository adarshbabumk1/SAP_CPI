package com.enterprise.sap.cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonBuilder

/**
 * Standardized Exception Mapping Script for SAP Cloud Integration.
 * 
 * Used inside Exception Subprocess to intercept uncaught faults, RFC/OData
 * error responses, or script failures, converting them into an RFC 7807 
 * compliant "Problem Details for HTTP APIs" JSON payload.
 * 
 * Sets HTTP status code headers and logs root causes into the SAP MPL.
 * 
 * @author Adarsh (Tech Lead - SAP Integration)
 */
Message processData(Message message) {
    // 1. Retrieve the underlying Camel/CPI exception
    Exception ex = message.getProperty("CamelExceptionCaught") as Exception
    String correlationId = message.getHeader("SAP_MessageProcessingLogID") as String ?: UUID.randomUUID().toString()
    String iflowId = message.getProperty("SAP_IntegrationFlowName") as String ?: "UNKNOWN_IFLOW"
    
    int statusCode = 500
    String errorTitle = "Internal Server Error"
    String detailedMessage = "An unhandled exception occurred during integration flow execution."
    String errorCode = "INTEGRATION_ERROR_500"

    if (ex != null) {
        detailedMessage = ex.getMessage() ?: ex.toString()

        // Distinguish common error categories
        if (detailedMessage.contains("Unauthorized") || detailedMessage.contains("401")) {
            statusCode = 401
            errorTitle = "Unauthorized Backend Access"
            errorCode = "AUTH_FAILURE_401"
        } else if (detailedMessage.contains("Forbidden") || detailedMessage.contains("403")) {
            statusCode = 403
            errorTitle = "Access Forbidden"
            errorCode = "AUTH_FORBIDDEN_403"
        } else if (detailedMessage.contains("Timeout") || detailedMessage.contains("Connection refused")) {
            statusCode = 504
            errorTitle = "Gateway Timeout"
            errorCode = "BACKEND_UNAVAILABLE_504"
        } else if (detailedMessage.contains("Validation") || detailedMessage.contains("IllegalArgumentException")) {
            statusCode = 400
            errorTitle = "Bad Request / Schema Validation Failed"
            errorCode = "VALIDATION_FAILED_400"
        }
    }

    // 2. Build RFC 7807 JSON Error Body
    def jsonBuilder = new JsonBuilder()
    jsonBuilder {
        type "https://<integration-runtime-host>/errors/${errorCode.toLowerCase()}"
        title errorTitle
        status statusCode
        detail detailedMessage
        instance "/v1/integrations/${iflowId}/executions/${correlationId}"
        code errorCode
        timestamp new Date().format("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", TimeZone.getTimeZone("UTC"))
        correlationId correlationId
    }

    String errorPayload = jsonBuilder.toPrettyString()

    // 3. Set headers and response body
    message.setBody(errorPayload)
    message.setHeader("Content-Type", "application/problem+json")
    message.setHeader("CamelHttpResponseCode", statusCode)

    // 4. Update Message Processing Log (MPL)
    def messageLog = messageLogFactory.getMessageLog(message)
    if (messageLog != null) {
        messageLog.setStringProperty("Error_Code", errorCode)
        messageLog.setStringProperty("Error_Status_Code", String.valueOf(statusCode))
        messageLog.addAttachmentAsString("Error_Details_RFC7807", errorPayload, "application/problem+json")
    }

    return message
}
