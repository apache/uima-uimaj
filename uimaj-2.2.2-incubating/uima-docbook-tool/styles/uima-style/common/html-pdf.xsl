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
               xmlns:exsl="http://exslt.org/common"
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

  <!-- expand plain imageobject nodes into doubles, one for fo output the other for html output
       Scale the html output by 1.1 for inches or 1.4 for pixels 
       use test="$fop.version = '0.93' to do fop versioning -->
  
  <!-- scale=xxx is ignored in 0.20.5, but works in 0.93 -->
  
  <xsl:template match="mediaobject[imageobject[not(@role)]]">
    <xsl:variable name="id1" select="imageobject/imagedata"/>
    <xsl:variable name="width" select="string(imageobject/imagedata/@width)"/>
    <xsl:variable name="scale" select="string(imageobject/imagedata/@scale)"/>
    
    <xsl:variable name="width_number"
      select="substring($width, 1, string-length($width) - 2)"/>      
    
    <xsl:variable name="width_unit"
      select="substring($width, string-length($width) - 1)"/>
    
    <xsl:variable name="scale_factor">
      <xsl:choose>
        <xsl:when test="$width_unit = 'px'">1.37</xsl:when>
        <xsl:otherwise>1.1</xsl:otherwise>
      </xsl:choose>
    </xsl:variable>
          
    <xsl:variable name="n">
      <mediaobject>
        <xsl:choose>
          <xsl:when test="$width"> 
            <imageobject role="html">
              <imagedata width="{concat($width_number * $scale_factor, $width_unit)}"
                format="{$id1/@format}" fileref="{$id1/@fileref}"/>
            </imageobject>
            <imageobject role="fo">
              <imagedata width="{concat($width_number, $width_unit)}" format="{$id1/@format}"
                fileref="{$id1/@fileref}"/>
            </imageobject>
          </xsl:when>
          <xsl:when test="$scale">
            <imageobject role="html">
              <imagedata scale="{$scale}"
                format="{$id1/@format}" fileref="{$id1/@fileref}"/>
            </imageobject>
            <imageobject role="fo">
              <imagedata scale="{round(number($scale * .71))}" format="{$id1/@format}"
                fileref="{$id1/@fileref}"/>
            </imageobject>
          </xsl:when>
        </xsl:choose>
        
        <textobject><phrase><xsl:value-of select="textobject/phrase"/></phrase></textobject>
      </mediaobject>
    </xsl:variable>
    
    <xsl:apply-templates select="exsl:node-set($n)/*"/>
  </xsl:template>
  
</xsl:stylesheet>

