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
package org.apache.uima.cas.test;

import static org.apache.uima.UIMAFramework.getXMLParser;
import static org.apache.uima.util.CasCreationUtils.createCas;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.internal.util.UIMAClassLoader;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.test.IsolatingClassloader;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.jupiter.api.Test;

public class FSCreatedInPearContextTest {

  @Test
  public void thatOneTrampolineIsUsedWhenClassLoaderIsSwitched() throws Exception, IOException {

    var rootCl = getClass().getClassLoader();

    var clForToken = new IsolatingClassloader("Token", rootCl)
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    var casImpl = (CASImpl) createCas(loadTokensAndSentencesTS(), null, null, null);
    casImpl.switchClassLoaderLockCasCL(new UIMAClassLoader(new URL[0], clForToken));
    casImpl.setDocumentText("Test");

    var tokenType = casImpl.getTypeSystem().getType(Token.class.getName());
    var token = casImpl.createAnnotation(tokenType, 0, 1);
    token.addToIndexes();
    assertThat(token.getClass().getClassLoader())
            .as("Trampoline returned by createAnnotation after classloader switch")
            .isSameAs(clForToken);

    assertThat(casImpl.select(Token.type).asList()) //
            .as("Same trampoline returned by [select(Token.type)] after classloader switch")
            .usingElementComparator((a, b) -> a == b ? 0 : 1) //
            .containsExactly(token) //
            .allMatch(t -> t.getClass().getClassLoader() == clForToken);

    casImpl.restoreClassLoaderUnlockCas();
    assertThat(casImpl.select(Token.type).asList()) //
            .as("After switching back out of the the classloader context, we get the base FS")
            .usingElementComparator((a, b) -> a._id() == b._id() ? 0 : 1) //
            .containsExactly(token) //
            .allMatch(t -> t.getClass().getClassLoader() == rootCl);
  }

  @Test
  public void thatResettingCasInPearContextWorks() throws Exception, IOException {
    var rootCl = getClass().getClassLoader();

    var clForToken = new IsolatingClassloader("Token", rootCl)
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");

    var casImpl = (CASImpl) createCas(loadTokensAndSentencesTS(), null, null, null);
    casImpl.switchClassLoaderLockCasCL(new UIMAClassLoader(new URL[0], clForToken));
    casImpl.setDocumentText("Test");

    // The normal "reset" is blocked in PEAR mode, but e.g. the XmiCasDeserializerHandler calls
    // resetNoQuestions()
    casImpl.resetNoQuestions();

    assertThatNoException().isThrownBy(() -> {
      var tokenType = casImpl.getTypeSystem().getType(Token.class.getName());
      var token = casImpl.createAnnotation(tokenType, 0, 1);
      token.addToIndexes();
    });
  }

  private TypeSystemDescription loadTokensAndSentencesTS() throws InvalidXMLException, IOException {
    return getXMLParser().parseTypeSystemDescription(new XMLInputSource(
            new File("src/test/resources/CASTests/desc/TokensAndSentencesTS.xml")));
  }
}
