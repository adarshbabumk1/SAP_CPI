<?xml version="1.0" encoding="UTF-8"?>
<!--
    XSLT 2.0 / 3.0 Mapping: SAP IDoc (ORDERS05) to Canonical Enterprise JSON Order
    Used in SAP Cloud Integration to bypass graphical message mapping overhead.
    
    Author: Adarsh (SAP Integration Architect)
-->
<xsl:stylesheet version="2.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:xs="http://www.w3.org/2001/XMLSchema">

    <xsl:output method="text" encoding="UTF-8" media-type="application/json"/>

    <xsl:template match="/">
        <xsl:variable name="idoc" select="//ORDERS05/IDOC"/>
        <xsl:variable name="edi_dc40" select="$idoc/EDI_DC40"/>
        
        {
            "metadata": {
                "sourceIdocNumber": "<xsl:value-of select="$edi_dc40/DOCNUM"/>",
                "messageType": "<xsl:value-of select="$edi_dc40/MESTYP"/>",
                "senderPartner": "<xsl:value-of select="$edi_dc40/SNDPRN"/>",
                "recipientPartner": "<xsl:value-of select="$edi_dc40/RCVPRN"/>",
                "creationTimestamp": "<xsl:value-of select="concat($edi_dc40/CREDAT, 'T', $edi_dc40/CRETIM)"/>"
            },
            "order": {
                "orderNumber": "<xsl:value-of select="$idoc/E1EDK01/BELNR"/>",
                "currency": "<xsl:value-of select="$idoc/E1EDK01/CURCY"/>",
                "documentDate": "<xsl:value-of select="$idoc/E1EDK03[IDDAT='012']/DATUM"/>",
                "parties": [
                    <xsl:for-each select="$idoc/E1EDKA1">
                    {
                        "role": "<xsl:value-of select="PARVW"/>",
                        "partnerId": "<xsl:value-of select="PARTN"/>",
                        "name": "<xsl:value-of select="normalize-space(NAME1)"/>",
                        "city": "<xsl:value-of select="normalize-space(ORT01)"/>",
                        "country": "<xsl:value-of select="normalize-space(LAND1)"/>"
                    }<xsl:if test="position() != last()">,</xsl:if>
                    </xsl:for-each>
                ],
                "items": [
                    <xsl:for-each select="$idoc/E1EDP01">
                    {
                        "itemNumber": "<xsl:value-of select="POSEX"/>",
                        "material": "<xsl:value-of select="E1EDP19[QUALF='002']/IDTNR"/>",
                        "quantity": <xsl:value-of select="MENGE"/>,
                        "unitOfMeasure": "<xsl:value-of select="MENEE"/>",
                        "netPrice": <xsl:value-of select="if (E1EDP26[QUALF='003']/BETRG) then E1EDP26[QUALF='003']/BETRG else '0.00'"/>
                    }<xsl:if test="position() != last()">,</xsl:if>
                    </xsl:for-each>
                ]
            }
        }
    </xsl:template>

</xsl:stylesheet>
