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

<!-- This is the common xsl parameterization shared by all formats -->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                version='1.0'>

  <xsl:attribute-set name="root.properties">
      <xsl:attribute name="text-align">left</xsl:attribute>
  </xsl:attribute-set>

  <!-- ##################
       # Admonitions    #
       ################## -->   

  <!-- Use nice graphics for admonitions 
  <xsl:param name="admon.graphics">'1'</xsl:param>
    -->
  <xsl:param name="admon.graphics.path" select="'../images/'"/>
  <xsl:param name="navig.graphics.path" select="'../images/'"/>
  <xsl:param name="callout.graphics.path" select="'../images/callouts/'"/>
  <xsl:param name="admon.graphics.extension">.gif</xsl:param>
  <xsl:param name="callout.graphics.extension">.gif</xsl:param>
 
<!--###################################################
                      olink styling
    ################################################### -->  
  <xsl:param name="olink.doctitle" select="'yes'"/>
  
  <!--###################################################
                         Labels
    ################################################### -->

    <!-- Label Chapters and Sections (numbering) -->
    <xsl:param name="chapter.autolabel" select="1"/>
    <xsl:param name="section.autolabel" select="1"/>
    <xsl:param name="section.autolabel.max.depth" select="3"/>

    <xsl:param name="section.label.includes.component.label" select="1"/>
  
    <xsl:param name="table.footnote.number.format" select="'1'"/>
 

<!--###################################################
                         Callouts
    ################################################### -->

    <!-- don't use images for callouts -->
    <xsl:param name="callout.graphics">0</xsl:param>
    <xsl:param name="callout.unicode">1</xsl:param>

    <!-- Place callout marks at this column in annotated areas -->
    <xsl:param name="callout.defaultcolumn">90</xsl:param>

  <!--###################################################
                      Table of Contents
    ################################################### -->

    <xsl:param name="generate.toc">
      book      toc,title
    </xsl:param>

  <!--###################################################
                          Misc
    ################################################### -->

    <!-- have ulinks with text show the link in a footnote -->
    <xsl:param name="ulink.footnotes" select="1"/>
    <xsl:param name="ulink.show" select="1"/>

    <!-- Glossary indent -->
    <xsl:param name="glossterm.width" select="'1.4in'"/>

    <!-- Placement of titles -->
    <xsl:param name="formal.title.placement">
        figure after
        example after
        equation before
        table before
        procedure before
    </xsl:param>

  <!-- Format Variable Lists as Blocks (prevents horizontal overflow) -->
  <xsl:param name="variablelist.as.blocks">1</xsl:param>
  
</xsl:stylesheet>

