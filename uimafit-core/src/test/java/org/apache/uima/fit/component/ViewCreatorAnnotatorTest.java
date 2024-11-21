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
package org.apache.uima.fit.component;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASException;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.testing.util.HideOutput;
import org.apache.uima.fit.type.Token;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.jupiter.api.Test;

/**
 * I initially thought that the behavior of mapping the default view to another yet-to-be-created
 * view might be different for sofa aware and sofa unaware components. So the tests are run on using
 * an analysis engine of both kinds.
 * 
 * 
 */
public class ViewCreatorAnnotatorTest extends ComponentTestBase {

  @Test
  public void testViewCreatorAnnotator()
          throws ResourceInitializationException, AnalysisEngineProcessException, CASException {
    AnalysisEngine viewCreator = AnalysisEngineFactory.createEngine(ViewCreatorAnnotator.class,
            typeSystemDescription, ViewCreatorAnnotator.PARAM_VIEW_NAME, "myView");
    viewCreator.process(jCas);
    JCas myView = jCas.getView("myView");
    assertNotNull(myView);
    myView.setDocumentText("my view text");
  }

  /**
   * This test basically demonstrates that the default view does not need to be initialized because
   * it is done automatically.
   */
  @SuppressWarnings("javadoc")
  @Test
  public void testDefaultView() throws Exception {
    AnalysisEngine engine = AnalysisEngineFactory.createEngine(SofaAwareAnnotator.class,
            typeSystemDescription);
    engine.process(jCas);
    assertEquals("some", JCasUtil.selectByIndex(jCas, Token.class, 0).getCoveredText());

    engine = AnalysisEngineFactory.createEngine(SofaUnawareAnnotator.class, typeSystemDescription);
    jCas.reset();
    engine.process(jCas);
    assertEquals("some", JCasUtil.selectByIndex(jCas, Token.class, 0).getCoveredText());
  }

  /**
   * This test demonstrates the bad behavior that occurs when you try to map the default view to
   * some other view without initializing that other view first. This is the behavior that
   * SofaInitializerAnnotator addresses.
   */
  @SuppressWarnings("javadoc")
  @Test
  public void testOtherViewAware() throws Exception {
    AnalysisEngineDescription description = AnalysisEngineFactory
            .createEngineDescription(SofaAwareAnnotator.class, typeSystemDescription);
    AnalysisEngine engine = AnalysisEngineFactory.createEngine(description, "myView");

    Throwable thrown = catchThrowable(() -> {
      // Avoid exception being logged to the console
      HideOutput hider = null;
      try {
        hider = new HideOutput();
        engine.process(jCas);
      } finally {
        if (hider != null) {
          hider.restoreOutput();
        }
      }
    });

    assertThat(thrown).as("Exception thrown when view does not exist")
            .hasRootCauseInstanceOf(CASRuntimeException.class)
            .hasStackTraceContaining("No sofaFS with name myView found");
  }

  @Test
  public void testOtherViewUnaware()
          throws ResourceInitializationException, AnalysisEngineProcessException {
    AnalysisEngineDescription description = AnalysisEngineFactory
            .createEngineDescription(SofaUnawareAnnotator.class, typeSystemDescription);
    AnalysisEngine engine = AnalysisEngineFactory.createEngine(description, "myView");

    Throwable thrown = catchThrowable(() -> {
      // Avoid exception being logged to the console
      HideOutput hider = null;
      try {
        hider = new HideOutput();
        engine.process(jCas);
      } finally {
        if (hider != null) {
          hider.restoreOutput();
        }
      }
    });

    assertThat(thrown).as("Exception thrown when view does not exist")
            .hasRootCauseInstanceOf(CASRuntimeException.class)
            .hasStackTraceContaining("No sofaFS with name myView found");

  }

  /**
   * This test demonstrates that running the viewCreator is doing the right thing (i.e. initializing
   * the view "myView")
   */
  @SuppressWarnings("javadoc")
  @Test
  public void testSofaInitializer() throws Exception {
    AnalysisEngineDescription description = AnalysisEngineFactory
            .createEngineDescription(SofaAwareAnnotator.class, typeSystemDescription);
    AnalysisEngine engine = AnalysisEngineFactory.createEngine(description, "myView");
    AnalysisEngine viewCreator = AnalysisEngineFactory.createEngine(ViewCreatorAnnotator.class,
            typeSystemDescription, ViewCreatorAnnotator.PARAM_VIEW_NAME, "myView");
    viewCreator.process(jCas);
    engine.process(jCas);
    assertEquals("some",
            JCasUtil.selectByIndex(jCas.getView("myView"), Token.class, 0).getCoveredText());

    // here I run again with viewCreator running twice to make sure it
    // does the right thing when the view
    // has already been created
    jCas.reset();
    viewCreator.process(jCas);
    viewCreator.process(jCas);
    engine.process(jCas);
    assertEquals("some",
            JCasUtil.selectByIndex(jCas.getView("myView"), Token.class, 0).getCoveredText());

    description = AnalysisEngineFactory.createEngineDescription(SofaUnawareAnnotator.class,
            typeSystemDescription);
    engine = AnalysisEngineFactory.createEngine(description, "myView");
    jCas.reset();
    viewCreator.process(jCas);
    engine.process(jCas);
    assertEquals("some",
            JCasUtil.selectByIndex(jCas.getView("myView"), Token.class, 0).getCoveredText());

    jCas.reset();
    viewCreator.process(jCas);
    viewCreator.process(jCas);
    engine.process(jCas);
    assertEquals("some",
            JCasUtil.selectByIndex(jCas.getView("myView"), Token.class, 0).getCoveredText());
  }

  @SofaCapability(inputSofas = CAS.NAME_DEFAULT_SOFA)
  public static class SofaAwareAnnotator extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      JCas view;
      try {
        view = jCas.getView(CAS.NAME_DEFAULT_SOFA);
      } catch (CASException e) {
        throw new AnalysisEngineProcessException(e);
      }

      view.setDocumentText("some text");
      Token token = new Token(view, 0, 4);
      token.addToIndexes();
    }

  }

  public static class SofaUnawareAnnotator extends JCasAnnotator_ImplBase {

    @Override
    public void process(JCas jCas) throws AnalysisEngineProcessException {
      jCas.setDocumentText("some text");
      Token token = new Token(jCas, 0, 4);
      token.addToIndexes();
    }

  }

}
