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
import org.apache.uima.cas.text.AnnotationFS;
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
                    .withTitle("casWithTextAndAnnotations") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithTextAndAnnotations)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithEmojiUnicodeTextAndAnnotations") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithEmojiUnicodeTextAndAnnotations)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithLeftToRightTextAndAnnotations") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithLeftToRightTextAndAnnotations)
                    .withTargetCasSupplier(CasCreationUtils::createCas) //
                    .build(),
            CasSourceTargetConfiguration.builder() //
                    .withTitle("casWithTraditionalChineseTextAndAnnotations") //
                    .withSourceCasSupplier(
                            ProgrammaticallyCreatedCasDataSuite::casWithTraditionalChineseTextAndAnnotations)
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

  public static CAS casWithTextAndAnnotations() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    StringBuilder sb = new StringBuilder();
    createAnnotatedText(cas, sb, "This", " ");
    createAnnotatedText(cas, sb, "is", " ");
    createAnnotatedText(cas, sb, "a", " ");
    createAnnotatedText(cas, sb, "test");
    return cas;
  }

  public static CAS casWithEmojiUnicodeTextAndAnnotations() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    StringBuilder sb = new StringBuilder();
    createAnnotatedText(cas, sb, "🥳", " ");
    createAnnotatedText(cas, sb, "This", " ");
    createAnnotatedText(cas, sb, "👳🏻‍♀️", " ");
    createAnnotatedText(cas, sb, "is", " ");
    createAnnotatedText(cas, sb, "✆", " ");
    createAnnotatedText(cas, sb, "a", " ");
    createAnnotatedText(cas, sb, "🧔🏾‍♂️", " ");
    createAnnotatedText(cas, sb, "test", " ");
    createAnnotatedText(cas, sb, "👻");
    cas.setDocumentText(sb.toString());
    return cas;
  }

  public static CAS casWithLeftToRightTextAndAnnotations() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    StringBuilder sb = new StringBuilder();
    // "this is a test" per Google Translate
    createAnnotatedText(cas, sb, "هذا", " ");
    createAnnotatedText(cas, sb, "اختبار");
    cas.setDocumentText(sb.toString());
    return cas;
  }

  public static CAS casWithTraditionalChineseTextAndAnnotations() throws Exception {
    CAS cas = CasCreationUtils.createCas();
    StringBuilder sb = new StringBuilder();
    // "This is a test" per Google Translate
    createAnnotatedText(cas, sb, "這");
    createAnnotatedText(cas, sb, "是");
    createAnnotatedText(cas, sb, "一個");
    createAnnotatedText(cas, sb, "測試");
    cas.setDocumentText(sb.toString());
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

  private static void createAnnotatedText(CAS aCas, StringBuilder aBuffer, String aText,
          String... aSuffix) {
    int begin = aBuffer.length();
    aBuffer.append(aText);
    AnnotationFS a = aCas.createAnnotation(aCas.getAnnotationType(), begin, aBuffer.length());
    aCas.addFsToIndexes(a);
    for (String s : aSuffix) {
      aBuffer.append(s);
    }
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
