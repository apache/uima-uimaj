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
import static org.apache.uima.UIMAFramework.newDefaultResourceManager;
import static org.apache.uima.util.CasCreationUtils.createCas;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;

import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.test.JCasClassLoaderTest.IsolatingClassloader;
import org.apache.uima.cas.test.JCasClassLoaderTest.JCasCreator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.junit.Assert;
import org.junit.Test;

public class FSCreatedInPearContextTest {

  @Test
  public void thatAnnotationIsCreatedOnce() throws Exception, IOException {

    ClassLoader rootCl = getClass().getClassLoader();

    IsolatingClassloader clForCas = new IsolatingClassloader("CAS", rootCl);

    ClassLoader clForToken = new IsolatingClassloader("Token", rootCl)
            .redefining("org\\.apache\\.uima\\.cas\\.test\\.Token(_Type)?.*");
    
    TypeSystemDescription tsd = loadTokensAndSentencesTS();
    
    JCas jcas = makeJCas(clForCas, tsd);
    jcas.setDocumentText("Test");

    CASImpl casImpl = jcas.getCasImpl();
    casImpl.switchClassLoader(clForToken, false);
    
    Annotation mockedPearToken = createToken(jcas, clForToken);
    
    Annotation indexedToken = jcas.getAnnotationIndex(Token.class).iterator().next();

    Assert.assertTrue("Token identical", mockedPearToken == indexedToken);
  }

  private Annotation createToken(JCas jcas, ClassLoader clForToken) throws Exception {
    Class<?> tokenClass = clForToken.loadClass(Token.class.getName());
    Constructor<?> constructor = tokenClass.getConstructor(JCas.class);
    Annotation token = (Annotation) constructor.newInstance(jcas);
    token.addToIndexes();
    return token;
  }

  private TypeSystemDescription loadTokensAndSentencesTS() throws InvalidXMLException, IOException {
    return getXMLParser().parseTypeSystemDescription(new XMLInputSource(
            new File("src/test/resources/CASTests/desc/TokensAndSentencesTS.xml")));
  }

  private JCas makeJCas(IsolatingClassloader cl, TypeSystemDescription tsd)
          throws Exception {
    cl.redefining("^.*JCasCreatorImpl$");
    Class<?> jcasCreatorClass = cl.loadClass(SimpleJCasCreatorImpl.class.getName());
    JCasCreator creator = (JCasCreator) jcasCreatorClass.newInstance();
    return creator.createJCas(cl, tsd);
  }
  
  public static class SimpleJCasCreatorImpl implements JCasCreator {

    @Override
    public JCas createJCas(ClassLoader cl, TypeSystemDescription tsd)
    {
      try {
        ResourceManager resMgr = newDefaultResourceManager();
        resMgr.setExtensionClassLoader(cl, false);
        CASImpl cas = (CASImpl) createCas(tsd, null, null, null, resMgr);
        cas.setJCasClassLoader(cl);
        return cas.getJCas();
      }
      catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
  }
  
}