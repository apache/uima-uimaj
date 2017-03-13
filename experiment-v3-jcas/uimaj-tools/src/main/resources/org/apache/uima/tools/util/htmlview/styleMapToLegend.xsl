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
  XSL stylesheet that translates a style map XML file to the HTML page representing the Legend for the
  annotation viewer.  The legend identifies the styles for all the annotations, and provides a checkbox for
  each that can be used to turn on and off the display of that kind of annotation in the document view.
-->

<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:axsl="http://www.w3.org/1999/XSL/TransformAlias">

<xsl:template match="/">
<html>
<head>
  <link rel="StyleSheet" href="annotations.css" type="text/css" media="screen"/>
  <script type="text/javascript" src="annotationViewer.js"></script>
</head>
<body>
<h2>Legend</h2>
<form name="legendForm">

<!-- Add indvidual checkboxes -->
<xsl:apply-templates/>

<!-- Add "deselect all" button -->
<p/>
<input type="button" value="Select All">
  <xsl:attribute name="onClick">
    <xsl:for-each select="styleMap/rule">
      <xsl:variable name="checkboxName" select="translate(label,' .','__')"/>
      if (!document.legendForm.<xsl:value-of select="$checkboxName"/>.checked) { document.legendForm.<xsl:value-of select="$checkboxName"/>.click() }; 
    </xsl:for-each>
  </xsl:attribute>
</input>
<xsl:text disable-output-escaping="yes">&amp;nbsp;&amp;nbsp;</xsl:text>
<input type="button" value="Deselect All">
  <xsl:attribute name="onClick">
    <xsl:for-each select="styleMap/rule">
      <xsl:variable name="checkboxName" select="translate(label,' .','__')"/>
      if (document.legendForm.<xsl:value-of select="$checkboxName"/>.checked) { document.legendForm.<xsl:value-of select="$checkboxName"/>.click() }; 
    </xsl:for-each>
  </xsl:attribute>
</input>

</form>
</body>
</html>
</xsl:template>

<xsl:template match="rule">
  <xsl:variable name="label" select="label"/>
  <xsl:variable name="className" select="translate(label,' .','__')"/>
  <nobr>
  <input name="{$className}" type="checkbox" checked="true">
    <xsl:attribute name="onClick">
      changeStyle('span.<xsl:value-of select="$className"/>',this.checked ? '<xsl:value-of select="style"/>' : '')
    </xsl:attribute>
  </input>
  <span class="{$className}"><xsl:value-of select="$label"/></span>
  </nobr>
</xsl:template>

</xsl:stylesheet>