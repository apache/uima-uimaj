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
                version='1.0'>

  <xsl:import href="@file.prefix@@docbook.xsl@/html/chunk.xsl" />
  <xsl:import href="@file.prefix@@src.dir@/styles/html/custom.xsl" />
  <xsl:include href="@file.prefix@@tmp.dir@/html-titlepage.xsl" />

  <!-- enable olink, or not -->
  <xsl:param name="collect.xref.targets" select="'@olink_work@'"/>

  <!-- where the olink output goes -->  
  <xsl:param name="targets.filename">@src.dir@/olink/@olink_file@/@type@-target.db</xsl:param>
  
  <!-- process using olink info -->
  <xsl:param name="target.database.document">@file.prefix@@src.dir@/olink/olink_db_@type@.xml</xsl:param>
  <xsl:param name="current.docid">@olink_file@</xsl:param>
  
  <!-- where to find the css stylesheet -->
  <xsl:param name="html.stylesheet">css/stylesheet.css</xsl:param>

  <!-- Do not use the file.prefix and make sure that you keep the trailing slash! -->
  <xsl:param name="base.dir">@html.target.dir@/</xsl:param>
  <!--
  <xsl:param name="chunk.fast" select="1"/>
    -->
</xsl:stylesheet>

