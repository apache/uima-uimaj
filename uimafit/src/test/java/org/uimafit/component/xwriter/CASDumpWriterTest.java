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
package org.uimafit.component.xwriter;

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.jcas.JCas;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.JCasFactory;

/**
 * @author Richard Eckart de Castilho
 */
public class CASDumpWriterTest {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void testXWriter() throws Exception {
		File outputFile = new File(folder.getRoot(), "dump-output.txt");

		AnalysisEngine writer = AnalysisEngineFactory.createPrimitive(CASDumpWriter.class,
				CASDumpWriter.PARAM_OUTPUT_FILE, outputFile.getPath());
		JCas jcas = writer.newJCas();
		JCasFactory.loadJCas(jcas, "src/test/resources/data/docs/test.xmi");
		writer.process(jcas);
		assertTrue(outputFile.exists());

		String reference = readFileToString(
				new File("src/test/resources/data/reference/test.xmi.dump"), "UTF-8").trim();
		String actual = readFileToString(outputFile, "UTF-8").trim();
		actual = actual.replaceAll("\r\n", "\n");

		assertEquals(reference, actual);
	}
}
