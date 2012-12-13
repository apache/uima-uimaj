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
package org.apache.uima.fit.util;

/**
 * @author Philip Ogren
 */

import java.util.Iterator;

import org.apache.uima.cas.CASException;
import org.apache.uima.fit.ComponentTestBase;
import org.junit.Test;

public class JCasIterableTest extends ComponentTestBase {

	@Test
	public void testResetViews() throws CASException {
		jCas.createView("point");
		Iterator<?> views = jCas.getViewIterator();
		while (views.hasNext()) {
			// JCas view = (JCas) views.next();
			views.next();
		}
		jCas.reset();

	}
}
