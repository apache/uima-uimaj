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
package org.apache.uima.fit.pipeline;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.CollectionReaderFactory.createReaderDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createResourceDescription;
import static org.apache.uima.fit.pipeline.SimplePipeline.iteratePipeline;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.junit.Test;

public class JCasIterableTest {

  @Test
  public void test() throws Exception {
    for (JCas jcas : iteratePipeline(createReaderDescription(ThreeDocsReader.class),
            createEngineDescription(GetTextAE.class))) {
      System.out.println(jcas.getDocumentText());
    }
    
    assertEquals("Document 3", GetTextAE.lastText);
    assertTrue(GetTextAE.complete);
    assertTrue(GetTextAE.destroyed);
  }

  @Test
  public void testResourceSharing() throws Exception {
    ThreeDocsReader.resource = null;
    GetTextAE.resource = null;
    
    ExternalResourceDescription res = createResourceDescription(DummySharedResource.class);
    for (@SuppressWarnings("unused") JCas jcas : iteratePipeline(
            createReaderDescription(ThreeDocsReader.class, "resource", res),
            createEngineDescription(GetTextAE.class, "resource", res))) {
    }
    
    assertNotNull(ThreeDocsReader.resource);
    assertNotNull(GetTextAE.resource);
    assertTrue(ThreeDocsReader.resource == GetTextAE.resource);
    
    ThreeDocsReader.resource = null;
    GetTextAE.resource = null;
  }
 
  public static final class DummySharedResource extends Resource_ImplBase {
  }

  public static final class ThreeDocsReader extends JCasCollectionReader_ImplBase {
    @ExternalResource(mandatory=false)
    private static DummySharedResource resource;

    private final int N = 3;
    private int n = 0;

    private boolean initTypeSystemCalled = false;

    @Override
    public void typeSystemInit(TypeSystem aTypeSystem) throws ResourceInitializationException {
      initTypeSystemCalled = true;
    }
          
    @Override
    public Progress[] getProgress() {
      return new Progress[] { new ProgressImpl(n, N, "document") };
    }

    @Override
    public boolean hasNext() throws IOException, CollectionException {
      return n < N;
    }

    @Override
    public void getNext(JCas aJCas) throws IOException, CollectionException {
      assertTrue("typeSystemInit() has not been called", initTypeSystemCalled);
      n++;
      aJCas.setDocumentText("Document " + n);
    }
  }
  
  public static final class GetTextAE extends JCasAnnotator_ImplBase {
    @ExternalResource(mandatory=false)
    private static DummySharedResource resource;
    
    public static String lastText = null;
    
    public static boolean complete = false;

    public static boolean destroyed = false;

    @Override
    public void initialize(UimaContext aContext) throws ResourceInitializationException {
      super.initialize(aContext);
      lastText = null;
      complete = false;
      destroyed = false;
    }
    
    @Override
    public void process(JCas aArg0) throws AnalysisEngineProcessException {
      lastText = aArg0.getDocumentText();
    }
    
    @Override
    public void collectionProcessComplete() throws AnalysisEngineProcessException {
      complete = true;
    }
    
    @Override
    public void destroy() {
      destroyed = true;
    }
  }
}
