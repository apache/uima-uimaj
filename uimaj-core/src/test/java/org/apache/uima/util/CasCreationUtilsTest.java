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
import java.util.*;

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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.*;

class CasCreationUtilsTest {

  // this test is a skeleton
  // it is currently disabled - it doesn't actually check anything
  // using debug one can see that no errors are thrown if the allowed values for subtypes of string
  // differ when merging
  // and that no merging occurs of the allowed values - the 1st one "wins" (as of 5/2013)
  // See Jira https://issues.apache.org/jira/browse/UIMA-2917
  @Test
  void testStringSubtype() throws Exception {
    var ts1desc = UIMAFramework.getXMLParser()
              .parseTypeSystemDescription(new XMLInputSource(JUnitExtension
                      .getFile("CasCreationUtilsTest/TypeSystemMergeStringSubtypeBasePlus.xml")));

    var result = checkMergeTypeSystem(ts1desc,
              "TypeSystemMergeStringSubtypeBase.xml",
              ResourceInitializationException.ALLOWED_VALUES_NOT_IDENTICAL);
  }

  @Test
  void testMergeTypeSystems() throws Exception {
    var ts1desc = UIMAFramework.getXMLParser().parseTypeSystemDescription(
              new XMLInputSource(JUnitExtension.getFile("CasCreationUtilsTest/TypeSystem1.xml")));

      assertThat(ts1desc.getType("Type1").getFeatures()).hasSize(1);
      assertThat(ts1desc.getType("Type2").getFeatures()).hasSize(1);
      assertThat(ts1desc.getType("Type3").getFeatures()).hasSize(1);

    var ts2desc = UIMAFramework.getXMLParser().parseTypeSystemDescription(
              new XMLInputSource(JUnitExtension.getFile("CasCreationUtilsTest/TypeSystem2.xml")));
      assertThat(ts2desc.getType("Type1").getFeatures()).hasSize(1);
      assertThat(ts2desc.getType("Type2").getFeatures()).hasSize(1);

    var tsList = new ArrayList<TypeSystemDescription>();
      tsList.add(ts1desc);
      tsList.add(ts2desc);
      var typesWithMergedFeatures = new HashMap<String, Set<String>>();
    var merged = CasCreationUtils.mergeTypeSystems(tsList,
              UIMAFramework.newDefaultResourceManager(), typesWithMergedFeatures);

      assertThat(merged.getType("Type1").getFeatures()).hasSize(2);
      assertThat(merged.getType("Type2").getFeatures()).hasSize(2);
      assertThat(merged.getType("Type3").getFeatures()).hasSize(1);

      assertThat(typesWithMergedFeatures.size()).isEqualTo(2);
      assertThat(typesWithMergedFeatures.containsKey("Type1")).isTrue();
      assertThat(typesWithMergedFeatures.containsKey("Type2")).isTrue();

      // make sure one-arg version doesn't fail
      CasCreationUtils.mergeTypeSystems(tsList);
  }

  @Test
  void testMergeTypeSystemElementType() throws Exception {
    var ts1desc = UIMAFramework.getXMLParser()
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
  }

  private TypeSystemDescription checkMergeTypeSystem(TypeSystemDescription ts1desc, String typeFile,
          String msgKey) throws Exception {
    TypeSystemDescription mergedTS = null;

    var ts2desc = UIMAFramework.getXMLParser().parseTypeSystemDescription(
              new XMLInputSource(JUnitExtension.getFile("CasCreationUtilsTest/" + typeFile)));

      List<TypeSystemDescription> tsList = new ArrayList<>();
      tsList.add(ts1desc);
      tsList.add(ts2desc);

    var rightExceptionThrown = (null != msgKey) ? false : true;
      try {
        mergedTS = CasCreationUtils.mergeTypeSystems(tsList,
                UIMAFramework.newDefaultResourceManager(), new HashMap());
      } catch (ResourceInitializationException rie) {
        rightExceptionThrown = (null != msgKey) && rie.hasMessageKey(msgKey);
      }
      assertThat(rightExceptionThrown).isTrue();

    return mergedTS;
  }

