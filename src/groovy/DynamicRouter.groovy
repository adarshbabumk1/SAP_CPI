package com.enterprise.sap.cpi.scripts

import com.sap.gateway.ip.core.customdev.util.Message
import groovy.json.JsonSlurper
import groovy.xml.XmlSlurper

/**
 * Enterprise Dynamic Routing Script for SAP Cloud Integration.
 * 
 * Inspects incoming message header/body to dynamically evaluate:
 *  - Target System Routing Key (e.g. SFDC, S4HANA, LOGISTICS)
 *  - Target ProcessDirect Address (e.g. /finance/orders/s4)
 *  - Execution Priority (HIGH, MEDIUM, LOW)
 * 
 * Replaces legacy SAP PO XPath-based Receiver Determination without multiple
 * DOM tree builds.
 * 
 * @author Adarsh (Tech Lead - SAP Integration)
 */
Message processData(Message message) {
    String payload = message.getBody(java.lang.String) as String
    String contentType = (message.getHeader("Content-Type") as String) ?: "application/json"
    
    String senderSystem = message.getHeader("SAP_SenderSystem") as String ?: "UNKNOWN_SENDER"
    String targetSystem = "DEFAULT_ROUTER"
    String targetEndpoint = "/fallback/unrouted"
    String routingKey = ""

    try {
        if (contentType.toLowerCase().contains("json") || payload.trim().startsWith("{")) {
            def json = new JsonSlurper().parseText(payload)
            routingKey = json?.Header?.Region ?: json?.region ?: "GLOBAL"
            String businessType = json?.Header?.OrderType ?: json?.orderType ?: "STANDARD"

            // Routing decision matrix
            switch (routingKey.toUpperCase()) {
                case "EMEA":
                    targetSystem = "S4HANA_EMEA"
                    targetEndpoint = "/core/orders/s4hana-emea"
                    break
                case "NA":
                    targetSystem = "SALESFORCE_NA"
                    targetEndpoint = "/crm/orders/sfdc-na"
                    break
                case "APAC":
                    targetSystem = "S4HANA_APAC"
                    targetEndpoint = "/core/orders/s4hana-apac"
                    break
                default:
                    targetSystem = "DEFAULT_SYSTEM"
                    targetEndpoint = "/core/orders/generic"
                    break
            }
        } else if (contentType.toLowerCase().contains("xml") || payload.trim().startsWith("<")) {
            def xml = new XmlSlurper().parseText(payload)
            routingKey = xml.Header.Region.text() ?: "GLOBAL"
            
            if (routingKey == "US") {
                targetSystem = "SAP_ECC_US"
                targetEndpoint = "/legacy/ecc/us"
            } else {
                targetSystem = "S4HANA_GLOBAL"
                targetEndpoint = "/core/s4/global"
            }
        }
    } catch (Exception ex) {
        // Record error details for dead-letter processing
        message.setProperty("ROUTING_ERROR", ex.getMessage())
        targetSystem = "ERROR_QUEUE"
        targetEndpoint = "/common/exception/routing-failure"
    }

    // Assign Exchange Properties for downstream Router & ProcessDirect steps
    message.setProperty("TargetSystem", targetSystem)
    message.setProperty("TargetProcessDirectAddress", targetEndpoint)
    message.setProperty("RoutingKey", routingKey)
    message.setHeader("CamelHttpUri", targetEndpoint)

    // Log decision in MPL
    def messageLog = messageLogFactory.getMessageLog(message)
    if (messageLog != null) {
        messageLog.setStringProperty("Routing_Decision_Target", targetSystem)
        messageLog.setStringProperty("Target_ProcessDirect", targetEndpoint)
    }

    return message
}
