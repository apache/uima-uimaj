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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UIMARuntimeException;
import org.apache.uima.UIMA_IllegalStateException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.CasIterator;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TaeDescription;
import org.apache.uima.analysis_engine.TextAnalysisEngine;
import org.apache.uima.analysis_engine.asb.impl.ASB_impl;
import org.apache.uima.analysis_engine.asb.impl.FlowControllerContainer;
import org.apache.uima.analysis_engine.impl.AggregateAnalysisEngine_impl;
import org.apache.uima.analysis_engine.impl.PrimitiveAnalysisEngine_impl;
import org.apache.uima.analysis_engine.impl.ResultSpecification_impl;
import org.apache.uima.analysis_engine.impl.TaeDescription_impl;
import org.apache.uima.analysis_engine.metadata.FixedFlow;
import org.apache.uima.analysis_engine.metadata.impl.FixedFlow_impl;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.CASRuntimeException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIndexRepository;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.FSIndexComparator;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.impl.URISpecifier_impl;
import org.apache.uima.resource.metadata.AllowedValue;
import org.apache.uima.resource.metadata.ConfigurationParameter;
import org.apache.uima.resource.metadata.FeatureDescription;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.apache.uima.resource.metadata.NameValuePair;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.AllowedValue_impl;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.FeatureDescription_impl;
import org.apache.uima.resource.metadata.impl.FsIndexDescription_impl;
import org.apache.uima.resource.metadata.impl.FsIndexKeyDescription_impl;
import org.apache.uima.resource.metadata.impl.NameValuePair_impl;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.impl.ProcessTrace_impl;

/**
 * Tests the TextAnalysisEngine_impl class.
 * 
 * @author Adam Lally 
 */
public class AnalysisEngine_implTest extends TestCase
{
  /**
   * Constructor for TextAnalysisEngine_implTest.
   * @param arg0
   */
  public AnalysisEngine_implTest(String arg0)
    throws java.io.FileNotFoundException
  {
    super(arg0);
  }

  /**
   * @see TestCase#setUp()
   */
  protected void setUp() throws Exception
  {
    super.setUp();
  }

