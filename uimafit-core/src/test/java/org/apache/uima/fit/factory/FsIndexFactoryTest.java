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
package org.apache.uima.fit.factory;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createEngine;
import static org.apache.uima.fit.factory.FsIndexFactory.createFsIndexCollection;
import static org.apache.uima.fit.factory.FsIndexFactory.loadFsIndexCollectionsFromScannedLocations;
import static org.apache.uima.fit.factory.FsIndexFactory.loadFsIndexCollectionsfromSPIs;
import static org.apache.uima.resource.metadata.FsIndexDescription.KIND_SORTED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.component.JCasAnnotator_ImplBase;
import org.apache.uima.fit.descriptor.FsIndex;
import org.apache.uima.fit.descriptor.FsIndexCollection;
import org.apache.uima.fit.descriptor.FsIndexKey;
import org.apache.uima.fit.factory.spi.FsIndexCollectionProviderForTesting;
import org.apache.uima.fit.internal.ResourceManagerFactory;
import org.apache.uima.fit.type.Sentence;
import org.apache.uima.fit.type.Token;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.junit.jupiter.api.Test;

public class FsIndexFactoryTest extends ComponentTestBase {
  @Test
  public void testCreateIndexCollection() throws Exception {
    org.apache.uima.resource.metadata.FsIndexCollection fsIndexCollection = createFsIndexCollection(
            IndexTestComponent.class);

    assertEquals(2, fsIndexCollection.getFsIndexes().length);

    FsIndexDescription index1 = fsIndexCollection.getFsIndexes()[0];
    assertEquals("index1", index1.getLabel());
    assertEquals(Token.class.getName(), index1.getTypeName());
    assertEquals(FsIndexDescription.KIND_SORTED, index1.getKind());

    FsIndexKeyDescription key11 = index1.getKeys()[0];
    assertEquals("begin", key11.getFeatureName());
    assertEquals(FsIndexKeyDescription.REVERSE_STANDARD_COMPARE, key11.getComparator());

    FsIndexKeyDescription key12 = index1.getKeys()[1];
    assertEquals("end", key12.getFeatureName());
    assertEquals(FsIndexKeyDescription.STANDARD_COMPARE, key12.getComparator());

    FsIndexDescription index2 = fsIndexCollection.getFsIndexes()[1];
    assertEquals("index2", index2.getLabel());
    assertEquals(Sentence.class.getName(), index2.getTypeName());
    assertEquals(FsIndexDescription.KIND_SET, index2.getKind());

    FsIndexKeyDescription key21 = index2.getKeys()[0];
    assertEquals("begin", key21.getFeatureName());
    assertEquals(FsIndexKeyDescription.STANDARD_COMPARE, key21.getComparator());

    fsIndexCollection.toXML(new FileOutputStream("target/dummy.xml"));
  }

  @Test
  public void testIndexesWork() throws Exception {
    // Index should be added the descriptor and thus end up in the CAS generated from the
    // analysis engine.
    AnalysisEngine desc = createEngine(IndexTestComponent.class);
    JCas jcas = desc.newJCas();

    Token token1 = new Token(jcas, 1, 2);
    token1.addToIndexes();

    // index1 is a sorted index, so when adding a token twice, both remain in the index
    // Since UIMA 2.7.0, annotations are not added twice to the index by default.
    Token token2 = new Token(jcas, 3, 4);
    token2.addToIndexes();
    token2.addToIndexes();

    Sentence sentence1 = new Sentence(jcas, 1, 2);
    sentence1.addToIndexes();

    // index2 is a set index, so even when adding a sentence twice, only one remains in the index
    Sentence sentence2 = new Sentence(jcas, 3, 4);
    sentence2.addToIndexes();
    sentence2.addToIndexes();

    FSIndex<FeatureStructure> index1 = jcas.getFSIndexRepository().getIndex("index1");
    FSIndex<FeatureStructure> index2 = jcas.getFSIndexRepository().getIndex("index2");

    assertEquals(2, index1.size());
    assertEquals(2, index2.size());

    // AnalysisEngine dumpWriter = createPrimitive(CASDumpWriter.class);
    // dumpWriter.process(jcas.getCas());
  }

  @FsIndexCollection(fsIndexes = {
      @FsIndex(label = "index1", type = Token.class, kind = FsIndex.KIND_SORTED, keys = {
          @FsIndexKey(featureName = "begin", comparator = FsIndexKey.REVERSE_STANDARD_COMPARE),
          @FsIndexKey(featureName = "end", comparator = FsIndexKey.STANDARD_COMPARE) }),
      @FsIndex(label = "index2", type = Sentence.class, kind = FsIndex.KIND_SET, keys = {
          @FsIndexKey(featureName = "begin", comparator = FsIndexKey.STANDARD_COMPARE) }) })
  public static class IndexTestComponent extends JCasAnnotator_ImplBase {
    @Override
    public void process(JCas aJCas) throws AnalysisEngineProcessException {
      // Nothing to do
    }
  }

  @Test
  public void testAutoDetectIndexes() throws Exception {
    org.apache.uima.resource.metadata.FsIndexCollection fsIndexCollection = createFsIndexCollection();

    assertThat(fsIndexCollection.getFsIndexes()).hasSize(2);

    assertThat(fsIndexCollection.getFsIndexes()) //
            .extracting( //
                    FsIndexDescription::getLabel, //
                    FsIndexDescription::getTypeName, //
                    FsIndexDescription::getKind) //
            .containsExactlyInAnyOrder( //
                    tuple( //
                            "Automatically Scanned Index", //
                            Token.class.getName(), //
                            KIND_SORTED),
                    tuple( //
                            FsIndexCollectionProviderForTesting.INDEX_LABEL, //
                            FsIndexCollectionProviderForTesting.INDEX_TYPE, //
                            KIND_SORTED));
  }

  @Test
  public void testLoadingFromScannedLocations() throws Exception {
    ResourceManager resMgr = ResourceManagerFactory.newResourceManager();

    List<FsIndexDescription> indexes = new ArrayList<>();
    loadFsIndexCollectionsFromScannedLocations(indexes, resMgr);
    org.apache.uima.resource.metadata.FsIndexCollection fsIndexCollection = FsIndexFactory
            .createFsIndexCollection(indexes);

    FsIndexDescription index = fsIndexCollection.getFsIndexes()[0];
    assertEquals("Automatically Scanned Index", index.getLabel());
    assertEquals(Token.class.getName(), index.getTypeName());
    assertEquals(FsIndexDescription.KIND_SORTED, index.getKind());
  }

  @Test
  public void testLoadingFromSPIs() throws Exception {
    List<FsIndexDescription> indexes = new ArrayList<>();
    loadFsIndexCollectionsfromSPIs(indexes);
    org.apache.uima.resource.metadata.FsIndexCollection fsIndexCollection = FsIndexFactory
            .createFsIndexCollection(indexes);

    FsIndexDescription index = fsIndexCollection.getFsIndexes()[0];
    assertEquals(FsIndexCollectionProviderForTesting.INDEX_LABEL, index.getLabel());
    assertEquals(FsIndexCollectionProviderForTesting.INDEX_TYPE, index.getTypeName());
    assertEquals(FsIndexDescription.KIND_SORTED, index.getKind());
  }
}