  @Test
  void testMergeTypeSystemsWithDifferentSupertypes() throws Exception {
    var ts1desc = UIMAFramework.getXMLParser()
              .parseTypeSystemDescription(new XMLInputSource(
                      JUnitExtension.getFile("CasCreationUtilsTest/SupertypeMergeTest1.xml")));
      assertThat(ts1desc.getType("uima.test.Sub").getSupertypeName()).isEqualTo("uima.tcas.Annotation");
    var ts2desc = UIMAFramework.getXMLParser()
              .parseTypeSystemDescription(new XMLInputSource(
                      JUnitExtension.getFile("CasCreationUtilsTest/SupertypeMergeTest2.xml")));
      assertThat(ts2desc.getType("uima.test.Sub").getSupertypeName()).isEqualTo("uima.test.Super");

      List<TypeSystemDescription> tsList = new ArrayList<>();
      tsList.add(ts1desc);
      tsList.add(ts2desc);
    var merged = CasCreationUtils.mergeTypeSystems(tsList,
              UIMAFramework.newDefaultResourceManager());
      assertThat(merged.getType("uima.test.Sub").getSupertypeName()).isEqualTo("uima.test.Super");

      // try merging in the other order - bug UIMA-826 was an order dependency in the behavior of
      // this kind of merging
      tsList = new ArrayList<>();
      tsList.add(ts2desc);
      tsList.add(ts1desc);
      merged = CasCreationUtils.mergeTypeSystems(tsList, UIMAFramework.newDefaultResourceManager());
      assertThat(merged.getType("uima.test.Sub").getSupertypeName()).isEqualTo("uima.test.Super");
  }

  @Test
  void testAggregateWithImports() throws Exception {
    var pathSep = System.getProperty("path.separator");
    var resMgr = UIMAFramework.newDefaultResourceManager();
      resMgr.setDataPath(JUnitExtension.getFile("TypeSystemDescriptionImplTest/dataPathDir")
              .getAbsolutePath() + pathSep
              + JUnitExtension.getFile("TypePrioritiesImplTest/dataPathDir").getAbsolutePath()
              + pathSep
              + JUnitExtension.getFile("FsIndexCollectionImplTest/dataPathDir").getAbsolutePath());

    var taeDescriptorWithImport = JUnitExtension
              .getFile("CasCreationUtilsTest/AggregateTaeWithImports.xml");
    var desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(taeDescriptorWithImport));
    var mdList = new ArrayList<AnalysisEngineDescription>();
      mdList.add(desc);
    var tcas = CasCreationUtils.createCas(mdList,
              UIMAFramework.getDefaultPerformanceTuningProperties(), resMgr);
      // check that imports were resolved correctly
      assertThat(tcas.getTypeSystem().getType("DocumentStructure")).isNotNull();
      assertThat(tcas.getTypeSystem().getType("NamedEntity")).isNotNull();
      assertThat(tcas.getTypeSystem().getType("TestType3")).isNotNull();
      assertThat(tcas.getTypeSystem().getType("Sentence")).isNotNull();

      assertThat(tcas.getIndexRepository().getIndex("TestIndex")).isNotNull();
      assertThat(tcas.getIndexRepository().getIndex("ReverseAnnotationIndex")).isNotNull();
      assertThat(tcas.getIndexRepository().getIndex("DocumentStructureIndex")).isNotNull();

      // Check elementType and multipleReferencesAllowed for array feature
    var arrayFeat = tcas.getTypeSystem().getFeatureByFullName("Paragraph:sentences");
      assertThat(arrayFeat).isNotNull();
      assertThat(arrayFeat.isMultipleReferencesAllowed()).isFalse();
    var sentenceArrayType = arrayFeat.getRange();
      assertThat(sentenceArrayType).isNotNull();
      assertThat(sentenceArrayType.isArray()).isTrue();
      assertThat(sentenceArrayType.getComponentType()).isEqualTo(tcas.getTypeSystem().getType("Sentence"));

