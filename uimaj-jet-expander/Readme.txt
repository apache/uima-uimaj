/*
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
 */

 This java project is a stand alone tool used to generate
 Java source code that is part of the JCasGen tool.

 It is not included as part of the binary distribution
 of UIMA.

 It is not part of the Maven build (at this time).
 
 To get it into your Eclipse workspace, use the SVN Repo view, and
 do an eclipse SVN checkout.

 It is set up with launchers for use within the Eclipse
 IDE.  There are 2 launchers - one creates Java code
 for uimaj-tools: jcasgen: JCasTypeTemplate.java, the other
 for uimaj-tools: jcasgen: JCas_TypeTemplate.java.

 When the tool is run, the output is generated back
 into the original uimaj-tools jcasgen source as
 JCasTypeTemplate.java and
 JCas_TypeTemplate.java.  After running, compare
 these to the existing versions to insure correctness,
 (use Eclipse's compare with local history).

 The Expander code handles the apache 2 licensing
 and inserts the license in the target.
