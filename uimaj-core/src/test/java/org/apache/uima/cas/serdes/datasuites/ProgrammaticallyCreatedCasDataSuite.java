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

import java.nio.charset.StandardCharsets;
import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.cas.ByteArrayFS;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.serdes.transitions.CasSourceTargetConfiguration;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.util.CasCreationUtils;

public class ProgrammaticallyCreatedCasDataSuite
        extends AbstractCollection<CasSourceTargetConfiguration> implements CasDataSuite {

  private final List<CasSourceTargetConfiguration> confs;

  private ProgrammaticallyCreatedCasDataSuite(Builder builder) {
    confs = asList( //
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithText") //
                    .withSourceCasSupplier(ProgrammaticallyCreatedCasDataSuite::casWithText) //
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithTextAndAnnotation") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithTextAndAnnotation)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithSofaDataURI") //
                    .withSourceCasSupplier(ProgrammaticallyCreatedCasDataSuite::casWithSofaDataURI)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithSofaDataArray") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithSofaDataArray)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build());
  }

  @Override
  public Iterator<CasSourceTargetConfiguration> iterator() {
    return confs.iterator();
  }

  @Override
  public int size() {
    return confs.size();
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

  public static CAS casWithSofaDataURI() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    // NOTE: UIMA does not try to resolve the URI and also does not support "classpath:" URIs!
    cas.setSofaDataURI("classpath:/ProgrammaticallyCreatedCasDataSuite/document.txt", "text/plain");

    return cas;
  }

  public static CAS casWithSofaDataArray() throws Exception {
    CAS cas = CasCreationUtils.createCas();

    byte[] byteArray = "This is a test".getBytes(StandardCharsets.UTF_8);
    ByteArrayFS sofaDataArray = cas.createByteArrayFS(byteArray.length);
    for (int i = 0; i < byteArray.length; i++) {
      sofaDataArray.set(i, byteArray[i]);
    }

    cas.setSofaDataArray(sofaDataArray, "text/plain");

    return cas;
  }

  /**
   * Creates builder to build {@link ProgrammaticallyCreatedCasDataSuite}.
   * 
   * @return created builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Builder to build {@link ProgrammaticallyCreatedCasDataSuite}.
   */
  public static final class Builder {
    private Builder() {
    }

    public ProgrammaticallyCreatedCasDataSuite build() {
      return new ProgrammaticallyCreatedCasDataSuite(this);
    }
  }
}
