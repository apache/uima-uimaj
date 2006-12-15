<?xml version="1.0" encoding="UTF-8"?>

<!--
 Copyright 2006 The Apache Software Foundation.

 Licensed under the Apache License, Version 2.0 (the "License")
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:fo="http://www.w3.org/1999/XSL/Format"
                version='1.0'>
  
  <xsl:param name="body.font.family"  select="'Palatino'"/>
  <xsl:param name="body.font.master"    select="'10.5'"/>
  <xsl:attribute-set name="root.properties">
      <xsl:attribute name="text-align">left</xsl:attribute>
  </xsl:attribute-set>
  
  
<!--###################################################
                      olink styling
    ################################################### -->
  <xsl:param name="insert.xref.page.number" select="'yes'"/>
  <xsl:param name="insert.olink.pdf.frag" select="1"/>

  <!--###################################################
                      xref (and ulink) styling
    ################################################### -->
  <xsl:attribute-set name="xref.properties">
      <xsl:attribute name="color">blue</xsl:attribute>
  </xsl:attribute-set>
  
  <!--###################################################
                      Monospace font size
    ################################################### -->
  
  <xsl:attribute-set name="monospace.properties">
    <xsl:attribute name="font-size">9.5pt</xsl:attribute>
  </xsl:attribute-set>
 
  <xsl:attribute-set name="monospace.verbatim.properties">
    <xsl:attribute name="font-size">
      <xsl:choose>
        <xsl:when test="processing-instruction('db-font-size')">
          <xsl:value-of select="processing-instruction('db-font-size')"/>
        </xsl:when>
        <xsl:otherwise>9pt</xsl:otherwise>
      </xsl:choose>
    </xsl:attribute>
  </xsl:attribute-set>

<!--###################################################
                      Page margins
    ################################################### -->   
  <xsl:param name="page.margin.top" select="'1cm'"/>
  <xsl:param name="region.before.extent" select="'1cm'"/>
  <xsl:param name="body.margin.top" select="'1.5cm'"/>

  <xsl:param name="body.margin.bottom" select="'1.5cm'"/>
  <xsl:param name="region.after.extent" select="'1cm'"/>
  <xsl:param name="page.margin.bottom" select="'1cm'"/>
  <xsl:param name="title.margin.left" select="'-4pc'"/>
  
  <xsl:param name="page.margin.inner" select="'1.3in'"/>
  <xsl:param name="page.margin.outer" select="'.70in'"/>
<!--###################################################
                      Header
    ################################################### -->

    <!-- More space in the center header for long text -->
    <xsl:attribute-set name="header.content.properties">
        <xsl:attribute name="font-family">
            <xsl:value-of select="$body.font.family"/>
        </xsl:attribute>
        <xsl:attribute name="margin-left">-5em</xsl:attribute>
        <xsl:attribute name="margin-right">-5em</xsl:attribute>
    </xsl:attribute-set>

