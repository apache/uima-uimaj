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

package org.apache.uima.fit.spring;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.bindResourceOnceWithoutNested;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.jcas.JCas;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigUtils;
import org.springframework.context.support.GenericApplicationContext;

/**
 * Test making Spring beans available to a UIMA component via the resource manager.
 * 
 */
public class SpringContextResourceManagerTest {
  // Allow UIMA components to be configured with beans from a Spring context

  // Allow the construction of UIMA components as Spring beans. This means:
  // - UIMA AnalysisComponent

  @Test
  public void test() throws Exception {
    // Acquire application context
    ApplicationContext ctx = getApplicationContext();

    // Create resource manager
    SpringContextResourceManager resMgr = new SpringContextResourceManager();
    resMgr.setApplicationContext(ctx);

    // Create component description
    AnalysisEngineDescription desc = createEngineDescription(MyAnalysisEngine.class);
    bindResourceOnceWithoutNested(desc, "injectedBean", "springBean");

    // Instantiate component
    AnalysisEngine ae = UIMAFramework.produceAnalysisEngine(desc, resMgr, null);

    // Test that injection works
    ae.process(ae.newJCas());
  }

  public static class MyAnalysisEngine extends JCasAnnotator_ImplBase {
    @ExternalResource(key = "injectedBean")
    private Object injectedBean;

    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      assertEquals("BEAN", injectedBean);
    }
  }

  private ApplicationContext getApplicationContext() {
    final GenericApplicationContext ctx = new GenericApplicationContext();
    AnnotationConfigUtils.registerAnnotationConfigProcessors(ctx);
    ctx.registerBeanDefinition("springBean",
            BeanDefinitionBuilder.genericBeanDefinition(String.class).addConstructorArgValue("BEAN")
                    .getBeanDefinition());
    ctx.refresh();
    return ctx;
  }
}
