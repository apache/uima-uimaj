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

package org.apache.uima.util;

import java.io.File;
import java.util.ArrayList;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.cas.text.TCAS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;

/**
 * 
 * @author Adam Lally 
 */
public class CasCreationUtilsTest extends TestCase
{

  /**
   * Constructor for CasCreationUtilsTest.
   * @param arg0
   */
  public CasCreationUtilsTest(String arg0)
  {
    super(arg0);
  }

  public void testMergeTypeSystems() throws Exception
  {
    try
    {
      TypeSystemDescription ts1desc = UIMAFramework.getXMLParser().
        parseTypeSystemDescription(new XMLInputSource(JUnitExtension.
            getFile("CasCreationUtilsTest/TypeSystem1.xml")));

      Assert.assertEquals(1, ts1desc.getType("Type1").getFeatures().length);
      Assert.assertEquals(1, ts1desc.getType("Type2").getFeatures().length);
     
      TypeSystemDescription ts2desc = UIMAFramework.getXMLParser().
      parseTypeSystemDescription(new XMLInputSource(JUnitExtension.
          getFile("CasCreationUtilsTest/TypeSystem2.xml")));
      Assert.assertEquals(1, ts2desc.getType("Type1").getFeatures().length);
      Assert.assertEquals(1, ts2desc.getType("Type2").getFeatures().length);
      
      ArrayList tsList = new ArrayList();
      tsList.add(ts1desc);
      tsList.add(ts2desc);
      TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(tsList);

      Assert.assertEquals(2, merged.getType("Type1").getFeatures().length);
      Assert.assertEquals(2, merged.getType("Type2").getFeatures().length);
    }
    catch (Exception e)
    {
      JUnitExtension.handleException(e);
    }
  }

