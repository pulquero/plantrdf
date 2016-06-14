<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
	xmlns:p="http://plantrdf-morethancode.rhcloud.com/schema#"
	exclude-result-prefixes="rdf">

	<xsl:variable name="plant">
	<xsl:value-of select="/rdf:RDF/rdf:Description[rdf:type/@rdf:resource='http://plantrdf-morethancode.rhcloud.com/schema#Plant']/@rdf:about"/>
	</xsl:variable>

	<xsl:template match="/">
		<html>
			<head>
			</head>
			<body>
				<h1><xsl:value-of select="/rdf:RDF/rdf:Description[@rdf:about=$plant]/rdfs:label"/></h1>
				<table style="th:after {{content: ':'}}">
				<xsl:apply-templates select="/rdf:RDF/rdf:Description[@rdf:about=$plant]/*"/>
				</table>
			</body>
		</html>
	</xsl:template>

	<xsl:template match="*">
		<xsl:variable name="property">
		<xsl:value-of select="concat(namespace-uri(),name())"/>
		</xsl:variable>
		<xsl:variable name="label">
		<xsl:value-of select="/rdf:RDF/rdf:Description[@rdf:about=$property]/rdfs:label"/>
		</xsl:variable>
		<tr><th><xsl:value-of select="$label"/></th><td><xsl:value-of select=".[$property!='http://www.w3.org/2000/01/rdf-schema#label' and $label]"/></td></tr>
	</xsl:template>
</xsl:stylesheet>