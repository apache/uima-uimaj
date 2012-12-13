/* 
 Copyright 2010	Regents of the University of Colorado.  
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

import static org.apache.uima.fit.util.LocaleUtil.createLocale;
import static org.apache.uima.fit.util.LocaleUtil.getLocaleConstant;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Locale;

import org.junit.Test;

/**
 * 
 * @author Philip Ogren
 * 
 */
public class LocaleUtilTest {

	@Test
	public void testGetLocaleConstant() throws Exception {
		assertEquals(Locale.US, getLocaleConstant("US"));
		assertNull(getLocaleConstant("UN"));
		assertEquals(Locale.ENGLISH, getLocaleConstant("ENGLISH"));
		assertEquals(Locale.CHINA, getLocaleConstant("CHINA"));
		assertNull(getLocaleConstant(""));
		assertNull(getLocaleConstant(null));
	}

	@Test
	public void testCreateLocale() throws Exception {
		assertEquals(new Locale("en", "US"), createLocale("en-US"));
		assertEquals(new Locale("es"), createLocale("es"));
		assertEquals(new Locale("ko", "KR"), createLocale("ko-KR"));
		assertEquals(new Locale("es", "ES", "Traditional_WIN"),
				createLocale("es-ES-Traditional_WIN"));
		assertEquals(new Locale("en", "US", "Colorado"), createLocale("en-US-Colorado"));
		assertEquals(new Locale("en", "US", "Colorado-Boulder"),
				createLocale("en-US-Colorado-Boulder"));
		assertEquals(new Locale("de", "", "POSIX"), createLocale("de--POSIX"));
		// The following examples were taken from the javadoc for java.util.Locale.toString()
		assertEquals(new Locale("en"), createLocale("en"));
		assertEquals(new Locale("de", "DE"), createLocale("de_DE"));
		assertEquals(new Locale("", "GB"), createLocale("_GB"));
		assertEquals(new Locale("en", "US", "WIN"), createLocale("en_US_WIN"));
		assertEquals(new Locale("de", "", "POSIX"), createLocale("de__POSIX"));
		assertEquals(new Locale("fr", "", "MAC"), createLocale("fr__MAC"));
	}

}
