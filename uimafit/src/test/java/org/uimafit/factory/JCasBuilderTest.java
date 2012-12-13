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

import static org.apache.commons.io.FileUtils.readFileToString;
import static org.apache.uima.fit.factory.AnalysisEngineFactory.createPrimitive;
import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.factory.JCasBuilder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.uimafit.component.xwriter.CASDumpWriter;
import org.uimafit.type.Sentence;
import org.uimafit.type.Token;

/**
 * @author Richard Eckart de Castilho
 */
public class JCasBuilderTest extends ComponentTestBase {
	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	@Test
	public void test() throws Exception {
		JCasBuilder jb = new JCasBuilder(jCas);
		jb.add("This sentence is not annotated. ");
		jb.add("But this sentences is annotated. ", Sentence.class);
		int begin = jb.getPosition();
		jb.add("And", Token.class);
		jb.add(" ");
		jb.add("here", Token.class);
		jb.add(" ");
		jb.add("every", Token.class);
		jb.add(" ");
		jb.add("token", Token.class);
		jb.add(" ");
		jb.add("is", Token.class);
		jb.add(".", Token.class);
		jb.add(begin, Sentence.class);
		jb.close();

		File outputFile = new File(folder.getRoot(), "dump-output.txt");
		AnalysisEngine writer = createPrimitive(CASDumpWriter.class,
				CASDumpWriter.PARAM_OUTPUT_FILE, outputFile.getPath());
		writer.process(jb.getJCas());

		String reference = readFileToString(
				new File("src/test/resources/data/reference/JCasBuilderTest.dump"), "UTF-8").trim();
		String actual = readFileToString(outputFile, "UTF-8").trim();
		actual = actual.replaceAll("\r\n", "\n");

		assertEquals(reference, actual);
	}
}
