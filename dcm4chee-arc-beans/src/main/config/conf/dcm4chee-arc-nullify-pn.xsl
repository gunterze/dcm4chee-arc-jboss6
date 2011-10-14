<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
  <xsl:output method="xml"/>
  <xsl:template match="/NativeDicomModel">
    <NativeDicomModel>
      <xsl:apply-templates select="DicomAttribute[@vr=PN]"/>
    </NativeDicomModel>
  </xsl:template>
  <xsl:template match="DicomAttribute[@vr=PN]">
    <xsl:copy>
      <xsl:for-each select="@*">
        <xsl:copy/>
      </xsl:for-each>
    </xsl:copy>
  </xsl:template>
</xsl:stylesheet>
