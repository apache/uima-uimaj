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
package org.apache.uima.fit.examples.tutorial.ex1;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.examples.tutorial.ExamplesTestBase;
import org.apache.uima.fit.examples.tutorial.ex1.RoomNumberAnnotator;
import org.apache.uima.fit.examples.tutorial.type.RoomNumber;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.util.JCasUtil;
import org.junit.Test;

/**
 * This class demonstrates some simple tests using uimaFIT using the
 * ExamplesTestBase. These tests have the advantage that a new JCas is not
 * created for each test.
 * 
 * 
 */
public class RoomNumberAnnotator2Test extends ExamplesTestBase {

	/**
	 * This test is nice because the super classes provides the
	 * typeSystemDescription and jCas objects.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testRNA1() throws Exception {
		AnalysisEngine roomNumberAnnotatorAE = AnalysisEngineFactory.createPrimitive(RoomNumberAnnotator.class, typeSystemDescription);
		jCas.setDocumentText("The meeting is over at Yorktown 01-144");
		roomNumberAnnotatorAE.process(jCas);

		RoomNumber roomNumber = JCasUtil.selectByIndex(jCas, RoomNumber.class, 0);
		assertNotNull(roomNumber);
		assertEquals("01-144", roomNumber.getCoveredText());
		assertEquals("Yorktown", roomNumber.getBuilding());
	}

}
