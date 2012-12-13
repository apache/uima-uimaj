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

import static org.uimafit.util.JCasUtil.getType;

import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;

/**
 * Allows to add types and text to a CAS easily piece by piece.
 * 
 * @author Richard Eckart de Castilho
 */
public class JCasBuilder {
	private final StringBuilder documentText = new StringBuilder();
	private final JCas jcas;

	/**
	 * Create a new JCas builder working on the specified JCas. The JCas must not have any content
	 * yet.
	 * 
	 * @param aJCas
	 *            the working JCas.
	 */
	public JCasBuilder(JCas aJCas) {
		jcas = aJCas;
	}

	/**
	 * Append a text.
	 * 
	 * @param aText
	 *            the text to append.
	 */
	public void add(String aText) {
		documentText.append(aText);
	}

	/**
	 * Append a text annotated with the specified annotation. The created annotation is returned and
	 * further properties can be set on it. The annotation is already added to the indexes.
	 * 
	 * @param aText
	 *            covered text
	 * @param aClass
	 *            annotation type
	 * @param <T>
	 *            annotation type
	 * @return annotation instance - can be used to set features or determine offsets
	 */
	@SuppressWarnings("unchecked")
	public <T> T add(String aText, Class<T> aClass) {
		Type type = getType(jcas, aClass);
		int begin = documentText.length();
		add(aText);
		int end = documentText.length();
		AnnotationFS fs = jcas.getCas().createAnnotation(type, begin, end);
		jcas.addFsToIndexes(fs);
		return (T) fs;
	}

	/**
	 * Add an annotation starting at the specified position and ending at the current end of the
	 * text. The created annotation is returned and further properties can be set on it. The
	 * annotation is already added to the indexes.
	 * 
	 * @param <T>
	 *            annotation type
	 * @param aBegin
	 *            begin offset.
	 * @param aClass
	 *            annotation type
	 * @return annotation instance - can be used to set features or determine offsets
	 */
	@SuppressWarnings("unchecked")
	public <T> T add(int aBegin, Class<T> aClass) {
		Type type = getType(jcas, aClass);
		int end = documentText.length();
		AnnotationFS fs = jcas.getCas().createAnnotation(type, aBegin, end);
		jcas.addFsToIndexes(fs);
		return (T) fs;
	}

	/**
	 * Get the current "cursor" position (current text length).
	 * 
	 * @return current text length.
	 */
	public int getPosition() {
		return documentText.length();
	}

	/**
	 * Get the JCas.
	 * 
	 * @return the JCas.
	 */
	public JCas getJCas() {
		return jcas;
	}

	/**
	 * Complete the building process by writing the text into the CAS. This can only be called once.
	 */
	public void close() {
		jcas.setDocumentText(documentText.toString());
	}
}
