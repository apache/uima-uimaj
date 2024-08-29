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
package org.apache.uima.util;

import java.io.StringWriter;

import org.apache.uima.UIMAFramework;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.junit.jupiter.api.Test;

class TypeSystemUtilTest {
  @Test
  void testTypeSystem2TypeSystemDescription() throws Exception {
    // create a CAS with example type system
    var typeSystemFile = JUnitExtension.getFile("ExampleCas/testTypeSystem.xml");
    var tsDesc = UIMAFramework.getXMLParser()
            .parseTypeSystemDescription(new XMLInputSource(typeSystemFile));

    // add an example type to test FSArrays with and without elementTypes
    var type = tsDesc.addType("example.TestType", "", "uima.tcas.Annotation");
    type.addFeature("testFeat", "", "uima.cas.FSArray", "uima.tcas.Annotation", null);
    var type2 = tsDesc.addType("example.TestType2", "", "uima.tcas.Annotation");
    type2.addFeature("testFeat", "", "uima.cas.FSArray");

    var cas = CasCreationUtils.createCas(tsDesc, null, null);
    // convert that CAS's type system back to a TypeSystemDescription
    var tsDesc2 = TypeSystemUtil.typeSystem2TypeSystemDescription(cas.getTypeSystem());
    // test that this is valid by creating a new CAS
    CasCreationUtils.createCas(tsDesc2, null, null);

    // Check that can be written (without cluttering up the console)
    var out = new StringWriter();
    tsDesc2.toXML(out);
  }
}
