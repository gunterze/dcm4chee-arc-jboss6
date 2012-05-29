<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml"/>
  <xsl:include href="dcm4chee-arc-hl7-common.xsl"/>
  <xsl:template match="/hl7">
    <NativeDicomModel>
      <xsl:apply-templates select="PID"/>
      <xsl:apply-templates select="OBR[1]" mode="first"/>
      <xsl:apply-templates select="ZDS"/>
    </NativeDicomModel>
  </xsl:template>
  <xsl:template match="OBR" mode="first">
    <!--  Procedure Code Sequence-->
    <xsl:call-template name="codeItem">
      <xsl:with-param name="sqtag" select="'00081032'"/>
      <xsl:with-param name="code" select="field[44]/text()"/>
      <xsl:with-param name="scheme" select="field[44]/component[2]"/>
      <xsl:with-param name="meaning" select="field[44]/component[1]"/>
    </xsl:call-template>
  </xsl:template>
  <xsl:template match="ZDS">
    <!-- Study Instance UID -->
    <xsl:call-template name="attr">
      <xsl:with-param name="tag" select="'0020000D'"/>
      <xsl:with-param name="vr" select="'UI'"/>
      <xsl:with-param name="val" select="field[1]/text()"/>
    </xsl:call-template>
  </xsl:template>
</xsl:stylesheet>
