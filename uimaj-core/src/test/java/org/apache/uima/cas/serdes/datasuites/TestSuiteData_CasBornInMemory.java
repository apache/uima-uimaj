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
package org.apache.uima.cas.serdes.datasuites;

import static java.util.Arrays.asList;

import java.util.List;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.serdes.transitions.CasSourceTargetConfiguration;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.CasCreationUtils;

public class TestSuiteData_CasBornInMemory {

  public static List<CasSourceTargetConfiguration> configurations() {
    return asList( //
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithText") //
                    .withSourceCasSupplier(TestSuiteData_CasBornInMemory::casWithText) //
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithTextAndAnnotation") //
                    .withSourceCasSupplier(TestSuiteData_CasBornInMemory::casWithTextAndAnnotation)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build());
  }

  public static CAS emptyCas() throws Exception {
    return CasCreationUtils.createCas();
  }

  public static CAS casWithText() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    cas.setDocumentText("This is a test.");
    return cas;
  }

  public static CAS casWithTextAndAnnotation() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    cas.setDocumentText("This is a test.");

    Annotation a = new Annotation(cas.getJCas(), 0, cas.getDocumentText().length());
    a.addToIndexes();

    return cas;
  }
}
