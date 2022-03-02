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
package org.apache.uima.fit.factory.testCrs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.JCasIterator;
import org.apache.uima.fit.testing.util.HideOutput;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.junit.jupiter.api.Test;

/**
 */

public class SingleFileXReaderTest extends ComponentTestBase {

  @Test
  public void testXReader() throws UIMAException, IOException {
    ResourceInitializationException rie = null;
    try {
      CollectionReaderFactory.createReader(SingleFileXReader.class, null,
              SingleFileXReader.PARAM_XML_SCHEME, "XML");
    } catch (ResourceInitializationException e) {
      rie = e;
    }
    assertThat(rie).isNotNull();

    rie = null;
    try {
      CollectionReaderFactory.createReader(SingleFileXReader.class, null,
              SingleFileXReader.PARAM_XML_SCHEME, "XML", SingleFileXReader.PARAM_FILE_NAME,
              "myxslt.xml");
    } catch (ResourceInitializationException e) {
      rie = e;
    }
    assertThat(rie).isNotNull();

    CollectionReader cr = CollectionReaderFactory.createReader(SingleFileXReader.class,
            typeSystemDescription, SingleFileXReader.PARAM_XML_SCHEME, "XCAS",
            SingleFileXReader.PARAM_FILE_NAME, "src/test/resources/data/docs/test.xcas");
    Progress[] progress = cr.getProgress();
    assertThat(progress).hasSize(1);
    assertThat(progress[0].getCompleted()).isEqualTo(0);
    assertThat(cr.hasNext()).isTrue();

    new JCasIterator(cr).next();
    progress = cr.getProgress();
    assertThat(progress).hasSize(1);
    assertThat(progress[0].getCompleted()).isEqualTo(1);

    cr.close();

    cr = CollectionReaderFactory.createReader(SingleFileXReader.class, typeSystemDescription,
            SingleFileXReader.PARAM_XML_SCHEME, "XCAS", SingleFileXReader.PARAM_FILE_NAME,
            "test/data/docs/test.xcas");
    UnsupportedOperationException uoe = null;
    try {
      new JCasIterator(cr).remove();
    } catch (UnsupportedOperationException e) {
      uoe = e;
    }
    assertThat(uoe).isNotNull();
    cr.close();

    HideOutput hideOutput = new HideOutput();
    cr = CollectionReaderFactory.createReader(SingleFileXReader.class, typeSystemDescription,
            SingleFileXReader.PARAM_XML_SCHEME, "XCAS", SingleFileXReader.PARAM_FILE_NAME,
            "test/data/docs/bad.xcas");
    RuntimeException re = null;
    try {
      new JCasIterator(cr).next();
    } catch (RuntimeException e) {
      re = e;
    }
    assertThat(re).isNotNull();
    hideOutput.restoreOutput();

    cr = CollectionReaderFactory.createReader(SingleFileXReader.class, typeSystemDescription,
            SingleFileXReader.PARAM_XML_SCHEME, "XMI", SingleFileXReader.PARAM_FILE_NAME,
            "test/data/docs/dne.xmi");
    re = null;
    try {
      JCasIterator jCases = new JCasIterator(cr);
      assertThat(jCases.hasNext()).isTrue();
      jCases.next();
    } catch (RuntimeException e) {
      re = e;
    }
    assertThat(re).isNotNull();
  }
}