    var arrayFeat2 = tcas.getTypeSystem()
              .getFeatureByFullName("Paragraph:testMultiRefAllowedFeature");
      assertThat(arrayFeat2).isNotNull();
      assertThat(arrayFeat2.isMultipleReferencesAllowed()).isTrue();

      // test imports aren't resolved more than once
      Object spec1 = desc.getDelegateAnalysisEngineSpecifiers().get("Annotator1");
      assertThat(spec1).isNotNull();
      Object spec2 = desc.getDelegateAnalysisEngineSpecifiers().get("Annotator1");
      assertThat(spec1 == spec2).isTrue();

      // test removal
      desc.getDelegateAnalysisEngineSpecifiersWithImports().remove("Annotator1");
      assertThat(desc.getDelegateAnalysisEngineSpecifiers().isEmpty()).isTrue();
  }

  @Test
  void testMergeDelegateAnalysisEngineTypeSystems() throws Exception {
    var descFile = JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml");
    var desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(descFile));
      Map mergedTypes = new HashMap();
    var typeSys = CasCreationUtils.mergeDelegateAnalysisEngineTypeSystems(desc,
              UIMAFramework.newDefaultResourceManager(), mergedTypes);

      // test results of merge
      assertThat(typeSys.getTypes()).hasSize(8);

    var type0 = typeSys.getType("NamedEntity");
      assertThat(type0).isNotNull();
      assertThat(type0.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type0.getFeatures()).hasSize(1);

    var type1 = typeSys.getType("Person");
      assertThat(type1).isNotNull();
      assertThat(type1.getSupertypeName()).isEqualTo("NamedEntity");
      assertThat(type1.getFeatures()).hasSize(1);

    var type2 = typeSys.getType("Place");
      assertThat(type2).isNotNull();
      assertThat(type2.getSupertypeName()).isEqualTo("NamedEntity");
      assertThat(type2.getFeatures()).hasSize(3);

    var type3 = typeSys.getType("Org");
      assertThat(type3).isNotNull();
      assertThat(type3.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type3.getFeatures()).isEmpty();

    var type4 = typeSys.getType("DocumentStructure");
      assertThat(type4).isNotNull();
      assertThat(type4.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type4.getFeatures()).isEmpty();

    var type5 = typeSys.getType("Paragraph");
      assertThat(type5).isNotNull();
      assertThat(type5.getSupertypeName()).isEqualTo("DocumentStructure");
      assertThat(type5.getFeatures()).isEmpty();

    var type6 = typeSys.getType("Sentence");
      assertThat(type6).isNotNull();
      assertThat(type6.getSupertypeName()).isEqualTo("DocumentStructure");
      assertThat(type6.getFeatures()).isEmpty();

    var type7 = typeSys.getType("test.flowController.Test");
      assertThat(type7).isNotNull();
      assertThat(type7.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type7.getFeatures()).hasSize(1);

      // Place has merged features, Person has different supertype
      assertThat(mergedTypes.size()).isEqualTo(2);
      assertThat(mergedTypes.containsKey("Place")).isTrue();
      assertThat(mergedTypes.containsKey("Person")).isTrue();

      // make sure one-arg version doesn't fail
      CasCreationUtils.mergeDelegateAnalysisEngineTypeSystems(desc);
  }

  @Test
  void testMergeDelegateAnalysisEngineTypePriorities() throws Exception {
    var descFile = JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml");
    var desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(descFile));
    var pri = CasCreationUtils.mergeDelegateAnalysisEngineTypePriorities(desc);

      // test results of merge
      assertThat(pri).isNotNull();
    var priLists = pri.getPriorityLists();
      assertThat(priLists).hasSize(3);
    var list0 = priLists[0].getTypes();
    var list1 = priLists[1].getTypes();
    var list2 = priLists[2].getTypes();
      // order of the three lists is not defined
      assertThat((list0.length == 2 && list1.length == 2 && list2.length == 3)
              || (list0.length == 2 && list1.length == 3 && list2.length == 2)
              || (list0.length == 3 && list1.length == 2 && list2.length == 2)).isTrue();
  }

  @Test
  void testMergeDelegateAnalysisEngineFsIndexCollections() throws Exception {
    var descFile = JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml");
    var desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(descFile));
    var indexColl = CasCreationUtils
              .mergeDelegateAnalysisEngineFsIndexCollections(desc);

      // test results of merge
    var indexes = indexColl.getFsIndexes();
      assertThat(indexes).hasSize(3);
      // order of indexes is not defined
    var label0 = indexes[0].getLabel();
    var label1 = indexes[1].getLabel();
    var label2 = indexes[2].getLabel();
      assertThat(label0.equals("DocStructIndex") || label1.equals("DocStructIndex")
              || label2.equals("DocStructIndex")).isTrue();
      assertThat(label0.equals("PlaceIndex") || label1.equals("PlaceIndex")
              || label2.equals("PlaceIndex")).isTrue();
      assertThat(
              label0.equals("FlowControllerTestIndex") || label1.equals("FlowControllerTestIndex")
                      || label2.equals("FlowControllerTestIndex")).isTrue();
  }

  @Test
  void testSetupTypeSystem() throws Exception {
      // test that duplicate feature names on supertype and subtype works
      // regardless of the order in which the types appear in the TypeSystemDescription
      TypeSystemDescription tsd1 = new TypeSystemDescription_impl();
    var supertype = tsd1.addType("test.Super", "", "uima.cas.TOP");
      supertype.addFeature("testfeat", "", "uima.cas.Integer");
    var subtype = tsd1.addType("test.Sub", "", "test.Super");
      subtype.addFeature("testfeat", "", "uima.cas.Integer");

    var casMgr = CASFactory.createCAS();
      CasCreationUtils.setupTypeSystem(casMgr, tsd1);
      assertThat(casMgr.getTypeSystemMgr().getType("test.Super").getFeatureByBaseName("testfeat")).isNotNull();

      TypeSystemDescription tsd2 = new TypeSystemDescription_impl();
      tsd2.setTypes(subtype, supertype);

      casMgr = CASFactory.createCAS();
      CasCreationUtils.setupTypeSystem(casMgr, tsd2);
      assertThat(casMgr.getTypeSystemMgr().getType("test.Super").getFeatureByBaseName("testfeat")).isNotNull();
  }

  @Test
  void testCreateCasCollectionPropertiesResourceManager() throws Exception {
      // parse an AE descriptor
    var taeDescriptorWithImport = JUnitExtension
              .getFile("CasCreationUtilsTest/TaeWithImports.xml");
    var desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(taeDescriptorWithImport));

      // create Resource Manager & set data path - necessary to resolve imports
    var resMgr = UIMAFramework.newDefaultResourceManager();
    var pathSep = System.getProperty("path.separator");
      resMgr.setDataPath(JUnitExtension.getFile("TypeSystemDescriptionImplTest/dataPathDir")
              .getAbsolutePath() + pathSep
              + JUnitExtension.getFile("TypePrioritiesImplTest/dataPathDir").getAbsolutePath()
              + pathSep
              + JUnitExtension.getFile("FsIndexCollectionImplTest/dataPathDir").getAbsolutePath());

      // call method
    var descList = new ArrayList<AnalysisEngineDescription>();
      descList.add(desc);
    var cas = CasCreationUtils.createCas(descList,
              UIMAFramework.getDefaultPerformanceTuningProperties(), resMgr);
      // check that imports were resolved correctly
      assertThat(cas.getTypeSystem().getType("DocumentStructure")).isNotNull();
      assertThat(cas.getTypeSystem().getType("NamedEntity")).isNotNull();
      assertThat(cas.getTypeSystem().getType("TestType3")).isNotNull();

      assertThat(cas.getIndexRepository().getIndex("TestIndex")).isNotNull();
      assertThat(cas.getIndexRepository().getIndex("ReverseAnnotationIndex")).isNotNull();
      assertThat(cas.getIndexRepository().getIndex("DocumentStructureIndex")).isNotNull();

      // check of type priority
    var fs1 = cas.createAnnotation(cas.getTypeSystem().getType("Paragraph"), 0, 1);
    var fs2 = cas.createAnnotation(cas.getTypeSystem().getType("Sentence"), 0, 1);
      assertThat(cas.getAnnotationIndex().compare(fs1, fs2) < 0).isTrue();
  }

  @Test
  void testCreateCasCollection() throws Exception {
      // create two Type System description objects
      TypeSystemDescription tsd1 = new TypeSystemDescription_impl();
    var supertype = tsd1.addType("test.Super", "", "uima.tcas.Annotation");
      supertype.addFeature("testfeat", "", "uima.cas.Integer");
    var subtype = tsd1.addType("test.Sub", "", "test.Super");
      subtype.addFeature("testfeat", "", "uima.cas.Integer");

      TypeSystemDescription tsd2 = new TypeSystemDescription_impl();
    var fooType = tsd1.addType("test.Foo", "", "uima.cas.TOP");
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
    var descList = asList(tsd1, tsd2, indexes, priorities);

    var cas = CasCreationUtils.createCas(descList);

      // check that type system has been installed
    var ts = cas.getTypeSystem();
    var supertypeHandle = ts.getType(supertype.getName());
      assertThat(supertypeHandle).isNotNull();
      assertThat(supertypeHandle.getFeatureByBaseName("testfeat")).isNotNull();
    var subtypeHandle = ts.getType(subtype.getName());
      assertThat(subtypeHandle).isNotNull();
      assertThat(subtypeHandle.getFeatureByBaseName("testfeat")).isNotNull();
    var fooTypeHandle = ts.getType(fooType.getName());
      assertThat(fooTypeHandle).isNotNull();
      assertThat(fooTypeHandle.getFeatureByBaseName("bar")).isNotNull();

      // check that index exists
      assertThat(cas.getIndexRepository().getIndex("MyIndex")).isNotNull();

      // test that priorities work
      cas.createFS(supertypeHandle);
      cas.createFS(subtypeHandle);
      FSIterator iter = cas.getAnnotationIndex().iterator();
      while (iter.isValid()) {
        if (iter.get().getType() == subtypeHandle) {
          break;
        }

        assertThat(iter.get().getType()).isNotSameAs(supertypeHandle);

        iter.moveToNext();
      }

      // test that passing an invalid object causes an error
      descList.add(new ConfigurationParameter_impl());
      assertThatExceptionOfType(ResourceInitializationException.class).isThrownBy(() -> CasCreationUtils.createCas(descList));
  }

  @Test
  void testCreateCasTypeSystemDescription() throws Exception {
      // parse type system description
    var tsDesc = UIMAFramework.getXMLParser()
              .parseTypeSystemDescription(new XMLInputSource(
                      JUnitExtension.getFile("CasCreationUtilsTest/SupertypeMergeTestMaster.xml")));

      // call method
    var cas = CasCreationUtils.createCas(tsDesc, null, null);

      // check that imports were resolved and supertype merged properly
    var subType = cas.getTypeSystem().getType("uima.test.Sub");
      assertThat(subType).isNotNull();
    var superType = cas.getTypeSystem().getType("uima.test.Super");
      assertThat(superType).isNotNull();
      assertThat(cas.getTypeSystem().subsumes(superType, subType)).isTrue();
  }

  @Test
  void testMergeDelegateAnalysisEngineMetaData() throws Exception {
    var descFile = JUnitExtension
              .getFile("TextAnalysisEngineImplTest/AggregateTaeForMergeTest.xml");
    var desc = UIMAFramework.getXMLParser()
              .parseAnalysisEngineDescription(new XMLInputSource(descFile));
      Map mergedTypes = new HashMap<String, Set<String>>();
    var mergedMetaData = CasCreationUtils
              .mergeDelegateAnalysisEngineMetaData(desc, UIMAFramework.newDefaultResourceManager(),
                      mergedTypes, null);
    var typeSys = mergedMetaData.getTypeSystem();
    var pri = mergedMetaData.getTypePriorities();
    var indexColl = mergedMetaData.getFsIndexCollection();

      // test results of merge
      // Type System
      assertThat(typeSys.getTypes()).hasSize(8);

    var type0 = typeSys.getType("NamedEntity");
      assertThat(type0).isNotNull();
      assertThat(type0.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type0.getFeatures()).hasSize(1);

    var type1 = typeSys.getType("Person");
      assertThat(type1).isNotNull();
      assertThat(type1.getSupertypeName()).isEqualTo("NamedEntity");
      assertThat(type1.getFeatures()).hasSize(1);

    var type2 = typeSys.getType("Place");
      assertThat(type2).isNotNull();
      assertThat(type2.getSupertypeName()).isEqualTo("NamedEntity");
      assertThat(type2.getFeatures()).hasSize(3);

    var type3 = typeSys.getType("Org");
      assertThat(type3).isNotNull();
      assertThat(type3.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type3.getFeatures()).isEmpty();

    var type4 = typeSys.getType("DocumentStructure");
      assertThat(type4).isNotNull();
      assertThat(type4.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type4.getFeatures()).isEmpty();

    var type5 = typeSys.getType("Paragraph");
      assertThat(type5).isNotNull();
      assertThat(type5.getSupertypeName()).isEqualTo("DocumentStructure");
      assertThat(type5.getFeatures()).isEmpty();

    var type6 = typeSys.getType("Sentence");
      assertThat(type6).isNotNull();
      assertThat(type6.getSupertypeName()).isEqualTo("DocumentStructure");
      assertThat(type6.getFeatures()).isEmpty();

    var type7 = typeSys.getType("test.flowController.Test");
      assertThat(type7).isNotNull();
      assertThat(type7.getSupertypeName()).isEqualTo("uima.tcas.Annotation");
      assertThat(type7.getFeatures()).hasSize(1);

      // Place has merged features, Person has different supertype
      assertThat(mergedTypes).hasSize(2);
      assertThat(mergedTypes).containsKey("Place");
      assertThat(mergedTypes).containsKey("Person");

      // Type Priorities
      assertThat(pri).isNotNull();
    var priLists = pri.getPriorityLists();
      assertThat(priLists).hasSize(3);
    var list0 = priLists[0].getTypes();
    var list1 = priLists[1].getTypes();
    var list2 = priLists[2].getTypes();
      // order of the three lists is not defined
      assertThat((list0.length == 2 && list1.length == 2 && list2.length == 3)
              || (list0.length == 2 && list1.length == 3 && list2.length == 2)
              || (list0.length == 3 && list1.length == 2 && list2.length == 2)).isTrue();

      // Indexes
    var indexes = indexColl.getFsIndexes();
      assertThat(indexes).hasSize(3);
      // order of indexes is not defined
    var label0 = indexes[0].getLabel();
    var label1 = indexes[1].getLabel();
    var label2 = indexes[2].getLabel();
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
    var resMgr = UIMAFramework.newDefaultResourceManager();
    var pathSep = System.getProperty("path.separator");
      resMgr.setDataPath(JUnitExtension.getFile("TypeSystemDescriptionImplTest/dataPathDir")
              .getAbsolutePath() + pathSep
              + JUnitExtension.getFile("TypePrioritiesImplTest/dataPathDir").getAbsolutePath()
              + pathSep
              + JUnitExtension.getFile("FsIndexCollectionImplTest/dataPathDir").getAbsolutePath());
  }
}
