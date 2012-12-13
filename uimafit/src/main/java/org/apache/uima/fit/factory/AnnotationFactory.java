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

package org.apache.uima.fit.factory;

import org.apache.uima.UIMAException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.tcas.Annotation;

/**
 * 
 * @author Steven Bethard, Philip Ogren
 * 
 */
public final class AnnotationFactory {
	private AnnotationFactory() {
		// This class is not meant to be instantiated
	}

	/**
	 * Provides a convenient way to create an annotation and addToIndexes in a single line.
	 */
	public static <T extends Annotation> T createAnnotation(JCas jCas, int begin, int end,
			Class<T> cls) throws UIMAException {
		T annotation;
		try {
			annotation = cls.getConstructor(JCas.class, Integer.TYPE, Integer.TYPE).newInstance(
					jCas, begin, end);
		}
		catch (Exception e) {
			throw new UIMAException(e);
		}
		annotation.addToIndexes();
		return annotation;
	}

}
