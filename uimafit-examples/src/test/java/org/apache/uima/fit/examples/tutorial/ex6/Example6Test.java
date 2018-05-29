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
package org.apache.uima.fit.examples.tutorial.ex6;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngineDescription;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createSharedResourceDescription;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.examples.tutorial.type.Meeting;
import org.apache.uima.fit.examples.tutorial.type.UimaAcronym;
import org.apache.uima.fit.examples.tutorial.type.UimaMeeting;
import org.apache.uima.fit.factory.AggregateBuilder;
import org.apache.uima.fit.util.JCasUtil;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ExternalResourceDescription;
import org.junit.Test;

public class Example6Test {

  @Test
  public void test1() throws Exception {
    // This resource is shared between the UimaAcronymAnnotator and UimaMeetingAnnotator
    ExternalResourceDescription resource = createSharedResourceDescription(
            "file:src/main/resources/org/apache/uima/fit/examples/tutorial/ex6/uimaAcronyms.txt",
            StringMapResource_impl.class);

    AggregateBuilder builder = new AggregateBuilder();
    builder.add(createEngineDescription(UimaAcronymAnnotator.class,
            UimaAcronymAnnotator.RES_ACRONYM_TABLE, resource));
    builder.add(createEngineDescription(UimaMeetingAnnotator.class,
            UimaMeetingAnnotator.RES_UIMA_TERM_TABLE, resource));
    AnalysisEngine engine = createEngine(builder.createAggregateDescription());

    JCas jCas = engine.newJCas();
    jCas.setDocumentText("Let's meet to talk about the CPE. The meeting is over at Yorktown 01-144");
    new Meeting(jCas, 0, 33).addToIndexes();

    engine.process(jCas);

    UimaAcronym uimaAcronym = JCasUtil.selectByIndex(jCas, UimaAcronym.class, 0);
    assertNotNull(uimaAcronym);
    assertEquals("CPE", uimaAcronym.getCoveredText());
    assertEquals("Collection Processing Engine", uimaAcronym.getExpandedForm());

    UimaMeeting uimaMeeting = JCasUtil.selectByIndex(jCas, UimaMeeting.class, 0);
    assertNotNull(uimaMeeting);
    assertEquals("Let's meet to talk about the CPE.", uimaMeeting.getCoveredText());
  }
}
