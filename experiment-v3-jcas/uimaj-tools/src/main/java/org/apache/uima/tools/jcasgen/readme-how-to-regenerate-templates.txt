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

These instructions are for using Eclipse to regenerate the JCasTypeTemplate and JCas_TypeTemplate
java source files from the src/main/javajet/jcasgen templates.  You only need to do this if you
update the javajet templates.

0) (if not already done) Checkout the uimaj project-area from SVN, and import the projects
using import-existing-maven-projects.
1) create a new Eclipse "java" project: uimaj-jet-expander
2) highlight that project, and right-click and pick  import -> File System
3) navigate to where you've checked out from SVN the uimaj projects, and select the one
   named uimaj-jet-expander
4) click the uima-jet-expander checkbox in the left panel to select all of the content
5) **do not** check the checkbox name "create top level folder"

This should import the contents of this folder into your java project of the same name.

This project includes two launchers - you can go to the Eclipse run menu and type 
into the filter the word "jet" to find them.  One launcher creates the JCas_TypeTemplate.java, the other
the JCasTypeTemplate.java (no underscore).  Run them both, then compare the results to the 
previous values to check they did what you intended.
