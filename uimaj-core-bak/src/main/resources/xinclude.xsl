<?xml version="1.0"?>

	<!--
	 ***************************************************************
	 * Licensed to the Apache Software Foundation (ASF) under one
	 * or more contributor license agreements.  See the NOTICE file
	 * distributed with this work for additional information
	 * regarding copyright ownership.  The ASF licenses this file
	 * to you under the Apache License, Version 2.0 (the
	 * "License"); you may not use this file except in compliance
	 * with the License.  You may obtain a copy of the License at
         *
	 *   http://www.apache.org/licenses/LICENSE-2.0
	 * 
	 * Unless required by applicable law or agreed to in writing,
	 * software distributed under the License is distributed on an
	 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
	 * KIND, either express or implied.  See the License for the
	 * specific language governing permissions and limitations
	 * under the License.
	 ***************************************************************
   -->

<!-- XSL stylesheet that can implements a subset of the XInclude specification.
     Supports hrefs of the form URL#xpointer(XPathExpression). 
-->
<xsl:stylesheet 
  xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
  xmlns:xi="http://www.w3.org/2001/XInclude"
  xmlns:exsl="http://exslt.org/common"
  xmlns:dyn="http://exslt.org/dynamic"
  extension-element-prefixes="exsl dyn"
  version="1.0">

<xsl:output method="xml" version="1.0" encoding="UTF-8"/>

<!-- Parameter controlling whether <xincluded> placeholder elements should be used. -->
<xsl:param name="leavePlaceholderElements" select="false"/>

<!-- Identity transform --> 
<xsl:template match="@*|node()">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>


<!-- Processes xi:include element-->
<xsl:template match="xi:include">
  <!-- Leave a Placeholder Tag if requested -->	
  <xsl:choose>
    <xsl:when test="$leavePlaceholderElements='true'">
      <xsl:element name="xincluded">
        <xsl:attribute name="href"><xsl:value-of select="@href"/></xsl:attribute>
      	<xsl:call-template name="processXInclude"/>
      </xsl:element>
    </xsl:when>
    <xsl:otherwise>
      <xsl:call-template name="processXInclude"/>
    </xsl:otherwise>	
  </xsl:choose> 
</xsl:template>

<!-- Continue XInclude Processing -->
<xsl:template name="processXInclude">  
  <!-- Determine if the href is an xpointer -->
  <xsl:choose>
    <xsl:when test="contains(@href,'#xpointer(')">
      <!-- It is an Xpointer.  Extract the include filename and the xpointer expression -->
      <xsl:variable name="before" select="substring-before(@href,'#')"/>
      <xsl:variable name="after" select="substring-after(@href,'#xpointer(')"/>
      <xsl:variable name="xpath" select="substring($after,1,string-length($after)-1)"/>

      <!-- Process nested XIncludes -->
      <xsl:variable name="afterNestedIncludes">
        <xsl:apply-templates select="document($before,.)"/>
      </xsl:variable>        

      <!-- Call another template to take the entire included document and just extract the
           part pointed to by the xpointer. -->
      <xsl:apply-templates select="exsl:node-set($afterNestedIncludes)" mode="extractXPathTarget">
        <xsl:with-param name="xpath"><xsl:value-of select="$xpath"/></xsl:with-param>
      </xsl:apply-templates>

    </xsl:when>

    <xsl:otherwise>
      <!-- Not an XPointer - include entire document. --> 
      <xsl:apply-templates select="document(@href,.)"/>
    </xsl:otherwise>

  </xsl:choose>
</xsl:template>

<!-- This template applies a dynanmic XPath expression to the current context node. 
     Dynamically evaluating an XPath expression requires an XSLT extension (Which Xalan provides).     
-->
<xsl:template match="/" mode="extractXPathTarget">
  <xsl:param name="xpath"/>
  <xsl:copy-of select="dyn:evaluate(string($xpath))"/>
</xsl:template>

</xsl:stylesheet>
