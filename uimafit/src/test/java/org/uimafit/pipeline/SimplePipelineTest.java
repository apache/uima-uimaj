/*
 Copyright 2011	Regents of the University of Colorado.
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
package org.uimafit.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.Assert;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.junit.Test;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.component.JCasCollectionReader_ImplBase;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.type.Sentence;
import org.uimafit.util.JCasUtil;

/**
 * @author Steven Bethard
 */
public class SimplePipelineTest {

	public static final String SENTENCE_TEXT = "Some text";

	public static class Reader extends JCasCollectionReader_ImplBase {

		private int size = 1;
		private int current = 0;

		public Progress[] getProgress() {
			return null;
		}

		public boolean hasNext() throws IOException, CollectionException {
			return this.current < this.size;
		}

		@Override
		public void getNext(JCas jCas) throws IOException, CollectionException {
			jCas.setDocumentText(SENTENCE_TEXT);
			this.current += 1;
		}

	}

	public static class Annotator extends JCasAnnotator_ImplBase {

		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			String text = jCas.getDocumentText();
			Sentence sentence = new Sentence(jCas, 0, text.length());
			sentence.addToIndexes();
		}

	}

	public static class Writer extends JCasAnnotator_ImplBase {

		public static List<String> SENTENCES = new ArrayList<String>();

		@Override
		public void initialize(UimaContext context) throws ResourceInitializationException {
			super.initialize(context);
			SENTENCES = new ArrayList<String>();
		}
		
		@Override
		public void process(JCas jCas) throws AnalysisEngineProcessException {
			for (Sentence sentence : JCasUtil.select(jCas, Sentence.class)) {
				SENTENCES.add(sentence.getCoveredText());
			}
		}

	}

	@Test
	public void test() throws Exception {
		SimplePipeline.runPipeline(CollectionReaderFactory.createCollectionReader(Reader.class),
				AnalysisEngineFactory.createPrimitive(Annotator.class),
				AnalysisEngineFactory.createPrimitive(Writer.class));
		Assert.assertEquals(Arrays.asList(SENTENCE_TEXT), Writer.SENTENCES);
	}
}
