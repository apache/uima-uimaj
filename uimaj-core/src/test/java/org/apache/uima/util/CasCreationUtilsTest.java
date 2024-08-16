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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.Feature;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.cas.admin.CASFactory;
import org.apache.uima.cas.admin.CASMgr;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.ProcessingResourceMetaData;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypePriorityList;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.resource.metadata.impl.ConfigurationParameter_impl;
import org.apache.uima.resource.metadata.impl.FsIndexCollection_impl;
import org.apache.uima.resource.metadata.impl.FsIndexDescription_impl;
import org.apache.uima.resource.metadata.impl.TypePriorities_impl;
import org.apache.uima.resource.metadata.impl.TypePriorityList_impl;
import org.apache.uima.resource.metadata.impl.TypeSystemDescription_impl;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.junit.jupiter.api.Test;

public class CasCreationUtilsTest {

  // this test is a skeleton
  // it is currently disabled - it doesn't actually check anything
  // using debug one can see that no errors are thrown if the allowed values for subtypes of string
  // differ when merging
  // and that no merging occurs of the allowed values - the 1st one "wins" (as of 5/2013)
  // See Jira https://issues.apache.org/jira/browse/UIMA-2917
  @Test
  public void testStringSubtype() throws Exception {
    try {

      TypeSystemDescription ts1desc = UIMAFramework.getXMLParser()
              .parseTypeSystemDescription(new XMLInputSource(JUnitExtension
                      .getFile("CasCreationUtilsTest/TypeSystemMergeStringSubtypeBasePlus.xml")));

      TypeSystemDescription result = checkMergeTypeSystem(ts1desc,
              "TypeSystemMergeStringSubtypeBase.xml",
              ResourceInitializationException.ALLOWED_VALUES_NOT_IDENTICAL);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testMergeTypeSystems() throws Exception {
    try {
      TypeSystemDescription ts1desc = UIMAFramework.getXMLParser().parseTypeSystemDescription(
              new XMLInputSource(JUnitExtension.getFile("CasCreationUtilsTest/TypeSystem1.xml")));

      assertThat(ts1desc.getType("Type1").getFeatures()).hasSize(1);
      assertThat(ts1desc.getType("Type2").getFeatures()).hasSize(1);
      assertThat(ts1desc.getType("Type3").getFeatures()).hasSize(1);

      TypeSystemDescription ts2desc = UIMAFramework.getXMLParser().parseTypeSystemDescription(
              new XMLInputSource(JUnitExtension.getFile("CasCreationUtilsTest/TypeSystem2.xml")));
      assertThat(ts2desc.getType("Type1").getFeatures()).hasSize(1);
      assertThat(ts2desc.getType("Type2").getFeatures()).hasSize(1);

      ArrayList<TypeSystemDescription> tsList = new ArrayList<>();
      tsList.add(ts1desc);
      tsList.add(ts2desc);
      Map typesWithMergedFeatures = new HashMap();
      TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(tsList,
              UIMAFramework.newDefaultResourceManager(), typesWithMergedFeatures);

      assertThat(merged.getType("Type1").getFeatures()).hasSize(2);
      assertThat(merged.getType("Type2").getFeatures()).hasSize(2);
      assertThat(merged.getType("Type3").getFeatures()).hasSize(1);

      assertEquals(2, typesWithMergedFeatures.size());
      assertTrue(typesWithMergedFeatures.containsKey("Type1"));
      assertTrue(typesWithMergedFeatures.containsKey("Type2"));

      // make sure one-arg version doesn't fail
      CasCreationUtils.mergeTypeSystems(tsList);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testMergeTypeSystemElementType() throws Exception {
    try {

      TypeSystemDescription ts1desc = UIMAFramework.getXMLParser()
              .parseTypeSystemDescription(new XMLInputSource(
                      JUnitExtension.getFile("CasCreationUtilsTest/TypeSystemMergeBase.xml")));

      checkMergeTypeSystem(ts1desc, "TypeSystemMergeWrongElementType1.xml",
              ResourceInitializationException.INCOMPATIBLE_ELEMENT_RANGE_TYPES);

      checkMergeTypeSystem(ts1desc, "TypeSystemMergeWrongElementType2.xml",
              ResourceInitializationException.INCOMPATIBLE_ELEMENT_RANGE_TYPES);

      checkMergeTypeSystem(ts1desc, "TypeSystemMergeWrongMultiRef1.xml",
              ResourceInitializationException.INCOMPATIBLE_MULTI_REFS);

      checkMergeTypeSystem(ts1desc, "TypeSystemMergeWrongMultiRef2.xml",
              ResourceInitializationException.INCOMPATIBLE_MULTI_REFS);

      checkMergeTypeSystem(ts1desc, "TypeSystemMergeWrongMultiRef3.xml",
              ResourceInitializationException.INCOMPATIBLE_MULTI_REFS);

      checkMergeTypeSystem(ts1desc, "TypeSystemMergeOkMultiRef.xml", null);

      checkMergeTypeSystem(ts1desc, "TypeSystemMergeNoElementType.xml", null);

      checkMergeTypeSystem(ts1desc, "typeSystemMergeTopElementType.xml", null);

      checkMergeTypeSystem(ts1desc, "TypeSystemMergeWrongElementTypeWithNone.xml",
              ResourceInitializationException.INCOMPATIBLE_ELEMENT_RANGE_TYPES);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  private TypeSystemDescription checkMergeTypeSystem(TypeSystemDescription ts1desc, String typeFile,
          String msgKey) throws Exception {
    TypeSystemDescription mergedTS = null;
    try {

      TypeSystemDescription ts2desc = UIMAFramework.getXMLParser().parseTypeSystemDescription(
              new XMLInputSource(JUnitExtension.getFile("CasCreationUtilsTest/" + typeFile)));

      List<TypeSystemDescription> tsList = new ArrayList<>();
      tsList.add(ts1desc);
      tsList.add(ts2desc);

      boolean rightExceptionThrown = (null != msgKey) ? false : true;
      try {
        mergedTS = CasCreationUtils.mergeTypeSystems(tsList,
                UIMAFramework.newDefaultResourceManager(), new HashMap());
      } catch (ResourceInitializationException rie) {
        rightExceptionThrown = (null != msgKey) && rie.hasMessageKey(msgKey);
      }
      assertTrue(rightExceptionThrown);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
    return mergedTS;
  }

  @Test
  public void testMergeTypeSystemsWithDifferentSupertypes() throws Exception {
    try {
      TypeSystemDescription ts1desc = UIMAFramework.getXMLParser()
              .parseTypeSystemDescription(new XMLInputSource(
                      JUnitExtension.getFile("CasCreationUtilsTest/SupertypeMergeTest1.xml")));
      assertEquals("uima.tcas.Annotation", ts1desc.getType("uima.test.Sub").getSupertypeName());
      TypeSystemDescription ts2desc = UIMAFramework.getXMLParser()
              .parseTypeSystemDescription(new XMLInputSource(
                      JUnitExtension.getFile("CasCreationUtilsTest/SupertypeMergeTest2.xml")));
      assertEquals("uima.test.Super", ts2desc.getType("uima.test.Sub").getSupertypeName());

      List<TypeSystemDescription> tsList = new ArrayList<>();
      tsList.add(ts1desc);
      tsList.add(ts2desc);
      TypeSystemDescription merged = CasCreationUtils.mergeTypeSystems(tsList,
              UIMAFramework.newDefaultResourceManager());
      assertEquals("uima.test.Super", merged.getType("uima.test.Sub").getSupertypeName());

      // try merging in the other order - bug UIMA-826 was an order dependency in the behavior of
      // this kind of merging
      tsList = new ArrayList<>();
      tsList.add(ts2desc);
      tsList.add(ts1desc);
      merged = CasCreationUtils.mergeTypeSystems(tsList, UIMAFramework.newDefaultResourceManager());
      assertEquals("uima.test.Super", merged.getType("uima.test.Sub").getSupertypeName());

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testAggregateWithImports() throws Exception {
    try {
      String pathSep = System.getProperty("path.separator");
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setDataPath(JUnitExtension.getFile("TypeSystemDescriptionImplTest/dataPathDir")
              .getAbsolutePath() + pathSep
              + JUnitExtension.getFile("TypePrioritiesImplTest/dataPathDir").getAbsolutePath()
              + pathSep
              + JUnitExtension.getFile("FsIndexCollectionImplTest/dataPathDir").getAbsolutePath());

      File taeDescriptorWithImport = JUnitExtension
              .getFile("CasCreationUtilsTest/AggregateTaeWithImports.xml");
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(taeDescriptorWithImport));
      ArrayList<AnalysisEngineDescription> mdList = new ArrayList<>();
      mdList.add(desc);
      CAS tcas = CasCreationUtils.createCas(mdList,
              UIMAFramework.getDefaultPerformanceTuningProperties(), resMgr);
      // check that imports were resolved correctly
      assertNotNull(tcas.getTypeSystem().getType("DocumentStructure"));
      assertNotNull(tcas.getTypeSystem().getType("NamedEntity"));
      assertNotNull(tcas.getTypeSystem().getType("TestType3"));
      assertNotNull(tcas.getTypeSystem().getType("Sentence"));

      assertNotNull(tcas.getIndexRepository().getIndex("TestIndex"));
      assertNotNull(tcas.getIndexRepository().getIndex("ReverseAnnotationIndex"));
      assertNotNull(tcas.getIndexRepository().getIndex("DocumentStructureIndex"));

      // Check elementType and multipleReferencesAllowed for array feature
      Feature arrayFeat = tcas.getTypeSystem().getFeatureByFullName("Paragraph:sentences");
      assertNotNull(arrayFeat);
      assertFalse(arrayFeat.isMultipleReferencesAllowed());
      Type sentenceArrayType = arrayFeat.getRange();
      assertNotNull(sentenceArrayType);
      assertTrue(sentenceArrayType.isArray());
      assertEquals(tcas.getTypeSystem().getType("Sentence"), sentenceArrayType.getComponentType());

      Feature arrayFeat2 = tcas.getTypeSystem()
              .getFeatureByFullName("Paragraph:testMultiRefAllowedFeature");
      assertNotNull(arrayFeat2);
      assertTrue(arrayFeat2.isMultipleReferencesAllowed());

      // test imports aren't resolved more than once
      Object spec1 = desc.getDelegateAnalysisEngineSpecifiers().get("Annotator1");
      assertNotNull(spec1);
      Object spec2 = desc.getDelegateAnalysisEngineSpecifiers().get("Annotator1");
      assertTrue(spec1 == spec2);

      // test removal
      desc.getDelegateAnalysisEngineSpecifiersWithImports().remove("Annotator1");
      assertTrue(desc.getDelegateAnalysisEngineSpecifiers().isEmpty());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testMergeDelegateAnalysisEngineTypeSystems() throws Exception {
    try {
      File descFile = JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml");
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(descFile));
      Map mergedTypes = new HashMap();
      TypeSystemDescription typeSys = CasCreationUtils.mergeDelegateAnalysisEngineTypeSystems(desc,
              UIMAFramework.newDefaultResourceManager(), mergedTypes);

      // test results of merge
      assertThat(typeSys.getTypes()).hasSize(8);

      TypeDescription type0 = typeSys.getType("NamedEntity");
      assertThat(type0).isNotNull();
      assertThat(type0.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type0.getFeatures()).hasSize(1);

      TypeDescription type1 = typeSys.getType("Person");
      assertThat(type1).isNotNull();
      assertThat(type1.getSupertypeName()).isEqualTo("NamedEntity");
      assertThat(type1.getFeatures()).hasSize(1);

      TypeDescription type2 = typeSys.getType("Place");
      assertThat(type2).isNotNull();
      assertThat(type2.getSupertypeName()).isEqualTo("NamedEntity");
      assertThat(type2.getFeatures()).hasSize(3);

      TypeDescription type3 = typeSys.getType("Org");
      assertThat(type3).isNotNull();
      assertThat(type3.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type3.getFeatures()).isEmpty();

      TypeDescription type4 = typeSys.getType("DocumentStructure");
      assertThat(type4).isNotNull();
      assertThat(type4.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type4.getFeatures()).isEmpty();

      TypeDescription type5 = typeSys.getType("Paragraph");
      assertThat(type5).isNotNull();
      assertThat(type5.getSupertypeName()).isEqualTo("DocumentStructure");
      assertThat(type5.getFeatures()).isEmpty();

      TypeDescription type6 = typeSys.getType("Sentence");
      assertThat(type6).isNotNull();
      assertThat(type6.getSupertypeName()).isEqualTo("DocumentStructure");
      assertThat(type6.getFeatures()).isEmpty();

      TypeDescription type7 = typeSys.getType("test.flowController.Test");
      assertThat(type7).isNotNull();
      assertThat(type7.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type7.getFeatures()).hasSize(1);

      // Place has merged features, Person has different supertype
      assertEquals(2, mergedTypes.size());
      assertTrue(mergedTypes.containsKey("Place"));
      assertTrue(mergedTypes.containsKey("Person"));

      // make sure one-arg version doesn't fail
      CasCreationUtils.mergeDelegateAnalysisEngineTypeSystems(desc);

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testMergeDelegateAnalysisEngineTypePriorities() throws Exception {
    try {
      File descFile = JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml");
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(descFile));
      TypePriorities pri = CasCreationUtils.mergeDelegateAnalysisEngineTypePriorities(desc);

      // test results of merge
      assertThat(pri).isNotNull();
      TypePriorityList[] priLists = pri.getPriorityLists();
      assertThat(priLists).hasSize(3);
      String[] list0 = priLists[0].getTypes();
      String[] list1 = priLists[1].getTypes();
      String[] list2 = priLists[2].getTypes();
      // order of the three lists is not defined
      assertThat((list0.length == 2 && list1.length == 2 && list2.length == 3)
              || (list0.length == 2 && list1.length == 3 && list2.length == 2)
              || (list0.length == 3 && list1.length == 2 && list2.length == 2)).isTrue();

    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testMergeDelegateAnalysisEngineFsIndexCollections() throws Exception {
    try {
      File descFile = JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml");
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(descFile));
      FsIndexCollection indexColl = CasCreationUtils
              .mergeDelegateAnalysisEngineFsIndexCollections(desc);

      // test results of merge
      FsIndexDescription[] indexes = indexColl.getFsIndexes();
      assertThat(indexes).hasSize(3);
      // order of indexes is not defined
      String label0 = indexes[0].getLabel();
      String label1 = indexes[1].getLabel();
      String label2 = indexes[2].getLabel();
      assertThat(label0.equals("DocStructIndex") || label1.equals("DocStructIndex")
              || label2.equals("DocStructIndex")).isTrue();
      assertThat(label0.equals("PlaceIndex") || label1.equals("PlaceIndex")
              || label2.equals("PlaceIndex")).isTrue();
      assertThat(
              label0.equals("FlowControllerTestIndex") || label1.equals("FlowControllerTestIndex")
                      || label2.equals("FlowControllerTestIndex")).isTrue();
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testSetupTypeSystem() throws Exception {
    try {
      // test that duplicate feature names on supertype and subtype works
      // regardless of the order in which the types appear in the TypeSystemDescription
      TypeSystemDescription tsd1 = new TypeSystemDescription_impl();
      TypeDescription supertype = tsd1.addType("test.Super", "", "uima.cas.TOP");
      supertype.addFeature("testfeat", "", "uima.cas.Integer");
      TypeDescription subtype = tsd1.addType("test.Sub", "", "test.Super");
      subtype.addFeature("testfeat", "", "uima.cas.Integer");

      CASMgr casMgr = CASFactory.createCAS();
      CasCreationUtils.setupTypeSystem(casMgr, tsd1);
      assertNotNull(
              casMgr.getTypeSystemMgr().getType("test.Super").getFeatureByBaseName("testfeat"));

      TypeSystemDescription tsd2 = new TypeSystemDescription_impl();
      tsd2.setTypes(subtype, supertype);

      casMgr = CASFactory.createCAS();
      CasCreationUtils.setupTypeSystem(casMgr, tsd2);
      assertNotNull(
              casMgr.getTypeSystemMgr().getType("test.Super").getFeatureByBaseName("testfeat"));

    } catch (ResourceInitializationException e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testCreateCasCollectionPropertiesResourceManager() throws Exception {
    try {
      // parse an AE descriptor
      File taeDescriptorWithImport = JUnitExtension
              .getFile("CasCreationUtilsTest/TaeWithImports.xml");
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(taeDescriptorWithImport));

      // create Resource Manager & set data path - necessary to resolve imports
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      String pathSep = System.getProperty("path.separator");
      resMgr.setDataPath(JUnitExtension.getFile("TypeSystemDescriptionImplTest/dataPathDir")
              .getAbsolutePath() + pathSep
              + JUnitExtension.getFile("TypePrioritiesImplTest/dataPathDir").getAbsolutePath()
              + pathSep
              + JUnitExtension.getFile("FsIndexCollectionImplTest/dataPathDir").getAbsolutePath());

      // call method
      ArrayList<AnalysisEngineDescription> descList = new ArrayList<>();
      descList.add(desc);
      CAS cas = CasCreationUtils.createCas(descList,
              UIMAFramework.getDefaultPerformanceTuningProperties(), resMgr);
      // check that imports were resolved correctly
      assertNotNull(cas.getTypeSystem().getType("DocumentStructure"));
      assertNotNull(cas.getTypeSystem().getType("NamedEntity"));
      assertNotNull(cas.getTypeSystem().getType("TestType3"));

      assertNotNull(cas.getIndexRepository().getIndex("TestIndex"));
      assertNotNull(cas.getIndexRepository().getIndex("ReverseAnnotationIndex"));
      assertNotNull(cas.getIndexRepository().getIndex("DocumentStructureIndex"));

      // check of type priority
      AnnotationFS fs1 = cas.createAnnotation(cas.getTypeSystem().getType("Paragraph"), 0, 1);
      AnnotationFS fs2 = cas.createAnnotation(cas.getTypeSystem().getType("Sentence"), 0, 1);
      assertTrue(cas.getAnnotationIndex().compare(fs1, fs2) < 0);
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testCreateCasCollection() throws Exception {
    try {
      // create two Type System description objects
      TypeSystemDescription tsd1 = new TypeSystemDescription_impl();
      TypeDescription supertype = tsd1.addType("test.Super", "", "uima.tcas.Annotation");
      supertype.addFeature("testfeat", "", "uima.cas.Integer");
      TypeDescription subtype = tsd1.addType("test.Sub", "", "test.Super");
      subtype.addFeature("testfeat", "", "uima.cas.Integer");

      TypeSystemDescription tsd2 = new TypeSystemDescription_impl();
      TypeDescription fooType = tsd1.addType("test.Foo", "", "uima.cas.TOP");
      fooType.addFeature("bar", "", "uima.cas.String");

      // create index and priorities descriptions

      FsIndexCollection indexes = new FsIndexCollection_impl();
      FsIndexDescription index = new FsIndexDescription_impl();
      index.setLabel("MyIndex");
      index.setTypeName("test.Foo");
      index.setKind(FsIndexDescription.KIND_BAG);
      indexes.addFsIndex(index);

      TypePriorities priorities = new TypePriorities_impl();
      TypePriorityList priList = new TypePriorityList_impl();
      priList.addType("test.Foo");
      priList.addType("test.Sub");
      priList.addType("test.Super");
      priorities.addPriorityList(priList);

      // create a CAS containing all these definitions
      ArrayList descList = new ArrayList();
      descList.add(tsd1);
      descList.add(tsd2);
      descList.add(indexes);
      descList.add(priorities);

      CAS cas = CasCreationUtils.createCas(descList);

      // check that type system has been installed
      TypeSystem ts = cas.getTypeSystem();
      Type supertypeHandle = ts.getType(supertype.getName());
      assertNotNull(supertypeHandle);
      assertNotNull(supertypeHandle.getFeatureByBaseName("testfeat"));
      Type subtypeHandle = ts.getType(subtype.getName());
      assertNotNull(subtypeHandle);
      assertNotNull(subtypeHandle.getFeatureByBaseName("testfeat"));
      Type fooTypeHandle = ts.getType(fooType.getName());
      assertNotNull(fooTypeHandle);
      assertNotNull(fooTypeHandle.getFeatureByBaseName("bar"));

      // check that index exists
      assertNotNull(cas.getIndexRepository().getIndex("MyIndex"));

      // test that priorities work
      cas.createFS(supertypeHandle);
      cas.createFS(subtypeHandle);
      FSIterator iter = cas.getAnnotationIndex().iterator();
      while (iter.isValid()) {
        if (iter.get().getType() == subtypeHandle) {
          break;
        }
        if (iter.get().getType() == supertypeHandle) {
          fail();
        }
        iter.moveToNext();
      }

      // test that passing an invalid object causes an error
      descList.add(new ConfigurationParameter_impl());
      try {
        CasCreationUtils.createCas(descList);
        fail();
      } catch (ResourceInitializationException e) {
        // expected
      }
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testCreateCasTypeSystemDescription() throws Exception {
    try {
      // parse type system description
      TypeSystemDescription tsDesc = UIMAFramework.getXMLParser()
              .parseTypeSystemDescription(new XMLInputSource(
                      JUnitExtension.getFile("CasCreationUtilsTest/SupertypeMergeTestMaster.xml")));

      // call method
      CAS cas = CasCreationUtils.createCas(tsDesc, null, null);

      // check that imports were resolved and supertype merged properly
      Type subType = cas.getTypeSystem().getType("uima.test.Sub");
      assertNotNull(subType);
      Type superType = cas.getTypeSystem().getType("uima.test.Super");
      assertNotNull(superType);
      assertTrue(cas.getTypeSystem().subsumes(superType, subType));
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }

  @Test
  public void testMergeDelegateAnalysisEngineMetaData() throws Exception {
    try {
      File descFile = JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml");
      AnalysisEngineDescription desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(descFile));
      Map mergedTypes = new HashMap();
      ProcessingResourceMetaData mergedMetaData = CasCreationUtils
              .mergeDelegateAnalysisEngineMetaData(desc, UIMAFramework.newDefaultResourceManager(),
                      mergedTypes, null);
      TypeSystemDescription typeSys = mergedMetaData.getTypeSystem();
      TypePriorities pri = mergedMetaData.getTypePriorities();
      FsIndexCollection indexColl = mergedMetaData.getFsIndexCollection();

      // test results of merge
      // Type System
      assertThat(typeSys.getTypes()).hasSize(8);

      TypeDescription type0 = typeSys.getType("NamedEntity");
      assertThat(type0).isNotNull();
      assertThat(type0.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type0.getFeatures()).hasSize(1);

      TypeDescription type1 = typeSys.getType("Person");
      assertThat(type1).isNotNull();
      assertThat(type1.getSupertypeName()).isEqualTo("NamedEntity");
      assertThat(type1.getFeatures()).hasSize(1);

      TypeDescription type2 = typeSys.getType("Place");
      assertThat(type2).isNotNull();
      assertThat(type2.getSupertypeName()).isEqualTo("NamedEntity");
      assertThat(type2.getFeatures()).hasSize(3);

      TypeDescription type3 = typeSys.getType("Org");
      assertThat(type3).isNotNull();
      assertThat(type3.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type3.getFeatures()).isEmpty();

      TypeDescription type4 = typeSys.getType("DocumentStructure");
      assertThat(type4).isNotNull();
      assertThat(type4.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type4.getFeatures()).isEmpty();

      TypeDescription type5 = typeSys.getType("Paragraph");
      assertThat(type5).isNotNull();
      assertThat(type5.getSupertypeName()).isEqualTo("DocumentStructure");
      assertThat(type5.getFeatures()).isEmpty();

      TypeDescription type6 = typeSys.getType("Sentence");
      assertThat(type6).isNotNull();
      assertThat(type6.getSupertypeName()).isEqualTo("DocumentStructure");
      assertThat(type6.getFeatures()).isEmpty();

      TypeDescription type7 = typeSys.getType("test.flowController.Test");
      assertThat(type7).isNotNull();
      assertThat(type7.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type7.getFeatures()).hasSize(1);

      // Place has merged features, Person has different supertype
      assertEquals(2, mergedTypes.size());
      assertTrue(mergedTypes.containsKey("Place"));
      assertTrue(mergedTypes.containsKey("Person"));

      // Type Priorities
      assertThat(pri).isNotNull();
      TypePriorityList[] priLists = pri.getPriorityLists();
      assertThat(priLists).hasSize(3);
      String[] list0 = priLists[0].getTypes();
      String[] list1 = priLists[1].getTypes();
      String[] list2 = priLists[2].getTypes();
      // order of the three lists is not defined
      assertThat((list0.length == 2 && list1.length == 2 && list2.length == 3)
              || (list0.length == 2 && list1.length == 3 && list2.length == 2)
              || (list0.length == 3 && list1.length == 2 && list2.length == 2)).isTrue();

      // Indexes
      FsIndexDescription[] indexes = indexColl.getFsIndexes();
      assertThat(indexes).hasSize(3);
      // order of indexes is not defined
      String label0 = indexes[0].getLabel();
      String label1 = indexes[1].getLabel();
      String label2 = indexes[2].getLabel();
      assertThat(label0.equals("DocStructIndex") || label1.equals("DocStructIndex")
              || label2.equals("DocStructIndex")).isTrue();
      assertThat(label0.equals("PlaceIndex") || label1.equals("PlaceIndex")
              || label2.equals("PlaceIndex")).isTrue();
      assertThat(
              label0.equals("FlowControllerTestIndex") || label1.equals("FlowControllerTestIndex")
                      || label2.equals("FlowControllerTestIndex")).isTrue();

      // Now test case where aggregate contains a remote, and we want to do the
      // merge of the non-remote delegates and report the failure. (This example
      // also happens to use import-by-name so we need to set the data path.)
      ResourceManager resMgr = UIMAFramework.newDefaultResourceManager();
      String pathSep = System.getProperty("path.separator");
      resMgr.setDataPath(JUnitExtension.getFile("TypeSystemDescriptionImplTest/dataPathDir")
              .getAbsolutePath() + pathSep
              + JUnitExtension.getFile("TypePrioritiesImplTest/dataPathDir").getAbsolutePath()
              + pathSep
              + JUnitExtension.getFile("FsIndexCollectionImplTest/dataPathDir").getAbsolutePath());
    } catch (Exception e) {
      JUnitExtension.handleException(e);
    }
  }
}
