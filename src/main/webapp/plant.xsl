<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:res="http://www.w3.org/2005/sparql-results#" exclude-result-prefixes="res">

	<xsl:template match="/">
		<html>
			<head>
			</head>
			<body>
				<h1><xsl:value-of select="/res:sparql/res:results/res:result/res:binding[name='label']"/></h1>
			</body>
		</html>
	</xsl:template>
</xsl:stylesheet>