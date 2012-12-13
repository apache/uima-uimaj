/*
 Copyright 2011
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
/*
 Copyright 2011
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

import static org.apache.uima.fit.factory.CollectionReaderFactory.createCollectionReader;
import static org.apache.uima.fit.factory.ExternalResourceFactory.createExternalResourceDescription;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.uima.UIMAException;
import org.apache.uima.cas.CAS;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.collection.CollectionReader;
import org.apache.uima.fit.descriptor.ExternalResource;
import org.apache.uima.util.Progress;
import org.junit.Test;
import org.uimafit.component.CasCollectionReader_ImplBase;
import org.uimafit.factory.testRes.TestExternalResource;

/**
 * @author Shuo Yang
 * @author Richard Eckart de Castilho
 */
public class CollectionReaderFactoryExternalResourceTest {
	@Test
	public void testAutoExternalResourceBinding() throws UIMAException, IOException {
		CollectionReader reader = createCollectionReader(
				TestReader.class,
				TestReader.PARAM_RESOURCE,
				createExternalResourceDescription(TestExternalResource.class,
						TestExternalResource.PARAM_VALUE, TestExternalResource.EXPECTED_VALUE));

		reader.hasNext();
	}

	public static class TestReader extends CasCollectionReader_ImplBase {
		public final static String PARAM_RESOURCE = "resource";
		@ExternalResource(key = PARAM_RESOURCE)
		private TestExternalResource resource;

		public boolean hasNext() throws IOException, CollectionException {
			assertNotNull(resource);
			resource.assertConfiguredOk();
			return false;
		}
		
		public void getNext(CAS aCAS) throws IOException, CollectionException {
			// This is never called
		}
		
		public Progress[] getProgress() {
			return new Progress[0];
		}
	}
}
