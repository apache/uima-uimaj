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
package org.uimafit.factory;

import static org.junit.Assert.assertEquals;

import org.apache.uima.UimaContext;
import org.apache.uima.fit.factory.UimaContextFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.junit.Test;

/**
 * @author Steven Bethard, Philip Ogren
 */

public class UimaContextFactoryTest {

	@Test
	public void test() throws ResourceInitializationException {
		UimaContext context = UimaContextFactory.createUimaContext("myBoolean", true, "myBooleans",
				new Boolean[] { true, false, true, false }, "myFloat", 1.0f, "myFloats",
				new Float[] { 2.0f, 2.1f, 3.0f }, "myInt", 1, "myInts", new Integer[] { 2, 3, 4 },
				"myString", "yourString", "myStrings", new String[] { "yourString1", "yourString2",
						"yourString3" });
		assertEquals(true, context.getConfigParameterValue("myBoolean"));
		Boolean[] myBooleans = (Boolean[]) context.getConfigParameterValue("myBooleans");
		assertEquals(4, myBooleans.length);
		assertEquals(true, myBooleans[0]);
		assertEquals(false, myBooleans[1]);
		assertEquals(true, myBooleans[2]);
		assertEquals(false, myBooleans[3]);

		assertEquals(1.0f, context.getConfigParameterValue("myFloat"));
		Float[] myFloats = (Float[]) context.getConfigParameterValue("myFloats");
		assertEquals(3, myFloats.length);
		assertEquals(2.0d, myFloats[0].doubleValue(), 0.001d);
		assertEquals(2.1d, myFloats[1].doubleValue(), 0.001d);
		assertEquals(3.0d, myFloats[2].doubleValue(), 0.001d);

		assertEquals(1, context.getConfigParameterValue("myInt"));
		Integer[] myInts = (Integer[]) context.getConfigParameterValue("myInts");
		assertEquals(3, myInts.length);
		assertEquals(2L, myInts[0].longValue());
		assertEquals(3L, myInts[1].longValue());
		assertEquals(4L, myInts[2].longValue());

		assertEquals("yourString", context.getConfigParameterValue("myString"));
		String[] myStrings = (String[]) context.getConfigParameterValue("myStrings");
		assertEquals(3, myStrings.length);
		assertEquals("yourString1", myStrings[0]);
		assertEquals("yourString2", myStrings[1]);
		assertEquals("yourString3", myStrings[2]);

	}

}
