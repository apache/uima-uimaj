/* 
 Copyright 2009-2010	Regents of the University of Colorado.  
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

package org.apache.uima.fit.pipeline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.uima.UIMAException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.uimafit.component.NoOpAnnotator;

/**
 * A class implementing iteration over a the documents of a collection. Each element in the Iterable
 * is a JCas containing a single document. The documents have been loaded by the CollectionReader
 * and processed by the AnalysisEngine (if any).
 * 
 * @author Steven Bethard, Philip Ogren
 */
public class JCasIterable implements Iterator<JCas>, Iterable<JCas> {

	private CollectionReader collectionReader;

	private AnalysisEngine[] analysisEngines;

	private JCas jCas;

	/**
	 * Iterate over the documents loaded by the CollectionReader. (Uses an JCasAnnotatorAdapter to
	 * create the document JCas.)
	 * 
	 * @param aReader
	 *            The CollectionReader for loading documents.
	 * @param aTypeSystemDescription
	 *            a type system description
	 */
	public JCasIterable(final CollectionReader aReader,
			final TypeSystemDescription aTypeSystemDescription) throws UIMAException, IOException {
		this(aReader, AnalysisEngineFactory.createPrimitive(NoOpAnnotator.class,
				aTypeSystemDescription));
	}

	/**
	 * Iterate over the documents loaded by the CollectionReader, running the AnalysisEngine on each
	 * one before yielding them.
	 * 
	 * @param aReader
	 *            The CollectionReader for loading documents.
	 * @param aEngines
	 *            The AnalysisEngines for processing documents.
	 */
	public JCasIterable(final CollectionReader aReader, final AnalysisEngine... aEngines)
			throws UIMAException, IOException {
		this.collectionReader = aReader;
		this.analysisEngines = aEngines;
		final List<ResourceMetaData> metaData = new ArrayList<ResourceMetaData>();
		metaData.add(aReader.getMetaData());
		for (AnalysisEngine engine : aEngines) {
			metaData.add(engine.getMetaData());
		}
		this.jCas = CasCreationUtils.createCas(metaData).getJCas();
	}

	public Iterator<JCas> iterator() {
		return this;
	}

	public boolean hasNext() {
		try {
			return this.collectionReader.hasNext();
		}
		catch (CollectionException e) {
			throw new IllegalStateException(e);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
	}

	public JCas next() {
		this.jCas.reset();
		try {
			this.collectionReader.getNext(this.jCas.getCas());
			for (AnalysisEngine engine : this.analysisEngines) {
				engine.process(this.jCas);
			}
		}
		catch (CollectionException e) {
			throw new IllegalStateException(e);
		}
		catch (IOException e) {
			throw new IllegalStateException(e);
		}
		catch (AnalysisEngineProcessException e) {
			throw new IllegalStateException(e);
		}
		return this.jCas;
	}

	public void remove() {
		throw new UnsupportedOperationException();
	}
}
