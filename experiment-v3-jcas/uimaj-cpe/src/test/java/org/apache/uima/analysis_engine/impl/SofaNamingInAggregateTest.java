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

package org.apache.uima.analysis_engine.impl;

import java.util.HashMap;

import org.junit.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.annotator.AnnotatorContext;
import org.apache.uima.analysis_engine.asb.impl.ASB_impl;
import org.apache.uima.analysis_engine.asb.impl.FlowControllerContainer;
import org.apache.uima.analysis_engine.impl.AggregateAnalysisEngine_impl;
import org.apache.uima.analysis_engine.impl.AnnotatorContext_impl;
import org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl;
import org.apache.uima.analysis_engine.metadata.impl.SofaMapping_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.SofaID;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;

public class SofaNamingInAggregateTest extends TestCase {
  HashMap additionalParams;

  AggregateAnalysisEngine_impl aggregateAE;

  PrimitiveAnalysisEngine_impl delegateAE;

  private AnalysisEngineDescription aeDescriptor;

  private FlowControllerContainer flowController;

  AggregateAnalysisEngine_impl aggregateAE2;

  PrimitiveAnalysisEngine_impl delegateAE2;

  /*
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception {
    try {
      super.setUp();
      UIMAFramework.getXMLParser().enableSchemaValidation(true);
      // create aggregate analysis engine with sofa name mappings
      XMLInputSource in1 = new XMLInputSource(JUnitExtension
              .getFile("CpeSofaTest/TransAnnotatorAggregate.xml"));
      // parse XML descriptor
      aeDescriptor = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in1);
      additionalParams = new HashMap();
      // instantiate AE
      // aggregateAE =
      // UIMAFramework.produceAnalysisEngine(desc1,additionalParams);
      aggregateAE = new AggregateAnalysisEngine_impl();
      aggregateAE.initialize(aeDescriptor, additionalParams);

      // get the delegate AE and the Flow Controller
      delegateAE = (PrimitiveAnalysisEngine_impl) aggregateAE._getASB()
              .getComponentAnalysisEngines().get("Translator1");
      flowController = ((ASB_impl) aggregateAE._getASB()).getFlowControllerContainer();

      // also try an aggregate that contains a sofa mapping for a
      // sofa-unaware component
      XMLInputSource in2 = new XMLInputSource(JUnitExtension
              .getFile("CpeSofaTest/TCasTransAnnotatorAggregate.xml"));
      AnalysisEngineDescription aeDescriptor2 = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(in2);
      aggregateAE2 = (AggregateAnalysisEngine_impl) UIMAFramework
              .produceAnalysisEngine(aeDescriptor2);
      delegateAE2 = (PrimitiveAnalysisEngine_impl) aggregateAE2._getASB()
              .getComponentAnalysisEngines().get("Translator1");

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /**
   * Do full validation of descriptor; this checks validity of Sofa Mappings.
   */
  public void testFullValidation() throws Exception {
    try {
      aeDescriptor.doFullValidation();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /**
   * Checks if sofa name mappings set in an aggregate AE descriptor get passed to UimaContext of the
   * delegate AE.
   * 
   */
  public void testGetSofaMappings() throws Exception {
    try {
      SofaID[] sofamappings = delegateAE.getUimaContext().getSofaMappings();
      Assert.assertEquals(2, sofamappings.length);
      Assert.assertEquals("EnglishDocument", sofamappings[0].getComponentSofaName());
      Assert.assertEquals("GermanDocument", sofamappings[1].getComponentSofaName());

      sofamappings = flowController.getUimaContext().getSofaMappings();
      Assert.assertEquals(1, sofamappings.length);
      Assert.assertEquals("OriginalDocument", sofamappings[0].getComponentSofaName());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /**
   * Test the mapToSofaID method in UimaContext.
   * 
   */
  public void testGetUimaContextMapToSofaID() throws Exception {
    try {
      SofaID sofaid1 = delegateAE.getUimaContext().mapToSofaID("EnglishDocument");
      Assert.assertEquals("SourceDocument", sofaid1.getSofaID());
      SofaID sofaid2 = delegateAE.getUimaContext().mapToSofaID("GermanDocument");
      Assert.assertEquals("OutputTranslator1", sofaid2.getSofaID());

      SofaID sofaid3 = flowController.getUimaContext().mapToSofaID("OriginalDocument");
      Assert.assertEquals("SourceDocument", sofaid3.getSofaID());

      // now try the second aggregate (With the sofa-unaware mapping)
      sofaid1 = delegateAE2.getUimaContext().mapToSofaID(CAS.NAME_DEFAULT_TEXT_SOFA);
      Assert.assertEquals("SourceDocument", sofaid1.getSofaID());
      sofaid2 = delegateAE2.getUimaContext().mapToSofaID("GermanDocument");
      Assert.assertEquals("OutputTranslator1", sofaid2.getSofaID());

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /**
   * Test the mapToSofaID method in UimaContext.
   * 
   */
  public void testMapRootSofaNameToSofaID() throws Exception {
    try {
      SofaID sofaid1 = delegateAE.getUimaContext().mapToSofaID("EnglishDocument.1.txt");
      Assert.assertEquals("SourceDocument.1.txt", sofaid1.getSofaID());
      SofaID sofaid2 = delegateAE.getUimaContext().mapToSofaID("SomeID.1");
      Assert.assertEquals("SomeID.1", sofaid2.getSofaID());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /**
   * Test the mapToSofaID method in Annotator Context.
   * 
   */
  public void testGetAnnotatorContextMapToSofaID() throws Exception {
    try {
      AnnotatorContext context = new AnnotatorContext_impl(delegateAE.getUimaContextAdmin());
      SofaID sofaid1 = context.mapToSofaID("EnglishDocument");
      Assert.assertEquals("SourceDocument", sofaid1.getSofaID());
      SofaID sofaid2 = context.mapToSofaID("GermanDocument");
      Assert.assertEquals("OutputTranslator1", sofaid2.getSofaID());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /**
   * Test the whether input sofa specified in the AE descriptar are in the AE meta data.
   * 
   */
  public void testGetInputSofas() throws Exception {
    try {
      Capability[] capabilities = aggregateAE.getAnalysisEngineMetaData().getCapabilities();
      String[] inputSofas = capabilities[0].getInputSofas();
      Assert.assertEquals(1, inputSofas.length);
      Assert.assertEquals("SourceDocument", inputSofas[0]);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /**
   * Test whether the output sofa specified in the AE descriptor are in the AE meta data.
   * 
   */
  public void testGetOutputSofas() throws Exception {
    try {
      Capability[] capabilities = aggregateAE.getAnalysisEngineMetaData().getCapabilities();
      String[] outputSofas = capabilities[0].getOutputSofas();
      Assert.assertEquals(2, outputSofas.length);
      Assert.assertEquals("OutputTranslator1", outputSofas[0]);
      Assert.assertEquals("OutputTranslator2", outputSofas[1]);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  /**
   * Tests programmatically specifying the sofa name mapping in a aggregate AE.
   * 
   */
  public void testSetSofaNameMappingInAggregateDescriptor() throws Exception {
    try {
      // create aggregate analysis engine with sofa name mappings
      XMLInputSource in1 = new XMLInputSource(JUnitExtension
              .getFile("CpeSofaTest/TransAnnotatorAggregateWithoutSofaNameMapping.xml"));
      // parse XML descriptor
      AnalysisEngineDescription desc1 = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(in1);

      // provide sofa name mappings for sofas in each component AE
      SofaMapping_impl[] sofamappings = new SofaMapping_impl[4];

      sofamappings[0] = new SofaMapping_impl();
      sofamappings[0].setComponentKey("Translator1");
      sofamappings[0].setComponentSofaName("EnglishDocument");
      sofamappings[0].setAggregateSofaName("SourceDocument");

      sofamappings[1] = new SofaMapping_impl();
      sofamappings[1].setComponentKey("Translator1");
      sofamappings[1].setComponentSofaName("GermanDocument");
      sofamappings[1].setAggregateSofaName("OutputTranslator1");

      sofamappings[2] = new SofaMapping_impl();
      sofamappings[2].setComponentKey("Translator2");
      sofamappings[2].setComponentSofaName("EnglishDocument");
      sofamappings[2].setAggregateSofaName("SourceDocument");

      sofamappings[3] = new SofaMapping_impl();
      sofamappings[3].setComponentKey("Translator2");
      sofamappings[3].setComponentSofaName("GermanDocument");
      sofamappings[3].setAggregateSofaName("OutputTranslator2");

      desc1.setSofaMappings(sofamappings);

      // instantiate AE
      AggregateAnalysisEngine_impl aggregateAE = new AggregateAnalysisEngine_impl();
      aggregateAE.initialize(desc1, additionalParams);

      // get the first delegate AE
      AnalysisEngine delegateAE1 = (PrimitiveAnalysisEngine_impl) aggregateAE._getASB()
              .getComponentAnalysisEngines().get("Translator1");
      Assert.assertEquals(2, delegateAE1.getUimaContext().getSofaMappings().length);
      Assert.assertEquals("SourceDocument", delegateAE1.getUimaContext().mapToSofaID(
              "EnglishDocument").getSofaID());

      // get the second delegate AE
      AnalysisEngine delegateAE2 = (PrimitiveAnalysisEngine_impl) aggregateAE._getASB()
              .getComponentAnalysisEngines().get("Translator2");
      Assert.assertEquals(2, delegateAE2.getUimaContext().getSofaMappings().length);
      Assert.assertEquals("SourceDocument", delegateAE2.getUimaContext().mapToSofaID(
              "EnglishDocument").getSofaID());

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

}
