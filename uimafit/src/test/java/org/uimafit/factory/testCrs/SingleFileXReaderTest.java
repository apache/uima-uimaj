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
package org.uimafit.factory.testCrs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.fit.pipeline.JCasIterable;
import org.apache.uima.fit.testing.util.HideOutput;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.junit.Test;

/**
 * @author Steven Bethard, Philip Ogren
 */

public class SingleFileXReaderTest extends ComponentTestBase {

	@Test
	public void testXReader() throws UIMAException, IOException {
		ResourceInitializationException rie = null;
		try {
			CollectionReaderFactory.createCollectionReader(SingleFileXReader.class, null,
					SingleFileXReader.PARAM_XML_SCHEME, "XML");
		}
		catch (ResourceInitializationException e) {
			rie = e;
		}
		assertNotNull(rie);

		rie = null;
		try {
			CollectionReaderFactory.createCollectionReader(SingleFileXReader.class, null,
					SingleFileXReader.PARAM_XML_SCHEME, "XML", SingleFileXReader.PARAM_FILE_NAME,
					"myxslt.xml");
		}
		catch (ResourceInitializationException e) {
			rie = e;
		}
		assertNotNull(rie);

		CollectionReader cr = CollectionReaderFactory
				.createCollectionReader(SingleFileXReader.class, typeSystemDescription,
						SingleFileXReader.PARAM_XML_SCHEME, "XCAS",
						SingleFileXReader.PARAM_FILE_NAME, "src/test/resources/data/docs/test.xcas");
		Progress[] progress = cr.getProgress();
		assertEquals(1, progress.length);
		assertEquals(0, progress[0].getCompleted());
		assertTrue(cr.hasNext());

		new JCasIterable(cr).next();
		progress = cr.getProgress();
		assertEquals(1, progress.length);
		assertEquals(1, progress[0].getCompleted());

		cr.close();

		cr = CollectionReaderFactory.createCollectionReader(SingleFileXReader.class,
				typeSystemDescription, SingleFileXReader.PARAM_XML_SCHEME, "XCAS",
				SingleFileXReader.PARAM_FILE_NAME, "test/data/docs/test.xcas");
		UnsupportedOperationException uoe = null;
		try {
			new JCasIterable(cr).iterator().remove();
		}
		catch (UnsupportedOperationException e) {
			uoe = e;
		}
		assertNotNull(uoe);
		cr.close();

		HideOutput hideOutput = new HideOutput();
		cr = CollectionReaderFactory.createCollectionReader(SingleFileXReader.class,
				typeSystemDescription, SingleFileXReader.PARAM_XML_SCHEME, "XCAS",
				SingleFileXReader.PARAM_FILE_NAME, "test/data/docs/bad.xcas");
		RuntimeException re = null;
		try {
			new JCasIterable(cr).next();
		}
		catch (RuntimeException e) {
			re = e;
		}
		assertNotNull(re);
		hideOutput.restoreOutput();

		cr = CollectionReaderFactory.createCollectionReader(SingleFileXReader.class,
				typeSystemDescription, SingleFileXReader.PARAM_XML_SCHEME, "XMI",
				SingleFileXReader.PARAM_FILE_NAME, "test/data/docs/dne.xmi");
		re = null;
		try {
			JCasIterable jCases = new JCasIterable(cr);
			assertTrue(jCases.hasNext());
			jCases.next();
		}
		catch (RuntimeException e) {
			re = e;
		}
		assertNotNull(re);

	}
}
