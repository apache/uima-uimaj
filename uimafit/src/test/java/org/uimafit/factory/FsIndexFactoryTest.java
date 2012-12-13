/*
 Copyright 2010
 Ubiquitous Knowledge Processing (UKP) Lab
 Technische Universitaet Darmstadt
 All rights reserved.

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
package org.uimafit.factory;

import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitive;
import static org.apache.uima.fit.factory.FsIndexFactory.createFsIndexCollection;
import static org.junit.Assert.assertEquals;

import java.io.FileOutputStream;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.descriptor.FsIndex;
import org.apache.uima.fit.descriptor.FsIndexCollection;
import org.apache.uima.fit.descriptor.FsIndexKey;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.FsIndexDescription;
import org.apache.uima.resource.metadata.FsIndexKeyDescription;
import org.junit.Test;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.type.Sentence;
import org.uimafit.type.Token;

/**
 * @author Richard Eckart de Castilho
 */
public class FsIndexFactoryTest extends ComponentTestBase {
	@Test
	public void testCreateIndexCollection() throws Exception {
		org.apache.uima.resource.metadata.FsIndexCollection fsIndexCollection =
			createFsIndexCollection(IndexTestComponent.class);

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
		AnalysisEngine desc = createPrimitive(IndexTestComponent.class);
		JCas jcas = desc.newJCas();

		Token token1 = new Token(jcas, 1, 2);
		token1.addToIndexes();

		// index1 is a sorted index, so when adding a token twice, both remain in the index
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

		assertEquals(3, index1.size());
		assertEquals(2, index2.size());

//		AnalysisEngine dumpWriter = createPrimitive(CASDumpWriter.class);
//		dumpWriter.process(jcas.getCas());
	}

	@FsIndexCollection(fsIndexes = {
			@FsIndex(label="index1", type=Token.class, kind=FsIndex.KIND_SORTED, keys = {
				@FsIndexKey(featureName="begin", comparator=FsIndexKey.REVERSE_STANDARD_COMPARE),
				@FsIndexKey(featureName="end", comparator=FsIndexKey.STANDARD_COMPARE)
			}),
			@FsIndex(label="index2", type=Sentence.class, kind=FsIndex.KIND_SET, keys = {
				@FsIndexKey(featureName="begin", comparator=FsIndexKey.STANDARD_COMPARE)
			})
	})
	public static class IndexTestComponent extends JCasAnnotator_ImplBase
	{
		@Override
		public void process(JCas aJCas) throws AnalysisEngineProcessException {
			// Nothing to do
		}
	}

	@Test
	public void testAutoDetectIndexes() throws Exception {
		org.apache.uima.resource.metadata.FsIndexCollection fsIndexCollection = createFsIndexCollection();

		FsIndexDescription index1 = fsIndexCollection.getFsIndexes()[0];
		assertEquals("Automatically Scanned Index", index1.getLabel());
		assertEquals(Token.class.getName(), index1.getTypeName());
		assertEquals(FsIndexDescription.KIND_SORTED, index1.getKind());
	}
}
