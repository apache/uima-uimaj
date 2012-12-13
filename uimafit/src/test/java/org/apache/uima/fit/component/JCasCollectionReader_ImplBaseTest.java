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
package org.apache.uima.fit.component;

import static org.apache.uima.fit.factory.CollectionReaderFactory.createCollectionReader;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.Progress;
import org.junit.Test;
import org.uimafit.component.JCasCollectionReader_ImplBase;

/**
 * Test {@link JCasCollectionReader_ImplBase}
 * 
 * @author Richard Eckart de Castilho
 */
public class JCasCollectionReader_ImplBaseTest {
	/**
	 * Test using a simple reader reading one text file.
	 * 
	 * @throws Exception
	 *             if an error occurs.
	 */
	@Test
	public void test() throws Exception {
		File file = new File("src/test/resources/data/docs/A.txt");

		CollectionReader reader = createCollectionReader(SingleTextReader.class,
				SingleTextReader.PARAM_FILE, file.getPath());

		CAS cas = CasCreationUtils.createCas(reader.getProcessingResourceMetaData());
		reader.getNext(cas);
		reader.close();

		assertEquals(FileUtils.readFileToString(file, "UTF-8"), cas.getDocumentText());
	}

	public static class SingleTextReader extends JCasCollectionReader_ImplBase {
		public static final String PARAM_FILE = "File";
		@ConfigurationParameter(name = PARAM_FILE, mandatory = true)
		private File file;

		public boolean hasNext() throws IOException, CollectionException {
			return file != null;
		}

		public Progress[] getProgress() {
			return new Progress[0];
		}

		@Override
		public void getNext(JCas jCas) throws IOException, CollectionException {
			jCas.setDocumentText(FileUtils.readFileToString(file, "UTF-8"));
		}
	}
}
