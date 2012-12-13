/*
 Copyright 2012
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
package org.uimafit.pipeline;

import static org.uimafit.pipeline.SimplePipelineTest.SENTENCE_TEXT;

import java.util.Arrays;

import junit.framework.Assert;

import org.junit.Test;
import org.uimafit.factory.AnalysisEngineFactory;
import org.uimafit.factory.CollectionReaderFactory;
import org.uimafit.pipeline.SimplePipelineTest.Annotator;
import org.uimafit.pipeline.SimplePipelineTest.Reader;
import org.uimafit.pipeline.SimplePipelineTest.Writer;

/**
 * @author Richard Eckart de Castilho
 */
public class CpePipelineTest {
	@Test
	public void test() throws Exception {
		CpePipeline.runPipeline(
				CollectionReaderFactory.createDescription(Reader.class),
				AnalysisEngineFactory.createPrimitiveDescription(Annotator.class),
				AnalysisEngineFactory.createPrimitiveDescription(Writer.class));
		Assert.assertEquals(Arrays.asList(SENTENCE_TEXT), Writer.SENTENCES);
	}
}