  public void testInitialize() throws Exception
  {
    try
    {
      PrimitiveAnalysisEngine_impl tae1 = new PrimitiveAnalysisEngine_impl();
      
      //try to initialize with the wrong kind of specifier - should return false
      boolean result = tae1.initialize(new URISpecifier_impl(), null);
      Assert.assertFalse(result);
      
      //try to initialize with an empty TaeDescription - should throw exception
      Exception ex = null;
      try
      {      
        TaeDescription taeDesc = new TaeDescription_impl();
        taeDesc.setPrimitive(true);
        tae1.initialize(taeDesc, null);
      }
      catch(ResourceInitializationException e)
      {
        ex = e;
      }  
      Assert.assertNotNull(ex);
        
      //initialize simple primitive TextAnalysisEngine
      tae1 = new PrimitiveAnalysisEngine_impl();
      TaeDescription primitiveDesc = new TaeDescription_impl();
      primitiveDesc.setFrameworkImplementation("org.apache.uima.java");
      primitiveDesc.setPrimitive(true);
      primitiveDesc.setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      result = tae1.initialize(primitiveDesc, null);
      Assert.assertTrue(result);    
      
      //initialize again - should fail    
      ex = null;
      try
      {      
        tae1.initialize(primitiveDesc, null);
      }
      catch(UIMA_IllegalStateException e)
      {
        ex = e;
      }
      Assert.assertNotNull(ex);
      
      //initialize simple aggregate TextAnalysisEngine (also pass TextAnalysisEngineProcessData as parameter)
      TaeDescription aggDesc = new TaeDescription_impl();
      aggDesc.setFrameworkImplementation("org.apache.uima.java");
      aggDesc.setPrimitive(false);
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow(new String[]{"Test"});
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      AggregateAnalysisEngine_impl tae2 = new AggregateAnalysisEngine_impl();
      result = tae2.initialize(aggDesc,null);
      Assert.assertTrue(result);
      
      //try some descriptors that are invalid due to config. param problems
      for (int i = 1; i <= 13; i++)
      {
        _testInvalidDescriptor(JUnitExtension.getFile("TextAnalysisEngineImplTest/InvalidConfigParams" + i + ".xml"));
      }  
      
      //try a descriptor with configuration parameter overrides - should work
      XMLInputSource in = new XMLInputSource(
          JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateTaeWithConfigParamOverrides.xml"));
      
      TaeDescription desc =
        UIMAFramework.getXMLParser().parseTaeDescription(in);
      AggregateAnalysisEngine_impl tae = new AggregateAnalysisEngine_impl();
      tae.initialize(desc, Collections.EMPTY_MAP);
      
      PrimitiveAnalysisEngine_impl delegate1 = (PrimitiveAnalysisEngine_impl)
			    tae._getASB().getComponentAnalysisEngines().get("Annotator1");
	  PrimitiveAnalysisEngine_impl delegate2 = (PrimitiveAnalysisEngine_impl)
					tae._getASB().getComponentAnalysisEngines().get("Annotator2");
    FlowControllerContainer flowController = ((ASB_impl)tae._getASB()).getFlowControllerContainer();
      String strVal1 = (String)
        delegate1.getUimaContext().getConfigParameterValue("en","StringParam");
      Assert.assertEquals("override",strVal1);
			String strVal2 = (String)
				delegate2.getUimaContext().getConfigParameterValue("en","StringParam");
			Assert.assertEquals("en",strVal2);
      String strVal3 = (String)
        flowController.getUimaContext().getConfigParameterValue("en","StringParam");
      Assert.assertEquals("en",strVal3);
      
    Assert.assertEquals("en",strVal2);
			Integer intVal1 = (Integer)
				delegate1.getUimaContext().getConfigParameterValue("en","IntegerParam");
			Assert.assertEquals(100, intVal1.intValue());
			Integer intVal2 = (Integer)
				delegate1.getUimaContext().getConfigParameterValue("en","IntegerParam");
			Assert.assertEquals(100, intVal2.intValue());
      Integer intVal3 = (Integer)
        flowController.getUimaContext().getConfigParameterValue("en","IntegerParam");
      Assert.assertEquals(100, intVal3.intValue());

      String[] strArrVal1 = (String[])
        delegate1.getUimaContext().getConfigParameterValue("en","StringArrayParam");
      Assert.assertEquals(Arrays.asList(new String[]{"override"}),Arrays.asList(strArrVal1));
			String[] strArrVal2 = (String[])
				delegate2.getUimaContext().getConfigParameterValue("en","StringArrayParam");
			Assert.assertEquals(Arrays.asList(new String[]{"override"}),Arrays.asList(strArrVal2));
      String[] strArrVal3 = (String[])
        flowController.getUimaContext().getConfigParameterValue("en","StringArrayParam");
      Assert.assertEquals(Arrays.asList(new String[]{"override"}),Arrays.asList(strArrVal3));

			//anotherdescriptor with configuration parameter overrides (this time no groups)
			in = new XMLInputSource(
					JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateTaeWithConfigParamOverrides2.xml"));
      
			desc =
				UIMAFramework.getXMLParser().parseTaeDescription(in);
			tae = new AggregateAnalysisEngine_impl();
			tae.initialize(desc, Collections.EMPTY_MAP);
      
			delegate1 = (PrimitiveAnalysisEngine_impl)
					tae._getASB().getComponentAnalysisEngines().get("Annotator1");
			delegate2 = (PrimitiveAnalysisEngine_impl)
					tae._getASB().getComponentAnalysisEngines().get("Annotator2");
      flowController = ((ASB_impl)tae._getASB()).getFlowControllerContainer();

      strVal1 = (String)
				delegate1.getUimaContext().getConfigParameterValue("StringParam");
			Assert.assertEquals("override",strVal1);
			strVal2 = (String)
			  delegate2.getUimaContext().getConfigParameterValue("StringParam");
			Assert.assertEquals("myString",strVal2);
      strVal3 = (String)
      flowController.getUimaContext().getConfigParameterValue("StringParam");
      Assert.assertEquals("myString",strVal3);

			intVal1 = (Integer)
				delegate1.getUimaContext().getConfigParameterValue("IntegerParam");
			Assert.assertEquals(100, intVal1.intValue());
			intVal2 = (Integer)
			  delegate2.getUimaContext().getConfigParameterValue("IntegerParam");
			Assert.assertEquals(100, intVal2.intValue());
      intVal3 = (Integer)
        flowController.getUimaContext().getConfigParameterValue("IntegerParam");
      Assert.assertEquals(100, intVal3.intValue());

			strArrVal1 = (String[])
				delegate1.getUimaContext().getConfigParameterValue("StringArrayParam");
			Assert.assertEquals(Arrays.asList(new String[]{"override"}),Arrays.asList(strArrVal1));
			strArrVal2 = (String[])
				delegate2.getUimaContext().getConfigParameterValue("StringArrayParam");
			Assert.assertEquals(Arrays.asList(new String[]{"override"}),Arrays.asList(strArrVal2));
      strArrVal3 = (String[])
      flowController.getUimaContext().getConfigParameterValue("StringArrayParam");
        Assert.assertEquals(Arrays.asList(new String[]{"override"}),Arrays.asList(strArrVal3));
            
      //try a descriptor that's invalid due to an unsatisfied resource dependency
      _testInvalidDescriptor(JUnitExtension.getFile("TextAnalysisEngineImplTest/UnsatisfiedResourceDependency.xml"));
      
      //test an aggregate TAE containing a CAS Consumer
			in = new XMLInputSource(
					JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateTaeWithCasConsumer.xml"));      
			desc =
				UIMAFramework.getXMLParser().parseTaeDescription(in);
			tae = new AggregateAnalysisEngine_impl();
			tae.initialize(desc, Collections.EMPTY_MAP);
			delegate1 = (PrimitiveAnalysisEngine_impl)
			  tae._getASB().getComponentAnalysisEngines().get("Annotator");
            delegate2 = (PrimitiveAnalysisEngine_impl)
              tae._getASB().getComponentAnalysisEngines().get("CasConsumer");
            assertTrue(delegate1.getAnalysisEngineMetaData().getOperationalProperties().getModifiesCas());
            assertFalse(delegate2.getAnalysisEngineMetaData().getOperationalProperties().getModifiesCas());

      //try an aggregate with no components (tests that empty flow works)
			in = new XMLInputSource(
					JUnitExtension.getFile("TextAnalysisEngineImplTest/EmptyAggregate.xml"));      
			desc =
				UIMAFramework.getXMLParser().parseTaeDescription(in);
			FixedFlow emptyFlow = (FixedFlow)desc.getAnalysisEngineMetaData().getFlowConstraints();
			assertNotNull(emptyFlow.getFixedFlow());
			assertTrue(emptyFlow.getFixedFlow().length == 0);
			tae = new AggregateAnalysisEngine_impl();
			tae.initialize(desc, Collections.EMPTY_MAP);  

			//aggregate with duplicate group overrides
			in = new XMLInputSource(
					JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateWithDuplicateGroupOverrides.xml"));      
			desc =
				UIMAFramework.getXMLParser().parseTaeDescription(in);
      tae = new AggregateAnalysisEngine_impl();
      tae.initialize(desc, Collections.EMPTY_MAP);
      
      delegate1 = (PrimitiveAnalysisEngine_impl)
			    tae._getASB().getComponentAnalysisEngines().get("Annotator1");
			delegate2 = (PrimitiveAnalysisEngine_impl)
					tae._getASB().getComponentAnalysisEngines().get("Annotator2");
      String commonParamA = (String)
        delegate1.getUimaContext().getConfigParameterValue("a","CommonParam");
      Assert.assertEquals("AggregateParam1a",commonParamA);
			String ann1_groupBParamBC = (String)
				delegate1.getUimaContext().getConfigParameterValue("b","BCParam");
			Assert.assertEquals("AggregateParam2b",ann1_groupBParamBC);
			String ann2_groupBParamBC = (String)
			  delegate2.getUimaContext().getConfigParameterValue("b","BCParam");
		  Assert.assertEquals("AggregateParam3b",ann2_groupBParamBC);

    }
    catch (Exception e)
    {
			JUnitExtension.handleException(e);
    }
  }      

  protected void _testInvalidDescriptor(File aFile)
    throws IOException
  {
    XMLInputSource in = new XMLInputSource(aFile);
    Exception ex = null;
    try
    {
      TaeDescription desc =
        UIMAFramework.getXMLParser().parseTaeDescription(in);
      UIMAFramework.produceAnalysisEngine(desc);
    }
    catch(InvalidXMLException e)
    {
//      e.printStackTrace();
      ex = e;
    }
    catch(ResourceInitializationException e)
    {
//      e.printStackTrace();
      ex = e;
    }
    Assert.assertNotNull(ex);  
    Assert.assertNotNull(ex.getMessage());   
    Assert.assertFalse(ex.getMessage().startsWith("EXCEPTION MESSAGE LOCALIZATION FAILED"));
  }
  
  public void testProcess() throws Exception
  {
    try
    {
      //test simple primitive TextAnalysisEngine (using TestAnnotator class)
      TaeDescription primitiveDesc = new TaeDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc.setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      primitiveDesc.getMetaData().setName("Test Primitive TAE");
      _testProcess(primitiveDesc);
           
      //test simple aggregate TextAnalysisEngine (again using TestAnnotator class)
      TaeDescription aggDesc = new TaeDescription_impl();
      aggDesc.setPrimitive(false);
      aggDesc.getMetaData().setName("Test Aggregate TAE");
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow(new String[]{"Test"});
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      _testProcess(aggDesc);
            
      //test aggregate TAE containing a CAS Consumer
      TaeDescription aggWithCcDesc = UIMAFramework.getXMLParser().
        parseTaeDescription(new XMLInputSource(
            JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateTaeWithCasConsumer.xml")));
      long startTime = System.currentTimeMillis();
      _testProcess(aggWithCcDesc);
      //test that CAS Consumer ran
      File outFile = JUnitExtension.getFile("CpmOutput.txt");
      assertTrue(outFile.lastModified() >= startTime);
    }
    catch (Exception e)
    {
			JUnitExtension.handleException(e);
    }
  }
  
  /**
   * Auxilliary method used by testProcess()
   * 
   * @param aTaeDesc description of TextAnalysisEngine to test
   */
  protected void _testProcess(TaeDescription aTaeDesc)
    throws UIMAException
  {    
    //create and initialize TextAnalysisEngine
    TextAnalysisEngine tae = UIMAFramework.produceTAE(aTaeDesc);
    
    //Test each form of the process method.  When TestAnnotator executes, it
    //stores in static fields the document text and the ResultSpecification. 
    //We use thse to make sure the information propogates correctly to the annotator.
      
    //process(CAS)
    TCAS tcas = tae.newTCAS();
    tcas.setDocumentText("new test");
    tae.process(tcas);
    assertEquals("new test", TestAnnotator.lastDocument);
    tcas.reset();
    
    //process(CAS,ResultSpecification)    
    ResultSpecification resultSpec = new ResultSpecification_impl();
    resultSpec.addResultType("FakeType",true);

    tcas.setDocumentText("testing...");
    tae.process(tcas, resultSpec);
    assertEquals("testing...", TestAnnotator.lastDocument);
    //TODO: resultSpec not supported assertEquals(resultSpec, TestAnnotator.lastResultSpec);
    tcas.reset();
  }
  
  public void testReconfigure() throws Exception
  {
    try
    {
      //create simple primitive TextAnalysisEngine descriptor (using TestAnnotator class)
      TaeDescription primitiveDesc = new TaeDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc.getMetaData().setName("Test Primitive TAE");    
      primitiveDesc.setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      ConfigurationParameter p1 = new ConfigurationParameter_impl();
      p1.setName("StringParam");
      p1.setDescription("parameter with String data type");
      p1.setType(ConfigurationParameter.TYPE_STRING);
      primitiveDesc.getMetaData().getConfigurationParameterDeclarations().
        setConfigurationParameters(new ConfigurationParameter[]{p1});
      primitiveDesc.getMetaData().getConfigurationParameterSettings().
        setParameterSettings(
          new NameValuePair[]{new NameValuePair_impl("StringParam","Test1")});
        
      //instantiate TextAnalysisEngine  
      PrimitiveAnalysisEngine_impl tae = new PrimitiveAnalysisEngine_impl();
      tae.initialize(primitiveDesc, null);
      
      //check value of string param (TestAnnotator saves it in a static field)
      assertEquals("Test1", TestAnnotator.stringParamValue);
           
      //reconfigure
      tae.setConfigParameterValue("StringParam","Test2");
      tae.reconfigure();

      //test again
      assertEquals("Test2", TestAnnotator.stringParamValue);
          
      //test aggregate TAE
      TaeDescription aggDesc = new TaeDescription_impl();
      aggDesc.setFrameworkImplementation("org.apache.uima.java");
      aggDesc.setPrimitive(false);
      aggDesc.getMetaData().setName("Test Aggregate TAE");
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow(new String[]{"Test"});
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      ConfigurationParameter p2 = new ConfigurationParameter_impl();
      p2.setName("StringParam");
      p2.setDescription("parameter with String data type");
      p2.setType(ConfigurationParameter.TYPE_STRING);
      aggDesc.getMetaData().getConfigurationParameterDeclarations().
        setConfigurationParameters(new ConfigurationParameter[]{p2});
      aggDesc.getMetaData().getConfigurationParameterSettings().
        setParameterSettings(
          new NameValuePair[]{new NameValuePair_impl("StringParam","Test3")});      
      //instantiate TextAnalysisEngine  
      AggregateAnalysisEngine_impl aggTae = new AggregateAnalysisEngine_impl();
      aggTae.initialize(aggDesc, null);

      assertEquals("Test3",TestAnnotator.stringParamValue);
           
      //reconfigure
      aggTae.setConfigParameterValue("StringParam","Test4");
      aggTae.reconfigure();

      //test again
      assertEquals("Test4",TestAnnotator.stringParamValue);
            
      //reconfigure WITHOUT setting that parameter
      aggTae.reconfigure();   
      //test again
      assertEquals("Test4",TestAnnotator.stringParamValue);
            
      //test aggregate TAE that does NOT override parameter
      primitiveDesc.getMetaData().getConfigurationParameterSettings().
        setParameterSettings(
          new NameValuePair[]{new NameValuePair_impl("StringParam","Test1")});
      TaeDescription aggDesc2 = new TaeDescription_impl();
      aggDesc2.setFrameworkImplementation("org.apache.uima.java");
      aggDesc2.setPrimitive(false);
      aggDesc2.getMetaData().setName("Test Aggregate TAE");
      aggDesc2.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow2 = new FixedFlow_impl();
      flow2.setFixedFlow(new String[]{"Test"});
      aggDesc2.getAnalysisEngineMetaData().setFlowConstraints(flow2);
      ConfigurationParameter p3 = new ConfigurationParameter_impl();
      p3.setName("IntParam");
      p3.setDescription("parameter with Integer data type");
      p3.setType(ConfigurationParameter.TYPE_INTEGER);
      aggDesc2.getMetaData().getConfigurationParameterDeclarations().
        setConfigurationParameters(new ConfigurationParameter[]{p3});
      aggDesc2.getMetaData().getConfigurationParameterSettings().
        setParameterSettings(
          new NameValuePair[]{new NameValuePair_impl("IntParam",new Integer(42))});      
      //instantiate TextAnalysisEngine  
      AggregateAnalysisEngine_impl aggTae2 = new AggregateAnalysisEngine_impl();
      aggTae2.initialize(aggDesc2, null);
      
      //call process - this should generate an event with a resource name equal
      //to the value of StringParam
      assertEquals("Test1",TestAnnotator.stringParamValue);
      //reconfigure
      aggTae2.setConfigParameterValue("IntParam",new Integer(0));
      aggTae2.reconfigure();
      //test again - should not have changed
      assertEquals("Test1",TestAnnotator.stringParamValue);
    }
    catch (Exception e)
    {
			JUnitExtension.handleException(e);
    }
  }
  
  
  public void testCreateAnalysisProcessData() throws Exception
  {
    try
    {
      //create simple primitive TAE with type system and indexes
      TaeDescription desc = new TaeDescription_impl();
      desc.setPrimitive(true);
      desc.getMetaData().setName("Test Primitive TAE");
      desc.setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      
      TypeSystemDescription typeSystem = new TypeSystemDescription_impl();
      TypeDescription type1 = 
        typeSystem.addType("Type1","Test Type One", TCAS.TYPE_NAME_ANNOTATION);
      FeatureDescription feat1 = new FeatureDescription_impl();
      feat1.setName("Feature1");
      feat1.setRangeTypeName(CAS.TYPE_NAME_INTEGER);
      type1.setFeatures(new FeatureDescription[]{feat1});
      TypeDescription type2 = 
        typeSystem.addType("Type2","Test Type Two",TCAS.TYPE_NAME_ANNOTATION);
      FeatureDescription feat2 = new FeatureDescription_impl();
      feat2.setName("Feature2");
      feat2.setRangeTypeName("EnumType");
      type2.setFeatures(new FeatureDescription[]{feat2});
      TypeDescription enumType = 
        typeSystem.addType("EnumType","Test Enumerated Type","uima.cas.String");
      enumType.setAllowedValues(new AllowedValue[]
        {
          new AllowedValue_impl("One","First Value"),
          new AllowedValue_impl("Two","Second Value")
        });
      desc.getAnalysisEngineMetaData().setTypeSystem(typeSystem);
      
      TypePriorities typePriorities = new TypePriorities_impl();
      TypePriorityList priorityList = typePriorities.addPriorityList();
      priorityList.addType("Type1");
      priorityList.addType("Type2");
      desc.getAnalysisEngineMetaData().setTypePriorities(typePriorities);
      
      FsIndexDescription index1 = new FsIndexDescription_impl();
      index1.setLabel("Index1");
      index1.setTypeName("Type1");
      FsIndexKeyDescription key1 = new FsIndexKeyDescription_impl();
      key1.setFeatureName("Feature1");
      key1.setComparator(FSIndexComparator.STANDARD_COMPARE);
      index1.setKeys(new FsIndexKeyDescription[]{key1});
      FsIndexDescription index2 = new FsIndexDescription_impl();
      index2.setLabel("Index2");
      index2.setTypeName("Type2");
      index2.setKind(FsIndexDescription.KIND_SET);
      FsIndexKeyDescription key2 = new FsIndexKeyDescription_impl();
      key2.setFeatureName("Feature2");
      key2.setComparator(FSIndexComparator.REVERSE_STANDARD_COMPARE);
      index2.setKeys(new FsIndexKeyDescription[]{key2});   
      FsIndexDescription index3 = new FsIndexDescription_impl();
      index3.setLabel("Index3");
      index3.setTypeName("uima.tcas.Annotation");
      index3.setKind(FsIndexDescription.KIND_SORTED);
      FsIndexKeyDescription key3 = new FsIndexKeyDescription_impl();
      key3.setFeatureName("begin");
      key3.setComparator(FSIndexComparator.STANDARD_COMPARE);
      FsIndexKeyDescription key4 = new FsIndexKeyDescription_impl();
      key4.setTypePriority(true);
      index3.setKeys(new FsIndexKeyDescription[]{key3,key4});    
      desc.getAnalysisEngineMetaData().setFsIndexes(
        new FsIndexDescription[]{index1,index2,index3});
            
      //instantiate TextAnalysisEngine  
      PrimitiveAnalysisEngine_impl tae = new PrimitiveAnalysisEngine_impl();
      tae.initialize(desc, null); //this calls createAnalysisProcessData
      
      //check results in CAS
      //type system
      TCAS cas = tae.newTCAS();
      TypeSystem ts = cas.getTypeSystem();
      Type t1 = ts.getType("Type1");
      Assert.assertEquals("Type1",t1.getName());
      Feature f1 = t1.getFeatureByBaseName("Feature1");
      Feature f1a = ts.getFeatureByFullName("Type1:Feature1");
      Assert.assertEquals(f1, f1a);
      Assert.assertEquals("Feature1", f1.getShortName());
      Assert.assertEquals(t1, f1.getDomain());
      
      Type t2 = ts.getType("Type2");
      Assert.assertEquals("Type2",t2.getName());
      Feature f2 = t2.getFeatureByBaseName("Feature2");
      Feature f2a = ts.getFeatureByFullName("Type2:Feature2");
      Assert.assertEquals(f2, f2a);
      Assert.assertEquals("Feature2", f2.getShortName());
      Assert.assertEquals(t2, f2.getDomain());
      
      Type et = ts.getType("EnumType");
      Assert.assertEquals("EnumType",et.getName());    
      Assert.assertEquals(et,f2.getRange());
      
      //indexes
      FSIndexRepository irep = cas.getIndexRepository();
      FSIndex ind = irep.getIndex("Index1");
      Assert.assertNotNull(ind);
      Assert.assertEquals("Type1", ind.getType().getName());
      Assert.assertEquals(FSIndex.SORTED_INDEX, ind.getIndexingStrategy());
      
      FeatureStructure fs1 = cas.createFS(t1);
      fs1.setIntValue(f1,0);
      FeatureStructure fs2 = cas.createFS(t1);
      fs2.setIntValue(f1,1);
      Assert.assertTrue(ind.compare(fs1,fs2) < 0);
      
      FSIndex ind2 = irep.getIndex("Index2");
      Assert.assertNotNull(ind2);
      Assert.assertEquals("Type2", ind2.getType().getName());
      Assert.assertEquals(FSIndex.SET_INDEX, ind2.getIndexingStrategy());
      
      FeatureStructure fs3 = cas.createFS(t2);
      fs3.setStringValue(f2,"One");
      FeatureStructure fs4 = cas.createFS(t2);
      fs4.setStringValue(f2,"Two");
      Assert.assertTrue(ind2.compare(fs3,fs4) > 0);
      
      FSIndex ind3 = irep.getIndex("Index3");
      Assert.assertNotNull(ind3);
      Assert.assertEquals("uima.tcas.Annotation", ind3.getType().getName());
      Assert.assertEquals(FSIndex.SORTED_INDEX, ind3.getIndexingStrategy());
      
      AnnotationFS fs5 = cas.createAnnotation(t1,0,0);
      AnnotationFS fs6 = cas.createAnnotation(t2,0,0);
      AnnotationFS fs7 = cas.createAnnotation(t1,0,0);
      Assert.assertTrue(ind3.compare(fs5,fs6) < 0);
      Assert.assertTrue(ind3.compare(fs6,fs7) > 0);
      
      //only way to check if allowed values is correct is to try to set an 
      //invalid value?
      CASRuntimeException ex = null;
      try
      {
        fs4.setStringValue(f2,"Three");
      }
      catch(CASRuntimeException e)
      {
        ex = e;  
      }
      Assert.assertNotNull(ex);
    }
    catch (Exception e)
    {
			JUnitExtension.handleException(e);
    }
  }
  
  
  public void testProcessDelegateAnalysisEngineMetaData()
    throws Exception
  {
    try
    {
      //create aggregate analysis engine whose delegates each declare 
      //type system, type priorities, and indexes
      XMLInputSource in = new XMLInputSource(
          JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml"));
      AnalysisEngineDescription desc =
        UIMAFramework.getXMLParser().parseTaeDescription(in);
      AggregateAnalysisEngine_impl tae = new AggregateAnalysisEngine_impl();
      tae.initialize(desc, Collections.EMPTY_MAP);
      //initialize method automatically calls processDelegateAnalysisEngineMetaData()
      
      //test results of merge
      //TypeSystem
      TypeSystemDescription typeSys =
          tae.getAnalysisEngineMetaData().getTypeSystem();
      Assert.assertEquals(8, typeSys.getTypes().length);  
      
      TypeDescription type0 = typeSys.getType("NamedEntity");
      Assert.assertNotNull(type0);
      Assert.assertEquals("uima.tcas.Annotation",type0.getSupertypeName());
      Assert.assertEquals(1,type0.getFeatures().length);    
      
      TypeDescription type1 = typeSys.getType("Person");
      Assert.assertNotNull(type1);
      Assert.assertEquals("NamedEntity",type1.getSupertypeName());
      Assert.assertEquals(1,type1.getFeatures().length);
      
      TypeDescription type2 = typeSys.getType("Place");
      Assert.assertNotNull(type2);
      Assert.assertEquals("NamedEntity",type2.getSupertypeName());
      Assert.assertEquals(3,type2.getFeatures().length);
      
      TypeDescription type3 = typeSys.getType("Org");
      Assert.assertNotNull(type3);
      Assert.assertEquals("uima.tcas.Annotation",type3.getSupertypeName());
      Assert.assertEquals(0,type3.getFeatures().length);
      
      TypeDescription type4 = typeSys.getType("DocumentStructure");
      Assert.assertNotNull(type4);
      Assert.assertEquals("uima.tcas.Annotation",type4.getSupertypeName());
      Assert.assertEquals(0,type4.getFeatures().length);
      
      TypeDescription type5 = typeSys.getType("Paragraph");
      Assert.assertNotNull(type5);
      Assert.assertEquals("DocumentStructure",type5.getSupertypeName());
      Assert.assertEquals(0,type5.getFeatures().length);
      
      TypeDescription type6 = typeSys.getType("Sentence");
      Assert.assertNotNull(type6);
      Assert.assertEquals("DocumentStructure",type6.getSupertypeName());
      Assert.assertEquals(0,type6.getFeatures().length);

      TypeDescription type7 = typeSys.getType("test.flowController.Test");
      Assert.assertNotNull(type7);
      Assert.assertEquals("uima.tcas.Annotation",type7.getSupertypeName());
      Assert.assertEquals(1,type7.getFeatures().length);
      
      //TypePriorities
      TypePriorities pri = tae.getAnalysisEngineMetaData().getTypePriorities();
      Assert.assertNotNull(pri);
      TypePriorityList[] priLists = pri.getPriorityLists();
      Assert.assertEquals(3,priLists.length);
      String[] list0 = priLists[0].getTypes();
      String[] list1 = priLists[1].getTypes();
      String[] list2 = priLists[2].getTypes();
      //order of the three lists is not defined
      Assert.assertTrue(
        (list0.length == 2 && list1.length == 2 && list2.length == 3) ||
        (list0.length == 2 && list1.length == 3 && list2.length == 2) ||
        (list0.length == 3 && list1.length == 2 && list2.length == 2));
        
      //Indexes
      FsIndexDescription[] indexes = tae.getAnalysisEngineMetaData().getFsIndexes();
      Assert.assertEquals(3, indexes.length);  
      //order of indexes is not defined
      String label0 = indexes[0].getLabel();
      String label1 = indexes[1].getLabel();
      String label2 = indexes[2].getLabel();
      Assert.assertTrue(label0.equals("DocStructIndex") || label1.equals("DocStructIndex") || label2.equals("DocStructIndex"));
      Assert.assertTrue(label0.equals("PlaceIndex") || label1.equals("PlaceIndex") || label2.equals("PlaceIndex"));
      Assert.assertTrue(label0.equals("FlowControllerTestIndex") || label1.equals("FlowControllerTestIndex") || label2.equals("FlowControllerTestIndex"));
      
      //test that we can create a CAS
      CAS cas = tae.newCAS();
      TypeSystem ts = cas.getTypeSystem();
      assertNotNull(ts.getType("NamedEntity"));
      assertNotNull(ts.getType("Person"));
      assertNotNull(ts.getType("Place"));
      assertNotNull(ts.getType("Org"));
      assertNotNull(ts.getType("DocumentStructure"));
      assertNotNull(ts.getType("Paragraph"));
      assertNotNull(ts.getType("Sentence"));
      assertNotNull(ts.getType("test.flowController.Test"));
    }
    catch (Exception e)
    {
			JUnitExtension.handleException(e);
    }
  }  
  
  public void testCollectionProcessComplete() throws Exception
  {
    try
    {
      //test simple primitive TextAnalysisEngine (using TestAnnotator class)
      TaeDescription primitiveDesc = new TaeDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc.setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      primitiveDesc.getMetaData().setName("Test Primitive TAE");
      PrimitiveAnalysisEngine_impl tae = new PrimitiveAnalysisEngine_impl();
      tae.initialize(primitiveDesc, null);
      tae.collectionProcessComplete(new ProcessTrace_impl());

      //test simple aggregate TextAnalysisEngine (again using TestAnnotator class)
      TaeDescription aggDesc = new TaeDescription_impl();
      aggDesc.setPrimitive(false);
      aggDesc.getMetaData().setName("Test Aggregate TAE");
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow(new String[]{"Test"});
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      AggregateAnalysisEngine_impl aggTae = new AggregateAnalysisEngine_impl();
      aggTae.initialize(aggDesc, null);
      aggTae.collectionProcessComplete(new ProcessTrace_impl());
    }
    catch (Exception e)
    {
      JUnitExtension.handleException(e);
    }
  }

  public void testBatchProcessComplete() throws Exception
  {
    try
    {
      //test simple primitive TextAnalysisEngine (using TestAnnotator class)
      TaeDescription primitiveDesc = new TaeDescription_impl();
      primitiveDesc.setPrimitive(true);
      primitiveDesc.setAnnotatorImplementationName("org.apache.uima.analysis_engine.impl.TestAnnotator");
      primitiveDesc.getMetaData().setName("Test Primitive TAE");
      PrimitiveAnalysisEngine_impl tae = new PrimitiveAnalysisEngine_impl();
      tae.initialize(primitiveDesc, null);
      tae.batchProcessComplete(new ProcessTrace_impl());

      //test simple aggregate TextAnalysisEngine (again using TestAnnotator class)
      TaeDescription aggDesc = new TaeDescription_impl();
      aggDesc.setPrimitive(false);
      aggDesc.getMetaData().setName("Test Aggregate TAE");
      aggDesc.getDelegateAnalysisEngineSpecifiersWithImports().put("Test", primitiveDesc);
      FixedFlow_impl flow = new FixedFlow_impl();
      flow.setFixedFlow(new String[]{"Test"});
      aggDesc.getAnalysisEngineMetaData().setFlowConstraints(flow);
      AggregateAnalysisEngine_impl aggTae = new AggregateAnalysisEngine_impl();
      aggTae.initialize(aggDesc, null);
      aggTae.batchProcessComplete(new ProcessTrace_impl());
    }
    catch (Exception e)
    {
      JUnitExtension.handleException(e);
    }
  }
  
  public void testTypeSystemInit() throws Exception
  {
    try
    {
      TaeDescription aggWithCcDesc = UIMAFramework.getXMLParser().
        parseTaeDescription(new XMLInputSource(
          JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateTaeWithCasConsumer.xml")));
      AggregateAnalysisEngine_impl aggTae = new AggregateAnalysisEngine_impl();
      aggTae.initialize(aggWithCcDesc, null);
      TCAS tcas = aggTae.newTCAS();
      tcas.setDocumentText("This is a test");
      aggTae.process(tcas);
      assertTrue(TestAnnotator.typeSystemInitCalled);
      assertTrue(AnnotationWriter.typeSystemInitCalled);
    }
    catch (Exception e)
    {
      JUnitExtension.handleException(e);
    }  
  }
  
  public void testProcessAndOutputNewCASes() throws Exception
  {
    try
    {
      //primitive
      AnalysisEngineDescription segmenterDesc = UIMAFramework.getXMLParser().
        parseAnalysisEngineDescription(new XMLInputSource(
          JUnitExtension.getFile("TextAnalysisEngineImplTest/NewlineSegmenter.xml")));
      AnalysisEngine tae = UIMAFramework.produceAnalysisEngine(segmenterDesc);
      CAS cas = tae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      CasIterator iter = tae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      CAS outCas = iter.next();
      assertEquals("Line one", outCas.getDocumentText());
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line two", outCas.getDocumentText());
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line three", outCas.getDocumentText());
      outCas.release();
      assertFalse(iter.hasNext());
      
      //aggregate
      AnalysisEngineDescription aggSegDesc = UIMAFramework.getXMLParser().
      parseAnalysisEngineDescription(new XMLInputSource(
        JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateWithSegmenter.xml")));
      tae = UIMAFramework.produceAnalysisEngine(aggSegDesc);
      cas = tae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      iter = tae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line one", outCas.getDocumentText());
      assertEquals("Line one", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line two", outCas.getDocumentText());
      assertEquals("Line two", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line three", outCas.getDocumentText());
      assertEquals("Line three", TestAnnotator.lastDocument);
      outCas.release();
      assertFalse(iter.hasNext());
      //Annotator should NOT get the original CAS according to the default flow
      assertEquals("Line three", TestAnnotator.lastDocument);
      
      //nested aggregate
      AnalysisEngineDescription nestedAggSegDesc = UIMAFramework.getXMLParser().
      parseAnalysisEngineDescription(new XMLInputSource(
        JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateContainingAggregateSegmenter.xml")));
      tae = UIMAFramework.produceAnalysisEngine(nestedAggSegDesc);
      cas = tae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      iter = tae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line one", outCas.getDocumentText());
      assertEquals("Line one", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line two", outCas.getDocumentText());
      assertEquals("Line two", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line three", outCas.getDocumentText());
      assertEquals("Line three", TestAnnotator.lastDocument);
      outCas.release();
      assertFalse(iter.hasNext());
      //Annotator should NOT get the original CAS according to the default flow
      assertEquals("Line three", TestAnnotator.lastDocument);
      
      
      //two segmenters
      AnalysisEngineDescription twoSegDesc = UIMAFramework.getXMLParser().
      parseAnalysisEngineDescription(new XMLInputSource(
        JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateWith2Segmenters.xml")));
      tae = UIMAFramework.produceAnalysisEngine(twoSegDesc);
      cas = tae.newCAS();
      cas.setDocumentText("One\tTwo\nThree\tFour");
      iter = tae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("One", outCas.getDocumentText());
      assertEquals("One", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Two", outCas.getDocumentText());
      assertEquals("Two", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("One\tTwo", outCas.getDocumentText());
      assertEquals("Two", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Three", outCas.getDocumentText());
      assertEquals("Three", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Four", outCas.getDocumentText());
      assertEquals("Four", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Three\tFour", outCas.getDocumentText());
      assertEquals("Four", TestAnnotator.lastDocument);
      outCas.release();
      assertFalse(iter.hasNext());
      //Annotator should NOT get the original CAS according to the default flow
      assertEquals("Four", TestAnnotator.lastDocument);           

      //dropping segments
      aggSegDesc = UIMAFramework.getXMLParser().
      parseAnalysisEngineDescription(new XMLInputSource(
        JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateSegmenterForDropTest.xml")));
      tae = UIMAFramework.produceAnalysisEngine(aggSegDesc);
      cas = tae.newCAS();
      cas.setDocumentText("Line one\nDROP\nLine two\nDROP\nLine three");
      //results should be the same as the first aggregate segmenter test.
      //segmetns whose text is DROP should not be output.
      iter = tae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line one", outCas.getDocumentText());
      assertEquals("Line one", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line two", outCas.getDocumentText());
      assertEquals("Line two", TestAnnotator.lastDocument);
      outCas.release();
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("Line three", outCas.getDocumentText());
      assertEquals("Line three", TestAnnotator.lastDocument);
      outCas.release();
      assertFalse(iter.hasNext());
      //Annotator should NOT get the original CAS according to the default flow
      assertEquals("Line three", TestAnnotator.lastDocument);
    }
    catch (Exception e)
    {
      JUnitExtension.handleException(e);
    }      
  }
  
  public void testProcessAndOutputNewCASesWithError() throws Exception
  {
    try
    {
      //aggregate
      AnalysisEngineDescription aggSegDesc = UIMAFramework.getXMLParser().
        parseAnalysisEngineDescription(new XMLInputSource(
          JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateWithSegmenterForErrorTest.xml")));
      AnalysisEngine tae = UIMAFramework.produceAnalysisEngine(aggSegDesc);
      CAS cas = tae.newCAS();
      for (int i = 0; i < 2; i++) //verify we can do this more than once
      {
        cas.setDocumentText("Line one\nLine two\nERROR");
        CasIterator iter = tae.processAndOutputNewCASes(cas);
        assertTrue(iter.hasNext());
        CAS outCas = iter.next();
        assertEquals("Line one", outCas.getDocumentText());
        outCas.release();
        assertTrue(iter.hasNext());
        outCas = iter.next();
        assertEquals("Line two", outCas.getDocumentText());
        outCas.release();
        try
        {
          assertTrue(iter.hasNext());
          outCas = iter.next();
          fail(); //the above should throw an exception
        }
        catch(AnalysisEngineProcessException e)
        {    
        }
        cas.reset();
      } 
 
      //nested aggregate
      AnalysisEngineDescription nestedAggSegDesc = UIMAFramework.getXMLParser().
        parseAnalysisEngineDescription(new XMLInputSource(
          JUnitExtension.getFile("TextAnalysisEngineImplTest/NestedAggregateSegmenterForErrorTest.xml")));
      tae = UIMAFramework.produceAnalysisEngine(nestedAggSegDesc);
      cas = tae.newCAS();
      for (int i = 0; i < 2; i++) //verify we can do this more than once
      {
        cas.setDocumentText("Line one\nLine two\nERROR");
        CasIterator iter = tae.processAndOutputNewCASes(cas);
        assertTrue(iter.hasNext());
        CAS outCas = iter.next();
        assertEquals("Line one", outCas.getDocumentText());
        outCas.release();
        assertTrue(iter.hasNext());
        outCas = iter.next();
        assertEquals("Line two", outCas.getDocumentText());
        outCas.release();
        try
        {
          assertTrue(iter.hasNext());
          outCas = iter.next();
          fail(); //the above should throw an exception
        }
        catch(AnalysisEngineProcessException e)
        {    
        }
        cas.reset();
      } 
      
      //2 segmenters
      AnalysisEngineDescription twoSegDesc = UIMAFramework.getXMLParser().
        parseAnalysisEngineDescription(new XMLInputSource(
          JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateWith2SegmentersForErrorTest.xml")));
      tae = UIMAFramework.produceAnalysisEngine(twoSegDesc);
      cas = tae.newCAS();
      for (int i = 0; i < 2; i++) //verify we can do this more than once
      {
        cas.setDocumentText("One\tTwo\nThree\tERROR");
        CasIterator iter = tae.processAndOutputNewCASes(cas);
        assertTrue(iter.hasNext());
        CAS outCas = iter.next();
        assertEquals("One", outCas.getDocumentText());
        outCas.release();
        assertTrue(iter.hasNext());
        outCas = iter.next();
        assertEquals("Two", outCas.getDocumentText());
        outCas.release();
        assertTrue(iter.hasNext());
        outCas = iter.next();
        assertEquals("One\tTwo", outCas.getDocumentText());
        outCas.release();
        assertTrue(iter.hasNext());
        outCas = iter.next();
        assertEquals("Three", outCas.getDocumentText());
        outCas.release();
        try
        {
          assertTrue(iter.hasNext());
          outCas = iter.next();
          fail(); //the above should throw an exception
        }
        catch(AnalysisEngineProcessException e)
        {    
        }
        cas.reset();
      }       
      
      //segmenter that requests too many CASes
      AnalysisEngineDescription segmenterDesc = UIMAFramework.getXMLParser().
      parseAnalysisEngineDescription(new XMLInputSource(
        JUnitExtension.getFile("TextAnalysisEngineImplTest/BadSegmenter.xml")));
      tae = UIMAFramework.produceAnalysisEngine(segmenterDesc);
      cas = tae.newCAS();
      cas.setDocumentText("Line one\nLine two\nLine three");
      CasIterator iter = tae.processAndOutputNewCASes(cas);
      assertTrue(iter.hasNext());
      CAS outCas = iter.next(); //first call OK
      outCas.release();
      assertTrue(iter.hasNext());
      //next call should fail with AnalysisEngineProcessException
      try
      {
        iter.next();
        fail(); //should not get here
      }
      catch(AnalysisEngineProcessException e)
      {        
        //should get here
      }
    }
    catch (Exception e)
    {
      JUnitExtension.handleException(e);
    }      
  }
  
  public void testResegment() throws Exception
  {
    try
    {
      //primitive
      AnalysisEngineDescription segmenterDesc = UIMAFramework.getXMLParser().
        parseAnalysisEngineDescription(new XMLInputSource(
          JUnitExtension.getFile("TextAnalysisEngineImplTest/NewlineResegmenter.xml")));
      AnalysisEngine tae = UIMAFramework.produceAnalysisEngine(segmenterDesc);
      CAS inputCas1 = tae.newCAS();
      Type sdiType = inputCas1.getTypeSystem().getType("org.apache.uima.examples.SourceDocumentInformation");
      Feature uriFeat = sdiType.getFeatureByBaseName("uri");
      inputCas1.setDocumentText("This is");
      FeatureStructure sdiFS = inputCas1.createFS(sdiType);
      sdiFS.setStringValue(uriFeat,"cas1");
      inputCas1.getIndexRepository().addFS(sdiFS);
      CAS inputCas2 = tae.newCAS();
      inputCas2.setDocumentText(" one.\nThis is");
      FeatureStructure sdiFS2 = inputCas2.createFS(sdiType);
      sdiFS2.setStringValue(uriFeat,"cas2");
      inputCas2.getIndexRepository().addFS(sdiFS2);
      CAS inputCas3 = tae.newCAS();
      inputCas3.setDocumentText(" two.\n");
      FeatureStructure sdiFS3 = inputCas3.createFS(sdiType);
      sdiFS3.setStringValue(uriFeat,"cas3");
      inputCas3.getIndexRepository().addFS(sdiFS3);
            
      //input first CAS.  Should be no segments yet.
      CasIterator iter = tae.processAndOutputNewCASes(inputCas1);
      assertFalse(iter.hasNext());
      //input second CAS.  We should get back one segment.
      iter = tae.processAndOutputNewCASes(inputCas2);
      assertTrue(iter.hasNext());
      CAS outCas = iter.next();
      assertEquals("This is one.", outCas.getDocumentText());
      //-- check SourceDocumentInformation FSs
      Iterator sdiIter = outCas.getAnnotationIndex(sdiType).iterator();
      assertTrue(sdiIter.hasNext());
      AnnotationFS outSdiFs = (AnnotationFS)sdiIter.next();
      assertEquals("This is", outSdiFs.getCoveredText());
      assertEquals("cas1", outSdiFs.getStringValue(uriFeat));
      assertTrue(sdiIter.hasNext());
      outSdiFs = (AnnotationFS)sdiIter.next();
      assertEquals(" one.", outSdiFs.getCoveredText());
      assertEquals("cas2", outSdiFs.getStringValue(uriFeat));
      assertFalse(sdiIter.hasNext());
      //--
      assertFalse(iter.hasNext());
      
      //input third CAS.  We should get back one more segment.
      iter = tae.processAndOutputNewCASes(inputCas3);
      assertTrue(iter.hasNext());
      outCas = iter.next();
      assertEquals("This is two.", outCas.getDocumentText());
      //-- check SourceDocumentInformation FSs
      sdiIter = outCas.getAnnotationIndex(sdiType).iterator();
      assertTrue(sdiIter.hasNext());
      outSdiFs = (AnnotationFS)sdiIter.next();
      assertEquals("This is", outSdiFs.getCoveredText());
      assertEquals("cas2", outSdiFs.getStringValue(uriFeat));
      assertTrue(sdiIter.hasNext());
      outSdiFs = (AnnotationFS)sdiIter.next();
      assertEquals(" two.", outSdiFs.getCoveredText());
      assertEquals("cas3", outSdiFs.getStringValue(uriFeat));
      assertFalse(sdiIter.hasNext());
      //--
      assertFalse(iter.hasNext());
    }
    catch (Exception e)
    {
      JUnitExtension.handleException(e);
    }      
  }
    
  public void testComputeAnalysisComponentResultSpec() throws Exception 
  {
    try
    {
      AnalysisEngineDescription aeDesc = UIMAFramework.getXMLParser().
        parseAnalysisEngineDescription(new XMLInputSource(
          JUnitExtension.getFile("SequencerTest/Annotator1.xml")));
      PrimitiveAnalysisEngine_impl ae = (PrimitiveAnalysisEngine_impl)UIMAFramework.produceAnalysisEngine(aeDesc);
      CAS cas = ae.newCAS();
      ResultSpecification resultSpec = new ResultSpecification_impl();
      resultSpec.addResultType("uima.tt.TokenLikeAnnotation", true);
      resultSpec.compile(cas.getTypeSystem());
      ResultSpecification acResultSpec = 
        ae.computeAnalysisComponentResultSpec(resultSpec, ae.getAnalysisEngineMetaData().getCapabilities());
      assertTrue(acResultSpec.containsType("uima.tt.TokenAnnotation"));
      assertFalse(acResultSpec.containsType("uima.tt.SentenceAnnotation"));
      assertFalse(acResultSpec.containsType("uima.tt.Lemma"));     
    }
    catch (Exception e)
    {
      JUnitExtension.handleException(e);
    }       
  }
}
