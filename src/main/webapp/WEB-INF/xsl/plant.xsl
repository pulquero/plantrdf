<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
	xmlns:p="http://plantrdf-morethancode.rhcloud.com/schema#"
	exclude-result-prefixes="rdf">

	<xsl:template match="/">
		<xsl:variable name="plant">
		<xsl:value-of select="/rdf:RDF/rdf:Description[rdf:type/@rdf:resource='http://plantrdf-morethancode.rhcloud.com/schema#Plant']/@rdf:about"/>
		</xsl:variable>
		<html>
			<head>
			</head>
			<body>
				<h1><xsl:value-of select="/rdf:RDF/rdf:Description[@rdf:about=$plant]/rdfs:label"/></h1>
				<table>
				<tr><th>Last watered:</th><td><xsl:value-of select="/rdf:RDF/rdf:Description[@rdf:about=$plant]/p:lastWatered"/></td></tr>
				</table>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>