<!--###################################################
                      Custom Footer
    ################################################### -->
    <!-- width specifications: inside, center, outside -->
    <xsl:param name="footer.column.widths">2 6 1</xsl:param>
  
    <xsl:template name="footer.content">
        <xsl:param name="pageclass" select="''"/>
        <xsl:param name="sequence" select="''"/>
        <xsl:param name="position" select="''"/>
        <xsl:param name="gentext-key" select="''"/>

        <xsl:variable name="Version">
          <xsl:choose>
            <xsl:when test="//productname">
              <xsl:value-of select="//productname"/><xsl:text> </xsl:text>
            </xsl:when>
            <xsl:otherwise>
              <xsl:text>please define productname in your docbook file!</xsl:text>
            </xsl:otherwise>
          </xsl:choose>

          <xsl:choose>
            <xsl:when test="//releaseinfo">
              <xsl:value-of select="//releaseinfo"/>
            </xsl:when>
            <xsl:otherwise>
                <!-- nop -->
            </xsl:otherwise>
          </xsl:choose>
        </xsl:variable>

        <xsl:variable name="Title">
          <!-- <xsl:value-of select="//title"/> -->
          <xsl:apply-templates select="." mode="titleabbrev.markup"/>
        </xsl:variable>

        <xsl:choose>
          <xsl:when test="$sequence='blank'">
            <xsl:choose>
              <xsl:when test="$double.sided != 0 and $position = 'left'">
                <xsl:value-of select="$Version"/>
              </xsl:when>

              <xsl:when test="$double.sided = 0 and $position = 'center'">
                <!-- nop -->
              </xsl:when>

              <xsl:otherwise>
                <fo:page-number/>
              </xsl:otherwise>
            </xsl:choose>
          </xsl:when>

          <xsl:when test="$pageclass='titlepage'">
          <!-- nop: other titlepage sequences have no footer -->
          </xsl:when>

          <xsl:when test="$double.sided != 0 and $sequence = 'even' and $position='left'">
            <fo:page-number/>
          </xsl:when>

          <xsl:when test="$double.sided != 0 and $sequence = 'odd' and $position='right'">
            <fo:page-number/>
          </xsl:when>

          <xsl:when test="$double.sided = 0 and $position='right'">
           <fo:page-number/>
          </xsl:when>

          <xsl:when test="$double.sided != 0 and $sequence = 'odd' and $position='left'">
            <xsl:value-of select="$Version"/>
          </xsl:when>

          <xsl:when test="$double.sided != 0 and $sequence = 'even' and $position='right'">
            <xsl:value-of select="$Version"/>
          </xsl:when>

          <xsl:when test="$double.sided = 0 and $position='left'">
            <xsl:value-of select="$Version"/>
          </xsl:when>

          <xsl:when test="$position='center'">
            <xsl:value-of select="$Title"/>
          </xsl:when>

          <xsl:otherwise>
          <!-- nop -->
          </xsl:otherwise>
        </xsl:choose>
    </xsl:template>

<!--###################################################
                      Extensions
    ################################################### -->

    <!-- These extensions are required for table printing and other stuff -->
    <xsl:param name="use.extensions">1</xsl:param>
    <xsl:param name="tablecolumns.extension">0</xsl:param>
    <xsl:param name="fop.extensions">1</xsl:param>

<!--###################################################
                   Paper & Page Size
    ################################################### -->

    <!-- Paper type, no headers on blank pages, no double sided printing -->
    <xsl:param name="double.sided">1</xsl:param>
    <xsl:param name="headers.on.blank.pages">0</xsl:param>
    <xsl:param name="footers.on.blank.pages">0</xsl:param>

<!--###################################################
                   Fonts & Styles
    ################################################### -->

    <xsl:param name="hyphenate">false</xsl:param>

    <!-- Line height in body text -->
    <xsl:param name="line-height">1.4</xsl:param>

<!--###################################################
                   Tables
    ################################################### -->

    <!-- Some padding inside tables -->
    <xsl:attribute-set name="table.cell.padding">
        <xsl:attribute name="padding-left">4pt</xsl:attribute>
        <xsl:attribute name="padding-right">4pt</xsl:attribute>
        <xsl:attribute name="padding-top">4pt</xsl:attribute>
        <xsl:attribute name="padding-bottom">4pt</xsl:attribute>
    </xsl:attribute-set>

    <!-- Only hairlines as frame and cell borders in tables -->
    <xsl:param name="table.frame.border.thickness">0.1pt</xsl:param>
    <xsl:param name="table.cell.border.thickness">0.1pt</xsl:param>

<!--###################################################
                         Labels
    ################################################### -->
 
    <xsl:attribute-set name="component.title.properties">
      <xsl:attribute name="border-top">
        <xsl:text>solid black 2pt</xsl:text>
      </xsl:attribute> 
    </xsl:attribute-set>
 
    <xsl:attribute-set name="section.title.level1.properties">
      <xsl:attribute name="border-top">
        <xsl:text>solid black 1pt</xsl:text>
      </xsl:attribute> 
    </xsl:attribute-set>

      <xsl:attribute-set name="section.title.level2.properties">
      <xsl:attribute name="border-top">
        <xsl:text>solid black .75pt</xsl:text>
      </xsl:attribute> 
    </xsl:attribute-set>

      <xsl:attribute-set name="section.title.level3.properties">
      <xsl:attribute name="border-top">
        <xsl:text>solid black .5pt</xsl:text>
      </xsl:attribute> 
    </xsl:attribute-set>

