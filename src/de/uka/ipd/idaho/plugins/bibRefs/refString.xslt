<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="xml" version="1.0" encoding="UTF-8" indent="yes"/>
	<xsl:output omit-xml-declaration="yes"/>
	
	<xsl:template match="//bibRef">
		<xsl:call-template name="author"/><xsl:call-template name="year"/><xsl:call-template name="title"/><xsl:call-template name="hostVolume"/><xsl:call-template name="pagination"/><xsl:call-template name="isbn"/><xsl:call-template name="doi"/><xsl:call-template name="handle"/><xsl:call-template name="url"/><xsl:if test="./@type = 'book'"><xsl:call-template name="bookContentInfo"/></xsl:if>
		<xsl:text disable-output-escaping="yes">&#xa;</xsl:text>
	</xsl:template>
	
	<xsl:template name="author">
		<xsl:choose>
			<xsl:when test="not(./author) and ./editor and ./year"><xsl:call-template name="editors"/>, Ed<xsl:if test="count(./editor) > 1">s</xsl:if></xsl:when>
			<xsl:when test="not(./author) and ./editor"><xsl:call-template name="editors"/> (Ed<xsl:if test="count(./editor) > 1">s</xsl:if>)</xsl:when>
			<xsl:otherwise><xsl:call-template name="authors"/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="authors">
		<xsl:for-each select="./author"><xsl:if test="./preceding-sibling::author">, </xsl:if><xsl:value-of select="."/></xsl:for-each>
	</xsl:template>
	
	<xsl:template name="year">
		<xsl:if test="./year"> (<xsl:value-of select="./year"/>)</xsl:if>
	</xsl:template>
	<xsl:template name="title">
		<xsl:if test="./title"><xsl:text disable-output-escaping="yes">: </xsl:text><xsl:value-of select="./title"/><xsl:if test="not(contains('?!.', substring(./title, (string-length(./title)-0))))">.</xsl:if></xsl:if>
	</xsl:template>
	
	<xsl:template name="pagination">
		<xsl:choose>
			<xsl:when test="./pagination">: <xsl:value-of select="./pagination"/></xsl:when>
			<xsl:when test="./firstPage">: <xsl:value-of select="./firstPage"/><xsl:if test="./lastPage and not(./lastPage = ./firstPage)">-<xsl:value-of select="./lastPage"/></xsl:if></xsl:when>
			<xsl:otherwise/>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="isbn">
		<xsl:choose>
			<xsl:when test="./isbn">, ISBN:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./isbn, ' ', '')"/></xsl:when>
			<xsl:when test="./ISBN">, ISBN:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./ISBN, ' ', '')"/></xsl:when>
			<xsl:when test="./ID[./@type = 'ISBN']">, ISBN:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./ID[./@type = 'ISBN'], ' ', '')"/></xsl:when>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="doi">
		<xsl:choose>
			<xsl:when test="./doi">, DOI:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./doi, ' ', '')"/></xsl:when>
			<xsl:when test="./DOI">, DOI:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./DOI, ' ', '')"/></xsl:when>
			<xsl:when test="./ID[./@type = 'DOI']">, DOI:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./ID[./@type = 'DOI'], ' ', '')"/></xsl:when>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="handle">
		<xsl:choose>
			<xsl:when test="./handle">, Hdl:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./handle, ' ', '')"/></xsl:when>
			<xsl:when test="./Handle">, Hdl:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./Handle, ' ', '')"/></xsl:when>
			<xsl:when test="./ID[./@type = 'Handle']">, Hdl:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./ID[./@type = 'Handle'], ' ', '')"/></xsl:when>
			<xsl:when test="./ID[./@type = 'Hdl']">, Hdl:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./ID[./@type = 'Hdl'], ' ', '')"/></xsl:when>
		</xsl:choose>
	</xsl:template>
	<xsl:template name="url">
		<xsl:choose>
			<xsl:when test="./url">, URL:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./url, ' ', '')"/></xsl:when>
			<xsl:when test="./publicationUrl">, URL:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./publicationUrl, ' ', '')"/></xsl:when>
			<xsl:when test="./URL">, URL:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./URL, ' ', '')"/></xsl:when>
			<xsl:when test="./ID[./@type = 'URL']">, URL:<xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="translate(./ID[./@type = 'URL'], ' ', '')"/></xsl:when>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="hostVolume">
		<xsl:choose>
			<xsl:when test="./@type = 'book'"><xsl:call-template name="bookVolume"/><xsl:call-template name="locationAndPublisher"/></xsl:when>
			<xsl:when test="./@type = 'book chapter'"> In<xsl:call-template name="editor"/><xsl:call-template name="volumeTitle"/><xsl:call-template name="bookVolume"/><xsl:call-template name="locationAndPublisher"/></xsl:when>
			<xsl:when test="./@type = 'journal volume' and ./journal and not(./journal = ./title)"><xsl:call-template name="journal"/></xsl:when>
			<xsl:when test="./@type = 'journal volume'"><xsl:call-template name="parts"/></xsl:when>
			<xsl:when test="./@type = 'journal article' and ./volumeTitle"> In<xsl:call-template name="editor"/><xsl:call-template name="volumeTitle"/><xsl:call-template name="journal"/></xsl:when>
			<xsl:when test="./@type = 'journal article'"><xsl:call-template name="journal"/></xsl:when>
			<xsl:when test="./@type = 'proceedings'"><xsl:call-template name="locationAndPublisher"/></xsl:when>
			<xsl:when test="./@type = 'proceedings paper'"> In<xsl:call-template name="editor"/><xsl:call-template name="volumeTitle"/><xsl:call-template name="locationAndPublisher"/></xsl:when>
			<xsl:when test="./@type = 'url'"/>
			<xsl:otherwise>UNKNOWN_REFERENCE_TYPE</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="editor">
		<xsl:if test="./editor">: <xsl:call-template name="editors"/> (Ed<xsl:if test="count(./editor) > 1">s</xsl:if>)</xsl:if>
	</xsl:template>
	<xsl:template name="editors">
		<xsl:if test="./editor"><xsl:for-each select="./editor"><xsl:if test="./preceding-sibling::editor">, </xsl:if><xsl:value-of select="."/></xsl:for-each></xsl:if>
	</xsl:template>
	
	<xsl:template name="volumeTitle"><!-- TODO figure out why substring has to go to string-lenth to get last character -->
		<xsl:if test="./volumeTitle">:<xsl:for-each select="./volumeTitle"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="."/><xsl:if test="not(contains('?!.', substring(., (string-length(.)-0))))">.</xsl:if></xsl:for-each></xsl:if>
	</xsl:template>
	
	<xsl:template name="bookVolume">
		<xsl:if test="./volume"> Volume <xsl:value-of select="./volume"/>.</xsl:if>
	</xsl:template>
	
	<xsl:template name="journal">
		<xsl:choose>
			<xsl:when test="./journal"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="./journal"/><xsl:if test="./seriesInJournal"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text>(<xsl:value-of select="./seriesInJournal"/>)</xsl:if><xsl:call-template name="parts"/></xsl:when>
			<xsl:when test="./journalOrPublisher"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="./journalOrPublisher"/><xsl:if test="./seriesInJournal"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text>(<xsl:value-of select="./seriesInJournal"/>)</xsl:if><xsl:call-template name="parts"/></xsl:when>
			<xsl:otherwise>UNKNOWN_JOURNAL</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="parts">
		<xsl:choose>
			<xsl:when test="./numero and ./issue and ./volume"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="./volume"/> (<xsl:value-of select="./issue"/>), No. <xsl:value-of select="./numero"/></xsl:when>
			<xsl:when test="./numero and ./issue"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="./issue"/>, No. <xsl:value-of select="./numero"/></xsl:when>
			<xsl:when test="./numero and ./volume"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="./volume"/>, No. <xsl:value-of select="./numero"/></xsl:when>
			<xsl:when test="./issue and ./volume"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="./volume"/> (<xsl:value-of select="./issue"/>)</xsl:when>
			<xsl:when test="./volume"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="./volume"/></xsl:when>
			<xsl:when test="./numero"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="./numero"/></xsl:when>
			<xsl:when test="./issue"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="./issue"/></xsl:when>
			<xsl:otherwise/>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="locationAndPublisher">
		<xsl:choose>
			<xsl:when test="./publisher and ./location"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="./location"/>: <xsl:value-of select="./publisher"/></xsl:when>
			<xsl:when test="./publisher"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="./publisher"/></xsl:when>
			<xsl:when test="./location"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="./location"/></xsl:when>
			<xsl:when test="./journalOrPublisher"><xsl:text disable-output-escaping="yes">&#x20;</xsl:text><xsl:value-of select="./journalOrPublisher"/></xsl:when>
			<xsl:otherwise/>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="bookContentInfo">
		<xsl:for-each select="./bookContentInfo">, <xsl:value-of select="."/></xsl:for-each>
	</xsl:template>
</xsl:stylesheet>
