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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.collection.CollectionReader_ImplBase;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.factory.testCrs.SingleFileXReader;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.type.Token;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.junit.Test;

/**
 */

public class CollectionReaderFactoryTest extends ComponentTestBase {

  @Test
  public void testCreateCollectionReader() throws UIMAException, IOException {

    CollectionReader reader = CollectionReaderFactory.createCollectionReader(
            SingleFileXReader.class, typeSystemDescription, SingleFileXReader.PARAM_FILE_NAME,
            "src/test/resources/data/docs/test.xmi", SingleFileXReader.PARAM_XML_SCHEME,
            SingleFileXReader.XMI);

    JCasIterable jCasIterable = new JCasIterable(reader, typeSystemDescription);
    jCas = jCasIterable.next();
    assertNotNull(jCas);
    assertEquals("Me and all my friends are non-conformists.", jCas.getDocumentText());
    Token token = JCasUtil.selectByIndex(jCas, Token.class, 2);
    assertEquals("all", token.getCoveredText());
    assertEquals("A", token.getPos());
    assertEquals("all", token.getStem());

    reader = CollectionReaderFactory.createCollectionReader(
            "org.apache.uima.fit.factory.testCrs.SingleFileXReader",
            SingleFileXReader.PARAM_FILE_NAME, "src/test/resources/data/docs/test.xmi",
            SingleFileXReader.PARAM_XML_SCHEME, SingleFileXReader.XMI);

    jCasIterable = new JCasIterable(reader, typeSystemDescription);
    jCas = jCasIterable.next();
    assertNotNull(jCas);
    assertEquals("Me and all my friends are non-conformists.", jCas.getDocumentText());
    token = JCasUtil.selectByIndex(jCas, Token.class, 9);
    assertEquals(".", token.getCoveredText());
    assertEquals(".", token.getPos());
    assertEquals(".", token.getStem());

    reader = CollectionReaderFactory.createCollectionReaderFromPath(
            "src/test/resources/org/apache/uima/fit/factory/testCrs/SingleFileXReader.xml",
            SingleFileXReader.PARAM_FILE_NAME, "src/test/resources/data/docs/test.xmi",
            SingleFileXReader.PARAM_XML_SCHEME, SingleFileXReader.XMI);

    jCasIterable = new JCasIterable(reader, typeSystemDescription);
    jCas = jCasIterable.next();
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
      CollectionReaderFactory.createCollectionReader(TestCR.class, (Object[]) null);
    } catch (ResourceInitializationException e) {
      rie = e;
    }
    assertNotNull(rie);
  }

  private class TestCR extends CollectionReader_ImplBase {

    private TestCR() {
      // do not instantiate
    }

    public void getNext(CAS acas) throws IOException, CollectionException {
      // TODO Auto-generated method stub
    }

    public void close() throws IOException {
      // TODO Auto-generated method stub
    }

    public Progress[] getProgress() {
      // TODO Auto-generated method stub
      return null;
    }

    public boolean hasNext() throws IOException, CollectionException {
      // TODO Auto-generated method stub
      return false;
    }
  }
}
