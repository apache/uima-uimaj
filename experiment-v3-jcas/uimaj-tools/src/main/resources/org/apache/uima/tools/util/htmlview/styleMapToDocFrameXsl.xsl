<?xml version="1.0" encoding="ISO-8859-1"?>
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

<!-- 
  This XSL stylesheet transforms a style map XML file into an XSL stylesheet that is used in the annotation viewer.
  That's right, applying this stylesheet generates a new XSL stylesheet as output.  The generated stylesheet can then
  be applied to a document with inline XML annotations in order to convert it to the HTML document that is used
  in the annotation viewer. 
-->

<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:axsl="http://www.w3.org/1999/XSL/TransformAlias"
xmlns:baxsl="http://www.w3.org/1999/XSL/TransformAliasBlank">

<xsl:namespace-alias stylesheet-prefix="axsl" result-prefix="xsl"/>
<xsl:namespace-alias stylesheet-prefix="baxsl" result-prefix=""/>

<xsl:template match="/">
  <axsl:stylesheet version="1.0" xmlns="http://www.w3.org/1999/XSL/Transform">
  <axsl:include baxsl:href="annotations.xsl"/>

  <xsl:apply-templates/> 
  
  </axsl:stylesheet>
</xsl:template>

<xsl:template match="rule">
  <axsl:template>
    <xsl:attribute name="match"><xsl:value-of select="pattern"/></xsl:attribute>

    <axsl:call-template name="Annotation">
      <axsl:with-param name="label"><xsl:value-of select="label"/></axsl:with-param>
    </axsl:call-template>
  </axsl:template>
</xsl:template>

</xsl:stylesheet>