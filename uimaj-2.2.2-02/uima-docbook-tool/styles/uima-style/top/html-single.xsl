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

  <!-- This file is always imported by another xsl file in another project
       having the data.
    
       That other file (see styles/common/samples) imports also:
          the right docbook version / pdf or html or html single
          the titlepage generated xsl
     -->     
  
  <!-- relative urls are relative to this stylesheet location -->
  <!-- xsl:imports must be first in the file, and have lower precedence
       than following elements -->
  
  <xsl:import href="../common/html.xsl"/>
  <xsl:import href="../common/html-pdf.xsl"/>
      
  <!-- where to find the css stylesheet -->
  <xsl:param name="html.stylesheet">css/stylesheet-html.css</xsl:param>

</xsl:stylesheet>

