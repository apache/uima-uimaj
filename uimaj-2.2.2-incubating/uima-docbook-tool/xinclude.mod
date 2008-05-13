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
<!-- xml comments allowed in this file -->

<!-- ================================================================-->
<!--   This file is incorporated in xml files which have xi:include
       elements, to allow those files to validate in an XML editor
       without actually doing the includes                           -->

<!-- defines xi:include as a valid element for validating editors -->
<!-- adds it as an element to sub parts of books and chapters. -->
<!--   This must be referenced by each source file that has xi:include
       elements, in the file's DOCTYPE declaration,
       using the syntax
       lt;!ENTITY % xinclude SYSTEM "../../ . . . /uima-docbook-tool/xinclude.mod">
       %xinclude; -->

<!ELEMENT xi:include (xi:fallback?) >
<!ATTLIST xi:include
  xmlns:xi   CDATA        #FIXED      "http://www.w3.org/2001/XInclude"
  href       CDATA        #REQUIRED
  parse      (xml|text)   "xml"
  encoding   CDATA        #IMPLIED >

<!ELEMENT xi:fallback ANY>
<!ATTLIST xi:fallback
  xmlns:xi   CDATA        #FIXED      "http://www.w3.org/2001/XInclude" >

<!ENTITY % local.chapter.class "| xi:include">
<!ENTITY % local.book.class "| xi:include">
