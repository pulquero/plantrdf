<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet version="1.0" xmlns="http://www.w3.org/1999/xhtml" xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" exclude-result-prefixes="rdf">

	<xsl:param name="resource" select="''" />
	<xsl:param name="updateEndpoint" select="''" />

	<xsl:template match="/">
		<html>
			<head>
				<style>
					h1 {
					font-size: 200%
					}

					h2 {
					font-size: 150%
					}

					.predicate {
					font-size: 120%;
					font-weight: bold
					}

					.object {
					font-size:
					110%
					}
				</style>
			</head>
			<body>
				<h1>About</h1>
				<xsl:apply-templates
					select="rdf:RDF/rdf:Description[@rdf:about=$resource or substring-before(@rdf:about, '#')=$resource]" />
				<h1>Referenced by</h1>
				<xsl:apply-templates
					select="rdf:RDF/rdf:Description[not(@rdf:about=$resource or substring-before(@rdf:about, '#')=$resource)]" />
			</body>
		</html>
	</xsl:template>

	<xsl:template match="rdf:Description">
		<h2>
			<xsl:if test="contains(@rdf:about, '#')">
				<xsl:attribute name="id"><xsl:value-of select="substring-after(@rdf:about, '#')" /></xsl:attribute>
			</xsl:if>
			<a href="{@rdf:about}">
				<xsl:value-of select="@rdf:about" />
			</a>
		</h2>
		<xsl:choose>
			<xsl:when test="$updateEndpoint">
				<xsl:apply-templates select="*" mode="edit" />
			</xsl:when>
			<xsl:otherwise>
				<table>
					<tbody>
						<xsl:apply-templates select="*" />
					</tbody>
				</table>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>

	<xsl:template match="node()">
		<tr>
			<td class="predicate">
				<xsl:value-of select="name()" />
			</td>
			<td class="object">
				<xsl:choose>
					<xsl:when test="@rdf:resource">
						<a href="{@rdf:resource}">
							<xsl:value-of select="@rdf:resource" />
						</a>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="text()" />
					</xsl:otherwise>
				</xsl:choose>
			</td>
		</tr>
	</xsl:template>


	<xsl:template match="node()" mode="edit">
		<xsl:variable name="subject">
			<xsl:value-of select="concat('<', ../@rdf:about, '>')" />
		</xsl:variable>
		<xsl:variable name="predicate">
			<xsl:value-of select="concat('<', namespace-uri(), local-name(), '>')" />
		</xsl:variable>
		<xsl:variable name="object">
			<xsl:choose>
				<xsl:when test="@rdf:resource">
					<xsl:value-of select="concat('<', @rdf:resource, '>')" />
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="concat('&quot;&quot;&quot;', text(), '&quot;&quot;&quot;')" />
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		<form action="{$updateEndpoint}">
			<input name="$s" type="url" value="{$subject}"/>
			<input name="$p" type="url" value="{$predicate}"/>
			<input name="$o" value="{$object}"/>
			<input name="update" type="hidden">
				<xsl:attribute name="value">
				<xsl:text>delete { </xsl:text>
				<xsl:value-of select="$subject"/>
				<xsl:text> </xsl:text>
				<xsl:value-of select="$object"/>
				<xsl:text> </xsl:text>
				<xsl:value-of select="$predicate"/>
				<xsl:text> } insert { ?s ?p ?o }</xsl:text>
				</xsl:attribute>
			</input>
			<button>Update</button>
		</form>
	</xsl:template>
</xsl:stylesheet>