  public void testCreateTCasCollectionPropertiesResourceManager() throws Exception
  {
    try
    {
      //parse descriptor
      File taeDescriptorWithImport = JUnitExtension
          .getFile("CasCreationUtilsTest/TaeWithImports.xml");
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
          new XMLInputSource(taeDescriptorWithImport));

      //create Resource Manager & set data path - necessary to resolve imports
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      String pathSep = System.getProperty("path.separator");
      resMgr.setDataPath(JUnitExtension.getFile("TypeSystemDescriptionImplTest/dataPathDir")
          .getAbsolutePath()
          + pathSep
          + JUnitExtension.getFile("TypePrioritiesImplTest/dataPathDir").getAbsolutePath()
          + pathSep
          + JUnitExtension.getFile("FsIndexCollectionImplTest/dataPathDir").getAbsolutePath());

      //call method
      ArrayList descList = new ArrayList();
      descList.add(desc);
      TCAS tcas = CasCreationUtils.createTCas(descList, UIMAFramework
          .getDefaultPerformanceTuningProperties(), resMgr);
      //check that imports were resolved correctly   
      assertNotNull(tcas.getTypeSystem().getType("DocumentStructure"));
      assertNotNull(tcas.getTypeSystem().getType("NamedEntity"));
      assertNotNull(tcas.getTypeSystem().getType("TestType3"));

      assertNotNull(tcas.getIndexRepository().getIndex("TestIndex"));
      assertNotNull(tcas.getIndexRepository().getIndex("ReverseAnnotationIndex"));
      assertNotNull(tcas.getIndexRepository().getIndex("DocumentStructureIndex"));

      //check of type priority
      AnnotationFS fs1 = tcas.createAnnotation(tcas.getTypeSystem().getType("Paragraph"), 0, 1);
      AnnotationFS fs2 = tcas.createAnnotation(tcas.getTypeSystem().getType("Sentence"), 0, 1);
      assertTrue(tcas.getAnnotationIndex().compare(fs1, fs2) < 0);
    }
    catch (Exception e)
    {
      JUnitExtension.handleException(e);
    }
  }

  public void testAggregateWithImports() throws Exception
  {
    try
    {
      String pathSep = System.getProperty("path.separator");
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setDataPath(JUnitExtension.getFile("TypeSystemDescriptionImplTest/dataPathDir")
          .getAbsolutePath()
          + pathSep
          + JUnitExtension.getFile("TypePrioritiesImplTest/dataPathDir").getAbsolutePath()
          + pathSep
          + JUnitExtension.getFile("FsIndexCollectionImplTest/dataPathDir").getAbsolutePath());

      File taeDescriptorWithImport = JUnitExtension
          .getFile("CasCreationUtilsTest/AggregateTaeWithImports.xml");
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(
          new XMLInputSource(taeDescriptorWithImport));
      ArrayList mdList = new ArrayList();
      mdList.add(desc);
      TCAS tcas = CasCreationUtils.createTCas(mdList, UIMAFramework
          .getDefaultPerformanceTuningProperties(), resMgr);
      //check that imports were resolved correctly   
      assertNotNull(tcas.getTypeSystem().getType("DocumentStructure"));
      assertNotNull(tcas.getTypeSystem().getType("NamedEntity"));
      assertNotNull(tcas.getTypeSystem().getType("TestType3"));
      assertNotNull(tcas.getTypeSystem().getType("Sentence"));

      assertNotNull(tcas.getIndexRepository().getIndex("TestIndex"));
      assertNotNull(tcas.getIndexRepository().getIndex("ReverseAnnotationIndex"));
      assertNotNull(tcas.getIndexRepository().getIndex("DocumentStructureIndex"));

      //Check elementType and multipleReferencesAllowed for array feature
      Feature arrayFeat = tcas.getTypeSystem().getFeatureByFullName("Paragraph:sentences");
      assertNotNull(arrayFeat);
      assertFalse(arrayFeat.isMultipleReferencesAllowed());
      Type sentenceArrayType = arrayFeat.getRange();
      assertNotNull(sentenceArrayType);
      assertTrue(sentenceArrayType.isArray());
      assertEquals(tcas.getTypeSystem().getType("Sentence"), sentenceArrayType.getComponentType());

      Feature arrayFeat2 = tcas.getTypeSystem().getFeatureByFullName("Paragraph:testMultiRefAllowedFeature");
      assertNotNull(arrayFeat2);
      assertTrue(arrayFeat2.isMultipleReferencesAllowed());

      
      //test imports aren't resolved more than once
      Object spec1 = desc.getDelegateAnalysisEngineSpecifiers().get("Annotator1");
      assertNotNull(spec1);
      Object spec2 = desc.getDelegateAnalysisEngineSpecifiers().get("Annotator1");
      assertTrue(spec1 == spec2);

      //test removal
      desc.getDelegateAnalysisEngineSpecifiersWithImports().remove("Annotator1");
      assertTrue(desc.getDelegateAnalysisEngineSpecifiers().isEmpty());
    }
    catch (Exception e)
    {
      JUnitExtension.handleException(e);
    }
  }
  
  public void testMergeDelegateAnalysisEngineMetaData() throws Exception
  {
    try
    {
      File descFile = JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml");
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(descFile));
      TypeSystemDescription typeSys = CasCreationUtils.mergeDelegateAnalysisEngineTypeSystems(desc);

      //test results of merge
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
    }
    catch(Exception e)
    {
      JUnitExtension.handleException(e);
    }
  }

  public void testMergeDelegateAnalysisEngineTypePriorities() throws Exception
  {
    try
    {
      File descFile = JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml");
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(descFile));
      TypePriorities pri = CasCreationUtils.mergeDelegateAnalysisEngineTypePriorities(desc);

      //test results of merge
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
       
    }
    catch(Exception e)
    {
      JUnitExtension.handleException(e);
    }
  }
  
  public void testMergeDelegateAnalysisEngineFsIndexCollections() throws Exception
  {
    try
    {
      File descFile = JUnitExtension.getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml");
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(new XMLInputSource(descFile));
      FsIndexCollection indexColl = CasCreationUtils.mergeDelegateAnalysisEngineFsIndexCollections(desc);

      //test results of merge
      FsIndexDescription[] indexes = indexColl.getFsIndexes();
      Assert.assertEquals(3, indexes.length);  
      //order of indexes is not defined
      String label0 = indexes[0].getLabel();
      String label1 = indexes[1].getLabel();
      String label2 = indexes[2].getLabel();
      Assert.assertTrue(label0.equals("DocStructIndex") || label1.equals("DocStructIndex") || label2.equals("DocStructIndex"));
      Assert.assertTrue(label0.equals("PlaceIndex") || label1.equals("PlaceIndex") || label2.equals("PlaceIndex"));
      Assert.assertTrue(label0.equals("FlowControllerTestIndex") || label1.equals("FlowControllerTestIndex") || label2.equals("FlowControllerTestIndex"));      
    }
    catch(Exception e)
    {
      JUnitExtension.handleException(e);
    }
  }
  
  public void testSetupTypeSystem() throws Exception
  {
    try
    {
      //test that duplicate feature names on supertype and subtype works
      //regardless of the order in which the types appear in the TypeSystemDescription
      TypeSystemDescription tsd1 = new TypeSystemDescription_impl();
      TypeDescription supertype = tsd1.addType("test.Super", "", "uima.cas.TOP");
      supertype.addFeature("testfeat", "", "uima.cas.Integer");
      TypeDescription subtype = tsd1.addType("test.Sub", "", "test.Super");
      subtype.addFeature("testfeat", "", "uima.cas.Integer");    
      
      CASMgr casMgr = CASFactory.createCAS();
      CasCreationUtils.setupTypeSystem(casMgr, tsd1);
      assertNotNull(casMgr.getTypeSystemMgr().getType("test.Super").getFeatureByBaseName("testfeat"));

      TypeSystemDescription tsd2 = new TypeSystemDescription_impl();
      tsd2.setTypes(new TypeDescription[]{subtype,supertype});
      
      casMgr = CASFactory.createCAS();
      CasCreationUtils.setupTypeSystem(casMgr, tsd2);
      assertNotNull(casMgr.getTypeSystemMgr().getType("test.Super").getFeatureByBaseName("testfeat"));

    }
    catch (ResourceInitializationException e)
    {
      JUnitExtension.handleException(e);
    }
  }  
}
