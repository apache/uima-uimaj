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
import static org.apache.uima.fit.internal.ResourceManagerFactory.getResourceManagerCreator;
import static org.apache.uima.fit.internal.ResourceManagerFactory.newResourceManager;
import static org.apache.uima.fit.internal.ResourceManagerFactory.setResourceManagerCreator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Iterator;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.component.Resource_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.fit.internal.ResourceManagerFactory;
import org.apache.uima.fit.internal.ResourceManagerFactory.ResourceManagerCreator;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class JCasIterableTest {

  private ResourceManagerCreator originalResourceManagerCreator;

  @BeforeEach
  public void setup() {
    // We need to resort to static fields here because there is no way to use Mockito's spy on the
    // component instances internally created by UIMA.
    ThreeDocsReader.destroyed = false;
    GetTextAE.complete = false;
    GetTextAE.destroyed = false;
    GetTextAE.resource = null;
    ThreeDocsReader.resource = null;

    originalResourceManagerCreator = getResourceManagerCreator();
  }

  @AfterEach
  public void teardown() {
    setResourceManagerCreator(originalResourceManagerCreator);
  }

  @Test
  public void thatComponentsGetDestroyed() throws Exception {
    consume(new JCasIterable(createReaderDescription(ThreeDocsReader.class),
            createEngineDescription(GetTextAE.class)));

    assertThat(GetTextAE.complete).isTrue();
    assertThat(GetTextAE.destroyed).isTrue();
    assertThat(ThreeDocsReader.destroyed).isTrue();
    assertThat(GetTextAE.lastText).isEqualTo("Document 3");
  }

  @Test
  public void thatResourceCanBeShared() throws Exception {
    ExternalResourceDescription res = createResourceDescription(DummySharedResource.class);

    consume(new JCasIterable(createReaderDescription(ThreeDocsReader.class, "resource", res),
            createEngineDescription(GetTextAE.class, "resource", res)));

    assertThat(ThreeDocsReader.resource).isNotNull().isEqualTo(GetTextAE.resource);
  }

  @Test
  public void thatSharedResourceManagerIsNotDestroyed() throws Exception {
    ResourceManager resMgr = spy(newResourceManager());

    consume(new JCasIterable(resMgr, createReaderDescription(ThreeDocsReader.class),
            createEngineDescription(GetTextAE.class)));

    verify(resMgr, never()).destroy();
  }

  /**
   * Mind that returning a singleton resource manager from {@link ResourceManagerFactory} is
   * generally a bad idea exactly because it gets destroyed on a regular basis. For this reason, it
   * is called {@link ResourceManagerFactory#newResourceManager()} and not
   * {@code getResourceManager()}.
   */
  @Test
  public void thatInternallyCreatedResourceManagerIsDestroyed() throws Exception {
    ResourceManager resMgr = spy(newResourceManager());
    setResourceManagerCreator(() -> resMgr);

    consume(new JCasIterable(createReaderDescription(ThreeDocsReader.class),
            createEngineDescription(GetTextAE.class)));

    verify(resMgr, times(1)).destroy();
  }

  private static void consume(Iterable<?> aIterable) {
    Iterator<?> i = aIterable.iterator();
    while (i.hasNext()) {
      i.next();
    }
  }

  public static final class DummySharedResource extends Resource_ImplBase {
  }

  public static final class ThreeDocsReader extends JCasCollectionReader_ImplBase {
    @ExternalResource(mandatory = false)
    private static DummySharedResource resource;

    private final int N = 3;

    private int n = 0;

    public boolean initTypeSystemCalled = false;

    public static boolean destroyed = false;

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
      assertThat(initTypeSystemCalled) //
              .as("typeSystemInit() has been called") //
              .isTrue();
      n++;
      aJCas.setDocumentText("Document " + n);
    }

    @Override
    public void destroy() {
      super.destroy();
      destroyed = true;
    }
  }

  public static final class GetTextAE extends JCasAnnotator_ImplBase {
    @ExternalResource(mandatory = false)
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