<!--###################################################
                      Programlistings
    ################################################### -->

    <xsl:attribute-set name="verbatim.properties">
        <xsl:attribute name="font-size">9pt</xsl:attribute>
        <xsl:attribute name="space-before.minimum">1em</xsl:attribute>
        <xsl:attribute name="space-before.optimum">1em</xsl:attribute>
        <xsl:attribute name="space-before.maximum">1em</xsl:attribute>
        <!-- alef: commented out because footnotes were screwed because of it -->
        <!--<xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
        <xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>-->

        <xsl:attribute name="border-color">#444444</xsl:attribute>
        <xsl:attribute name="border-style">solid</xsl:attribute>
        <xsl:attribute name="border-width">0.1pt</xsl:attribute>
        <xsl:attribute name="padding-top">0.5em</xsl:attribute>
        <xsl:attribute name="padding-left">0.5em</xsl:attribute>
        <xsl:attribute name="padding-right">0.5em</xsl:attribute>
        <xsl:attribute name="padding-bottom">0.5em</xsl:attribute>
        <xsl:attribute name="margin-left">0.5em</xsl:attribute>
        <xsl:attribute name="margin-right">0.5em</xsl:attribute>
    </xsl:attribute-set>

    <!-- Shade (background) programlistings -->
    <xsl:param name="shade.verbatim">1</xsl:param>
    <xsl:attribute-set name="shade.verbatim.style">
        <xsl:attribute name="background-color">#F0F0F0</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="list.block.spacing">
      <xsl:attribute name="space-before.optimum">0.1em</xsl:attribute>
      <xsl:attribute name="space-before.minimum">0.1em</xsl:attribute>
      <xsl:attribute name="space-before.maximum">0.1em</xsl:attribute>
      <xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
      <xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
      <xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
    </xsl:attribute-set>

    <xsl:attribute-set name="example.properties">
      <xsl:attribute name="space-before.minimum">0.5em</xsl:attribute>
      <xsl:attribute name="space-before.optimum">0.5em</xsl:attribute>
      <xsl:attribute name="space-before.maximum">0.5em</xsl:attribute>
      <xsl:attribute name="space-after.minimum">0.1em</xsl:attribute>
      <xsl:attribute name="space-after.optimum">0.1em</xsl:attribute>
      <xsl:attribute name="space-after.maximum">0.1em</xsl:attribute>
      <xsl:attribute name="keep-together.within-column">always</xsl:attribute>
    </xsl:attribute-set>

<!--###################################################
        Title information for Figures, Examples etc.
    ################################################### -->

    <xsl:attribute-set name="formal.title.properties" use-attribute-sets="normal.para.spacing">
      <xsl:attribute name="font-weight">normal</xsl:attribute>
      <xsl:attribute name="font-style">italic</xsl:attribute>
      <xsl:attribute name="font-size">
        <xsl:value-of select="$body.font.master"/>
        <xsl:text>pt</xsl:text>
      </xsl:attribute>
      <xsl:attribute name="hyphenate">false</xsl:attribute>
      <xsl:attribute name="space-before.minimum">0.1em</xsl:attribute>
      <xsl:attribute name="space-before.optimum">0.1em</xsl:attribute>
      <xsl:attribute name="space-before.maximum">0.1em</xsl:attribute>
    </xsl:attribute-set>

<!--###################################################
                          Misc
    ################################################### -->

  <!-- FOP 0.25 doens't support body.start.indent 
  <xsl:param name="body.start.indent">.75in</xsl:param>
   -->
  
  <!-- Remove "Chapter" from the Chapter titles... 
  <xsl:param name="local.l10n.xml" select="document('')"/>
  <l:i18n xmlns:l="http://docbook.sourceforge.net/xmlns/l10n/1.0">
    <l:l10n language="en">
      <l:context name="title-numbered">
        <l:template name="chapter" text="%n.&#160;%t"/>
        <l:template name="section" text="%n&#160;%t"/>
      </l:context>
      <l:context name="title">
        <l:template name="example" text="Example&#160;%n&#160;%t"/>
      </l:context>
    </l:l10n>
  </l:i18n>
--> 
  
<!-- create ability to switch to symbol fonts for FOP 0.20.5 -->
  <xsl:template match="symbol[@role = 'symbolfont']">
    <fo:inline font-family="Symbol">
      <xsl:call-template name="inline.charseq"/>
    </fo:inline>
  </xsl:template>

  <xsl:template match="emphasis[@role='bold-italic']">
   <fo:inline font-weight="bold" font-style="italic">
     <xsl:apply-templates/>
   </fo:inline>
 </xsl:template>
  
  <!-- bold-italic formatting -->
  
</xsl:stylesheet>