<?xml version="1.0" encoding="UTF-8"?>

<!--
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'>

  <!-- next 3 cause the role="xxx" to be propagated as class="xxx" in the output html -->
  <xsl:param name="para.propagates.style" select="1"/>
  <xsl:param name="emphasis.propagates.style" select="1"/>
  <xsl:param name="phrase.propagates.style" select="1"/>
 
  <!-- turn on id attributes for major components --> 
  <xsl:param name="generate.id.attributes" select="1"/>

  <xsl:param name="chunker.output.indent" select="'yes'"/>
  <xsl:param name="chunker.output.doctype-public" select="'-//W3C//DTD HTML 4.0 Transitional//EN'"/>
  <xsl:param name="table.frame.border.color" select="'black'"/>
  <xsl:param name="table.cell.border.color"  select="'black'"/>

  <xsl:param name="table.borders.with.css" select="1"/>
  <xsl:param name="html.stylesheet.type">text/css</xsl:param>           

  <!--
  <xsl:param name="use.extensions">1</xsl:param>  causes lots of failed to load image msgs !
  <xsl:param name="graphicsize.extension">1</xsl:param>
    -->

<!-- Remove "Chapter" from the Chapter titles... 
  <xsl:param name="local.l10n.xml" select="document('')"/>
  <l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0">
    <l:l10n language="en">
      <l:context name="title-numbered">
        <l:template name="chapter" text="%n.&#160;%t"/>
        <l:template name="section" text="%n&#160;%t"/>
      </l:context>
    </l:l10n>
  </l:i18n>
  -->    
</xsl:stylesheet>
