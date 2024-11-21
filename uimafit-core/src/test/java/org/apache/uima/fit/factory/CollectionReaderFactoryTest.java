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
package org.apache.uima.fit.factory;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createReader;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderFromPath;
import static org.apache.uima.fit.factory.TypeSystemDescriptionFactory.createTypeSystemDescription;
import static org.apache.uima.resource.metadata.FsIndexDescription.KIND_SORTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.descriptor.ResourceMetaData;
import org.apache.uima.fit.factory.spi.FsIndexCollectionProviderForTesting;
import org.apache.uima.fit.factory.spi.TypePrioritiesProviderForTesting;
import org.apache.uima.fit.factory.testCrs.SingleFileXReader;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.type.AnalyzedText;
import org.apache.uima.fit.type.Sentence;
import org.apache.uima.fit.type.Token;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.Progress;
import org.junit.jupiter.api.Test;

public class CollectionReaderFactoryTest extends ComponentTestBase {

  @Test
  public void testCreateCollectionReader() throws UIMAException, IOException {

    CollectionReader reader = CollectionReaderFactory.createReader(SingleFileXReader.class,
            typeSystemDescription, SingleFileXReader.PARAM_FILE_NAME,
            "src/test/resources/data/docs/test.xmi", SingleFileXReader.PARAM_XML_SCHEME,
            SingleFileXReader.XMI);

    JCasIterator jCasIterator = new JCasIterator(reader, typeSystemDescription);
    jCas = jCasIterator.next();
    assertNotNull(jCas);
    assertEquals("Me and all my friends are non-conformists.", jCas.getDocumentText());
    Token token = JCasUtil.selectByIndex(jCas, Token.class, 2);
    assertEquals("all", token.getCoveredText());
    assertEquals("A", token.getPos());
    assertEquals("all", token.getStem());

    reader = createReader("org.apache.uima.fit.factory.testCrs.SingleFileXReader",
            SingleFileXReader.PARAM_FILE_NAME, "src/test/resources/data/docs/test.xmi",
            SingleFileXReader.PARAM_XML_SCHEME, SingleFileXReader.XMI);

    jCasIterator = new JCasIterator(reader, typeSystemDescription);
    jCas = jCasIterator.next();
    assertNotNull(jCas);
    assertEquals("Me and all my friends are non-conformists.", jCas.getDocumentText());
    token = JCasUtil.selectByIndex(jCas, Token.class, 9);
    assertEquals(".", token.getCoveredText());
    assertEquals(".", token.getPos());
    assertEquals(".", token.getStem());

    reader = createReaderFromPath(
            "src/test/resources/org/apache/uima/fit/factory/testCrs/SingleFileXReader.xml",
            SingleFileXReader.PARAM_FILE_NAME, "src/test/resources/data/docs/test.xmi",
            SingleFileXReader.PARAM_XML_SCHEME, SingleFileXReader.XMI);

    jCasIterator = new JCasIterator(reader, typeSystemDescription);
    jCas = jCasIterator.next();
    assertNotNull(jCas);
    assertEquals("Me and all my friends are non-conformists.", jCas.getDocumentText());
    token = JCasUtil.selectByIndex(jCas, Token.class, 4);
    assertEquals("friends", token.getCoveredText());
    assertEquals("F", token.getPos());
    assertEquals("friend", token.getStem());
  }

  @Test
  public void testExceptions() {
    ResourceInitializationException rie = null;
    try {
      CollectionReaderFactory.createReader(TestCR.class, (Object[]) null);
    } catch (ResourceInitializationException e) {
      rie = e;
    }
    assertNotNull(rie);
  }

  @Test
  public void thatCreateReaderDescriptorAutoDetectionWorks() throws Exception {
    CollectionReaderDescription crd = createReaderDescription(TestCR.class);

    TypeSystemDescription tsd = createTypeSystemDescription();
    assertThat(tsd.getType(Token.class.getName())) //
            .as("Token type auto-detection") //
            .isNotNull();
    assertThat(tsd.getType(Sentence.class.getName())) //
            .as("Sentence type auto-detection") //
            .isNotNull();
    assertThat(tsd.getType(AnalyzedText.class.getName())) //
            .as("AnalyzedText type auto-detection") //
            .isNotNull();

    assertThat(crd.getCollectionReaderMetaData().getTypePriorities().getPriorityLists()) //
            .as("Type priorities auto-detection")//
            .extracting(TypePriorityList::getTypes) //
            .containsExactlyInAnyOrder( //
                    new String[] { //
                        TypePrioritiesProviderForTesting.TEST_TYPE_A },
                    new String[] { //
                        Sentence.class.getName(), //
                        Token.class.getName() });

    assertThat(crd.getCollectionReaderMetaData().getFsIndexCollection().getFsIndexes()) //
            .extracting( //
                    FsIndexDescription::getLabel, //
                    FsIndexDescription::getTypeName, //
                    FsIndexDescription::getKind) //
            .containsExactlyInAnyOrder( //
                    tuple( //
                            "Automatically Scanned Index", //
                            Token.class.getName(), //
                            KIND_SORTED),
                    tuple( //
                            FsIndexCollectionProviderForTesting.INDEX_LABEL, //
                            FsIndexCollectionProviderForTesting.INDEX_TYPE, //
                            KIND_SORTED));
  }

  @Test
  public void testResourceMetaData() throws Exception {
    CollectionReaderDescription desc = CollectionReaderFactory
            .createReaderDescription(TestCR.class);

    org.apache.uima.resource.metadata.ResourceMetaData meta = desc.getMetaData();

    assertEquals("dummy", meta.getName());
    assertEquals("1.0", meta.getVersion());
    assertEquals("Just a dummy", meta.getDescription());
    assertEquals("ASL 2.0", meta.getCopyright());
    assertEquals("uimaFIT", meta.getVendor());
  }

  @ResourceMetaData(name = "dummy", version = "1.0", description = "Just a dummy", copyright = "ASL 2.0", vendor = "uimaFIT")
  private class TestCR extends CollectionReader_ImplBase {

    private TestCR() {
      // do not instantiate
    }

    @Override
    public void getNext(CAS acas) throws IOException, CollectionException {
      // Not required for test
    }

    @Override
    public void close() throws IOException {
      // Not required for test
    }

    @Override
    public Progress[] getProgress() {
      // Not required for test
      return null;
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
      // Not required for test
      return false;
    }
  }
}
