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
  XSL stylesheet to be included in stylesheets used for transforming inline XML annotations to the
  HTML representation used in the annotation viewer.

  Stylesheets should include this stylesheet and extend it by adding individual rules for all
  annotations that are to be displayed.  Each rule should call the "Annotation" rule defined in
  this stylesheet, and pass a value for the "label" parameter.  This label (with any spaces replaced
  by underscores) should match the name of a style defined in the annotaitons.css stylesheet.   
-->


<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">


<xsl:template match="/">
  <html>
  <head>
    <LINK REL="StyleSheet" HREF="annotations.css" TYPE="text/css" MEDIA="screen"/>

    <script type="text/javascript" src="annotationViewer.js"></script>
  </head>
  <body onLoad="init()" onClick="toggleHoldInfoFrame()">
    <pre><xsl:apply-templates/></pre>
  </body>
  </html>
</xsl:template>

<xsl:template name="Annotation">
  <xsl:param name="label"/>
  <xsl:variable name="className" select="translate($label,' .','__')"/>
  <xsl:variable name="annotType" select="name()"/>
  <xsl:variable name="toReplace" select='"&#10;&#13;&apos;"'/>
  <xsl:variable name="spannedText" select="translate(.,$toReplace,'  ')"/>
  <span class="{$className}"
      onMouseOut="clearInfoFrame()">
    <xsl:attribute name="onMouseOver">writeAnnotationInfo('<xsl:value-of select="$className"/>','<xsl:value-of select="$spannedText"/>','<xsl:value-of select="name()"/>',new Array(<xsl:for-each select="@*">new Array('<xsl:value-of select="name()"/>','<xsl:value-of select="translate(.,$toReplace,'  ')"/>')<xsl:if test="position() != last()">,</xsl:if></xsl:for-each>));</xsl:attribute>    
    <xsl:apply-templates/>
  </span>
</xsl:template> 

</xsl:stylesheet>