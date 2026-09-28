# 📋 SAP PO to SAP Cloud Integration (CPI) Comprehensive Mapping Matrix

This document provides technical leads and integration developers with a direct side-by-side transition matrix when modernizing legacy **SAP Process Orchestration (PO 7.4 / 7.5 Single-Stack Java AEX)** artifacts to **SAP BTP Integration Suite (Cloud Integration)**.

---

## 1. Core Architecture & Concepts

| Area | SAP Process Orchestration (PO 7.5) | SAP Cloud Integration (CPI) | Migration Guidance |
| :--- | :--- | :--- | :--- |
| **Design Repository** | Enterprise Services Repository (ESR) | Design Tab (Integration Packages & iFlows) | Packages bundle iFlows, Value Mappings, Script Collections, and Message Mappings together. |
| **Directory Config** | Integration Directory (ID) | Externalized Parameters (`parameters.prop`) + Partner Directory | Environment endpoints, credentials, and business parameters are externalized per tenant (DEV/QA/PRD). |
| **Execution Model** | Java Adapter Engine (AEX) + Messaging System (MS) | Apache Camel Light Engine running on Cloud Foundry / Hyperscaler | Camel routes exchange Message objects (Headers, Properties, Body, Attachments). |
| **Protocol Adapter Execution**| J2EE Adapter Modules (EJB 3.0) | Standard Camel Components / Script Collections / Adapter SDK | Replace custom EJB adapter modules with Groovy scripts or reusable Camel steps. |

---

## 2. Artifact Mapping Breakdown

### 2.1 Interface & Data Definitions
* **Data Types (DT) & Message Types (MT):**
  * *SAP PO:* Defined as proprietary XML schemas in ESR.
  * *SAP CPI:* Standard XSD schemas imported into the `src.main.resources.xsd` directory or JSON Schemas for REST/OData APIs.
* **Service Interfaces (SI):**
  * *SAP PO:* Abstract, Inbound, Outbound WSDL contracts.
  * *SAP CPI:* WSDL or OpenAPI 3.0/Swagger specifications associated directly with SOAP/REST sender/receiver adapters.

### 2.2 Routing & Rules
* **Receiver Determination (ICO):**
  * *SAP PO:* XPath conditions inside Integrated Configuration Objects.
  * *SAP CPI:* **Router step** evaluating XML (`/xpath = 'value'`) or Non-XML (Groovy property check).
* **Interface Determination:**
  * *SAP PO:* 1-to-many message branch execution.
  * *SAP CPI:* **Parallel Multicast**, **Sequential Multicast**, or **General Splitter**.

### 2.3 Mappings & Logic
* **Graphical Message Mapping:**
  * *SAP PO:* `.mms` graphical mapping tool.
  * *SAP CPI:* Direct graphical Message Mapping tool supporting standard functions. Can be imported directly via ESR export (ZIP).
* **User Defined Functions (UDFs):**
  * *SAP PO:* Java code snippets (Simple, Queue, or Context execution).
  * *SAP CPI:* **Script Collection** using Apache Groovy 2.4/3.0 or JavaScript. Much simpler syntax and direct access to Message Properties/Headers.
* **RFC / JDBC Lookups:**
  * *SAP PO:* RFC Lookup API / JDBC Lookup inside Java UDFs.
  * *SAP CPI:* **Request-Reply step** before or within local integration processes. Do NOT execute lookups inside mapping if avoidable to keep transformations deterministic.

---

## 3. Communication Adapter Cross-Reference

| Legacy PO Adapter | Cloud Integration Adapter | Key Transition Notes |
| :--- | :--- | :--- |
| **IDoc_AAE** | **IDoc / OData** | S/4HANA can send IDocs directly via Cloud Connector or be modernized to OData APIs / Business Events. |
| **RFC** | **RFC Adapter** | Connects to on-prem ABAP instances using SAP Cloud Connector via RFC over WebSocket / SNC. |
| **FILE / FTP** | **SFTP / S3 / Azure Blob** | Deprecate unencrypted FTP. Use modern SFTP or native cloud object storage connectors. |
| **JDBC** | **JDBC / OData** | Direct DB querying via Cloud Connector JDBC, or prefer SAP Core Data Services (CDS) views exposed as OData. |
| **SOAP (XI3.0 / Axis)** | **SOAP (1.1 / 1.2 / RM)** | Supports standard WS-Security, basic authentication, and client certificate authentication (mTLS). |
| **REST** | **HTTP / HTTPS / REST** | Native REST sender/receiver with JSON/XML automatic negotiation and dynamic URI query param bindings. |
| **JMS** | **JMS / SAP Event Mesh** | Managed enterprise JMS queues in CPI (Enterprise Tenants) or external AMQP brokers via SAP Event Mesh. |

---

## 4. Key Step-by-Step Modernization Checklist

1. **Run SAP Integration Suite Migration Assessment:**
   * Activate the Migration Assessment capability in BTP to scan your SAP PO 7.5 system.
   * Review feasibility reports (Fully Automated, Semi-Automated, Manual Intervention).
2. **Harmonize Architecture into API-First & Event-Driven:**
   * Instead of point-to-point batch file interfaces, evaluate SAP Event Mesh for real-time notification + Cloud Integration for payload enrichment.
3. **Centralize Reusable Scripts:**
   * Consolidate repeated logging, error formatting, and token generation into shared **Script Collections**.
4. **Establish Automated Regression Testing:**
   * Validate payloads between PO and CPI using recorded production traffic to guarantee binary consistency.
