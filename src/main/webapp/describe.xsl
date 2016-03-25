<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0"
xmlns="http://www.w3.org/1999/xhtml"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
exclude-result-prefixes="rdf">
<xsl:template match="/">
<html>
<head>
<style>
h1 {
	font-size: 150%
}

.predicate {
	font-size: 120%;
	font-weight: bold
}

.object {
	font-size: 110%
}
</style>
</head>
<body>
<xsl:apply-templates select="rdf:RDF/rdf:Description"/>
</body>
</html>
</xsl:template>

<xsl:template match="rdf:Description">
<h1>
<xsl:if test="contains(@rdf:about, '#')">
<xsl:attribute name="id"><xsl:value-of select="substring-after(@rdf:about, '#')"/></xsl:attribute>
</xsl:if>
<a href="{@rdf:about}"><xsl:value-of select="@rdf:about"/></a>
</h1>
<table>
<tbody>
  <xsl:apply-templates select="*"/>
</tbody>
</table>
</xsl:template>

<xsl:template match="node()">
<tr>
<td class="predicate"><xsl:value-of select="name()"/></td>
<td class="object">
<xsl:choose>
<xsl:when test="@rdf:resource">
<a href="{@rdf:resource}"><xsl:value-of select="@rdf:resource"/></a>
</xsl:when>
<xsl:otherwise>
<xsl:value-of select="text()"/>
</xsl:otherwise>
</xsl:choose>
</td>
</tr>
</xsl:template>
</xsl:stylesheet>