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
package org.uimafit.component.initialize;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static java.util.Arrays.asList;

import java.awt.Point;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;

import org.apache.uima.UIMAException;
import org.apache.uima.UIMAFramework;
import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.fit.ComponentTestBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.impl.ConfigurationParameterSettings_impl;
import org.junit.Test;
import org.uimafit.component.JCasAnnotator_ImplBase;
import org.uimafit.factory.testAes.Annotator1;
import org.uimafit.factory.testAes.ParameterizedAE;
import org.uimafit.factory.testAes.ParameterizedAE.EnumValue;
import org.xml.sax.SAXException;

/**
 * @author Philip Ogren
 */

public class ConfigurationParameterInitializerTest extends ComponentTestBase {

	@Test
	public void testInitialize() throws ResourceInitializationException, SecurityException {

		ResourceInitializationException rie = null;
		try {
			AnalysisEngineFactory.createPrimitive(ParameterizedAE.class, typeSystemDescription);
		}
		catch (ResourceInitializationException e) {
			rie = e;
		}
		assertNotNull(rie);
		AnalysisEngine engine = AnalysisEngineFactory.createPrimitive(ParameterizedAE.class,
				typeSystemDescription,
				ParameterizedAE.PARAM_FLOAT_3, 1.234f,
				ParameterizedAE.PARAM_FLOAT_6, new Float[] { 1.234f, 0.001f },
				"file2", "foo/bar",
				"files9", new File[] { new File("test/data/file"), new File("test/data/file2") } );
				// Test initializing a multi-valued parameter with a single value
				// This is supposed to be fixed as part of issue #79
				// -- REC 2011-05-02
//				ParameterizedAE.PARAM_STRING_9, "singleelementarray");

		ParameterizedAE component = new ParameterizedAE();
		component.initialize(engine.getUimaContext());
		assertEquals("pineapple", component.getString1());
		assertArrayEquals(new String[] { "coconut", "mango" }, component.getString2());
		assertEquals(null, component.getString3());
		assertArrayEquals(new String[] { "apple" }, component.getString4());
		assertArrayEquals(new String[] { "" }, component.getString5());
		assertEquals(3, component.getStrings6().size());
		assertTrue(component.getStrings6().contains("kiwi fruit"));
		assertTrue(component.getStrings6().contains("grape"));
		assertTrue(component.getStrings6().contains("pear"));
		assertNull(component.getStrings7());
		assertEquals(1, component.getStrings8().size());
		assertTrue(component.getStrings8().contains("cherry"));
//		assertTrue(component.getStrings9().contains("singleelementarray"));

		assertFalse(component.isBoolean1());

		NullPointerException npe = null;
		try {
			assertFalse(component.isBoolean2());
		}
		catch (NullPointerException e) {
			npe = e;
		}
		assertNotNull(npe);

		assertFalse(component.isBoolean2b());

		assertTrue(component.getBoolean3()[0]);
		assertTrue(component.getBoolean3()[1]);
		assertFalse(component.getBoolean3()[2]);
		assertTrue(component.boolean4[0]);
		assertFalse(component.boolean4[1]);
		assertTrue(component.boolean4[2]);
		assertFalse(component.getBoolean5()[0]);
		assertEquals(4, component.getBooleans6().size());
		assertTrue(component.getBooleans6().get(0));
		assertTrue(component.getBooleans6().get(1));
		assertTrue(component.getBooleans6().get(2));
		assertFalse(component.getBooleans6().get(3));

		assertEquals(0, component.getInt1());
		assertEquals(42, component.getInt2());
		assertEquals(42, component.getInt3()[0]);
		assertEquals(111, component.getInt3()[1]);
		assertEquals(Integer.valueOf(2), component.getInt4()[0]);
		assertEquals(1, component.getInts5().size());
		assertEquals(2, component.getInts5().get(0).intValue());
		assertEquals(5, component.getInts6().size());
		assertEquals(1, component.getInts6().get(0).intValue());
		assertEquals(2, component.getInts6().get(1).intValue());
		assertEquals(3, component.getInts6().get(2).intValue());
		assertEquals(4, component.getInts6().get(3).intValue());
		assertEquals(5, component.getInts6().get(4).intValue());

		assertEquals(0.0f, component.getFloat1(), 0.001f);
		assertEquals(3.1415f, component.getFloat2(), 0.001f);
		assertEquals(1.234f, component.getFloat3(), 0.001f);
		assertNull(component.getFloat4());
		assertEquals(0f, component.getFloat5()[0], 0.001f);
		assertEquals(3.1415f, component.getFloat5()[1], 0.001f);
		assertEquals(2.7182818f, component.getFloat5()[2], 0.001f);
		assertEquals(1.234f, component.getFloat6()[0], 0.001f);
		assertEquals(0.001f, component.getFloat6()[1], 0.001f);
		assertEquals(1.1111f, component.getFloat7()[0], 0.001f);
		assertEquals(2.2222f, component.getFloat7()[1], 0.001f);
		assertEquals(3.3333f, component.getFloat7()[2], 0.001f);

		assertEquals(EnumValue.ENUM_1, component.getEnum1());
		assertArrayEquals(new EnumValue[] { EnumValue.ENUM_1, EnumValue.ENUM_2 }, component.getEnum2());
		assertEquals(asList( EnumValue.ENUM_1, EnumValue.ENUM_2 ), component.getEnum3());
		assertEquals(new File("test/data/file"), component.getFile1());
		assertEquals(new File("test/data/file"), component.getFile1b());
		assertEquals(new File("foo/bar"), component.getFile2());
		assertNull(component.getFiles3());
		assertArrayEquals(new File[] { new File("test/data/file") }, component.getFiles4());
		assertArrayEquals(new File[] { new File("test/data/file"), new File("test/data/file2") },
				component.getFiles5());
		assertNull(component.getFiles6());
		assertEquals(1, component.getFiles7().size());
		assertEquals(new File("test/data/file"), component.getFiles7().get(0));
		assertEquals(2, component.getFiles8().size());
		assertEquals(new File("test/data/file"), component.getFiles8().get(0));
		assertEquals(new File("test/data/file2"), component.getFiles8().get(1));
		assertEquals(2, component.getFiles9().size());
		assertEquals(new File("test/data/file"), component.getFiles9().get(0));
		assertEquals(new File("test/data/file2"), component.getFiles9().get(1));

		engine = AnalysisEngineFactory.createPrimitive(ParameterizedAE.class,
				typeSystemDescription, ParameterizedAE.PARAM_FLOAT_3, 1.234f,
				ParameterizedAE.PARAM_FLOAT_6, new Float[] { 1.234f, 0.001f },
				ParameterizedAE.PARAM_STRING_1, "lime", ParameterizedAE.PARAM_STRING_2,
				new String[] { "banana", "strawberry" }, ParameterizedAE.PARAM_STRING_3, "cherry",
				ParameterizedAE.PARAM_STRING_4, new String[] { "raspberry", "blueberry",
						"blackberry" }, ParameterizedAE.PARAM_STRING_5, new String[] { "a" },
				ParameterizedAE.PARAM_BOOLEAN_1, true, ParameterizedAE.PARAM_BOOLEAN_2, true,
				ParameterizedAE.PARAM_BOOLEAN_3, new boolean[] { true, true, false },
				ParameterizedAE.PARAM_BOOLEAN_4, new Boolean[] { true, false, false },
				ParameterizedAE.PARAM_BOOLEAN_5, new Boolean[] { true },
				ParameterizedAE.PARAM_INT_1, 0, ParameterizedAE.PARAM_INT_2, 24,
				ParameterizedAE.PARAM_INT_3, new int[] { 5 }, "file1", "foo1/bar1", "file1b",
				"foo1b/bar1b", "file2", "foo2/bar2", "files3", new String[] {
						"C:\\Documents and Settings\\Philip\\My Documents\\", "/usr/local/bin" },
				"files4", new String[0], "files5", new String[] { "foos/bars" }, "files6",
				new String[] { "C:\\Documents and Settings\\Philip\\My Documents\\",
						"/usr/local/bin" }, "files7", new String[0], "files8",
				new String[] { "foos/bars" }, "files9", Arrays.asList(new File("test/data/file"), 
						new File("test/data/file2")));
		component = new ParameterizedAE();
		component.initialize(engine.getUimaContext());
		assertEquals("lime", component.getString1());
		assertArrayEquals(new String[] { "banana", "strawberry" }, component.getString2());
		assertEquals("cherry", component.getString3());
		assertArrayEquals(new String[] { "raspberry", "blueberry", "blackberry" },
				component.getString4());
		assertArrayEquals(new String[] { "a" }, component.getString5());
		assertTrue(component.isBoolean1());
		assertTrue(component.isBoolean2());
		assertTrue(component.getBoolean3()[0]);
		assertTrue(component.getBoolean3()[1]);
		assertFalse(component.getBoolean3()[2]);
		assertTrue(component.boolean4[0]);
		assertFalse(component.boolean4[1]);
		assertFalse(component.boolean4[2]);
		assertTrue(component.getBoolean5()[0]);
		assertEquals(0, component.getInt1());
		assertEquals(24, component.getInt2());
		assertEquals(5, component.getInt3()[0]);

		assertEquals(new File("foo1/bar1"), component.getFile1());
		assertEquals(new File("foo1b/bar1b"), component.getFile1b());
		assertEquals(new File("foo2/bar2"), component.getFile2());
		assertArrayEquals(new File[] {
				new File("C:\\Documents and Settings\\Philip\\My Documents\\"),
				new File("/usr/local/bin") }, component.getFiles3());
		assertEquals(0, component.getFiles4().length);
		assertArrayEquals(new File[] { new File("foos/bars") }, component.getFiles5());
		assertEquals(2, component.getFiles6().size());
		assertEquals(new File("C:\\Documents and Settings\\Philip\\My Documents\\"), component
				.getFiles6().get(0));
		assertEquals(new File("/usr/local/bin"), component.getFiles6().get(1));
		assertEquals(0, component.getFiles7().size());
		assertEquals(1, component.getFiles8().size());
		assertEquals(new File("foos/bars"), component.getFiles8().get(0));
		assertEquals(2, component.getFiles9().size());
		assertEquals(new File("test/data/file"), component.getFiles9().get(0));
		assertEquals(new File("test/data/file2"), component.getFiles9().get(1));

		engine = AnalysisEngineFactory.createPrimitive(ParameterizedAE.class,
				typeSystemDescription, ParameterizedAE.PARAM_FLOAT_3, 1.234f,
				ParameterizedAE.PARAM_FLOAT_6, new Float[] { 1.234f, 0.001f },
				ParameterizedAE.PARAM_BOOLEAN_1, true, ParameterizedAE.PARAM_BOOLEAN_3,
				new boolean[3], ParameterizedAE.PARAM_FLOAT_5, new float[] { 1.2f, 3.4f }, "file2",
				"foo2/bar2");
		component = new ParameterizedAE();
		component.initialize(engine.getUimaContext());
		assertFalse(component.getBoolean3()[0]);
		assertFalse(component.getBoolean3()[1]);
		assertFalse(component.getBoolean3()[2]);
		assertEquals(component.getFloat5()[0], 1.2f, 0.001f);
		assertEquals(component.getFloat5()[1], 3.4f, 0.001f);

		rie = null;
		try {
			engine = AnalysisEngineFactory.createPrimitive(ParameterizedAE.class,
					typeSystemDescription, ParameterizedAE.PARAM_FLOAT_3, 1.234f,
					ParameterizedAE.PARAM_FLOAT_6, new Float[] { 1.234f, 0.001f },
					ParameterizedAE.PARAM_STRING_1, true);
		}
		catch (ResourceInitializationException e) {
			rie = e;
		}
		assertNotNull(rie);

	}

	@Test
	public void testInitialize2() throws ResourceInitializationException {
		AnalysisEngine engine = AnalysisEngineFactory.createPrimitive(Annotator1.class,
				typeSystemDescription);
		assertEquals(1, engine.getAnalysisEngineMetaData().getCapabilities().length);
	}

	@Test
	public void testInitialize3() throws FileNotFoundException, IOException, UIMAException {
		// here we test an optional parameter that is missing from the
		// configuration to ensure that it is filled in with the default value
		AnalysisEngine aed = AnalysisEngineFactory
				.createAnalysisEngineFromPath("src/test/resources/data/descriptor/DefaultValueAE1.xml");
		DefaultValueAE1 ae = new DefaultValueAE1();
		ae.initialize(aed.getUimaContext());
		assertEquals("green", ae.color);

		// here we test a mandatory parameter that is missing from the
		// configuration and ensure that an exception is thrown because
		// no default value is given in the configuration parameter annotation.
		ResourceInitializationException rie = null;
		try {
			aed = AnalysisEngineFactory
					.createAnalysisEngineFromPath("src/test/resources/data/descriptor/DefaultValueAE2.xml");
		}
		catch (ResourceInitializationException e) {
			rie = e;
		}
		assertNotNull(rie);
	}

	/**
	 * If a parameter value is set to null, that is as good as if it was not set at all. If a
	 * default value is specified, it should be used.
	 */
	@Test
	public void testParameterSetToNull() throws Exception {
		String paramColor = DefaultValueAE1.class.getName() + ".color";
		AnalysisEngine aed = AnalysisEngineFactory.createPrimitive(DefaultValueAE1.class, null,
				paramColor, null);
		DefaultValueAE1 ae = new DefaultValueAE1();
		ae.initialize(aed.getUimaContext());
		assertEquals("green", ae.color);
	}

	/**
	 * If a parameter value is set to null, that is as good as if it was not set at all. If it is
	 * mandatory, an exception has to be thrown.
	 */
	@Test(expected = ResourceInitializationException.class)
	public void testMandatoryParameterSetToNull() throws Exception {
		String paramColor = DefaultValueAE2.class.getName() + ".color";
		AnalysisEngine aed = AnalysisEngineFactory.createPrimitive(DefaultValueAE2.class, null,
				paramColor, null);
		DefaultValueAE2 ae = new DefaultValueAE2();
		ae.initialize(aed.getUimaContext());

	}

	/**
	 * Test that a parameter not supported by UIMA produces an error.
	 */
	@Test(expected = IllegalArgumentException.class)
	public void testNonUimaCompatibleParameterValue() throws Exception {
		String paramColor = DefaultValueAE2.class.getName() + ".color";
		AnalysisEngine aed = AnalysisEngineFactory.createPrimitive(DefaultValueAE2.class, null,
				paramColor, new Point(1, 2));
		DefaultValueAE2 ae = new DefaultValueAE2();
		ae.initialize(aed.getUimaContext());
	}

	/**
	 * Check that an Analysis Engine created from a descriptor declaring optional parameters but not
	 * setting them actually uses the default values declared in the Java annotation
	 */
	@Test
	public void testUnsetOptionalParameter() throws Exception {
		AnalysisEngineDescription aed = AnalysisEngineFactory.createPrimitiveDescription(
				DefaultValueAE1.class, (Object[]) null);
		// Remove the settings from the descriptor, but leave the declarations.
		// The settings are already filled with default values by createPrimitiveDescription,
		// but here we want to simulate loading a descriptor without settings from a file.
		// The file of course would declare the parameters optional and thus the settings
		// for the optional parameters would be empty. We expect that a default value from the
		// annotation is used in this case.
		aed.getMetaData().setConfigurationParameterSettings(
				new ConfigurationParameterSettings_impl());
		AnalysisEngine template = UIMAFramework.produceAnalysisEngine(aed);
		DefaultValueAE1 ae = new DefaultValueAE1();
		ae.initialize(template.getUimaContext());
		assertEquals("green", ae.color);
	}

	public static class DefaultValueAE1 extends JCasAnnotator_ImplBase {
		@ConfigurationParameter(defaultValue = "green")
		private String color;

		@Override
		public void initialize(UimaContext aContext) throws ResourceInitializationException {
			super.initialize(aContext);
		}

		@Override
		public void process(JCas aJCas) throws AnalysisEngineProcessException {
			/* do nothing */
		}

	}

	public static class DefaultValueAE2 extends JCasAnnotator_ImplBase {
		@SuppressWarnings("unused")
		@ConfigurationParameter(mandatory = true)
		private String color;

		@Override
		public void initialize(UimaContext aContext) throws ResourceInitializationException {
			super.initialize(aContext);
		}

		@Override
		public void process(JCas aJCas) throws AnalysisEngineProcessException {
			/* do nothing */
		}

	}

	@Test
	public void testEnumDefaultValue() throws Exception {
		try {
			AnalysisEngine aed = AnalysisEngineFactory.createPrimitive(DefaultEnumValueAE.class,
					(Object[]) null);
			DefaultEnumValueAE ae = new DefaultEnumValueAE();
			ae.initialize(aed.getUimaContext());
			assertEquals(Color.GREEN, ae.color);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static enum Color {
		RED, GREEN, BLUE
	}

	public static class DefaultEnumValueAE extends JCasAnnotator_ImplBase {
		@ConfigurationParameter(defaultValue = "GREEN")
		private Color color;

		@Override
		public void initialize(UimaContext aContext) throws ResourceInitializationException {
			super.initialize(aContext);
		}

		@Override
		public void process(JCas aJCas) throws AnalysisEngineProcessException {
			/* do nothing */
		}
	}

	public static class DefaultLocaleValueAE extends JCasAnnotator_ImplBase {
		@ConfigurationParameter(name = "L1", defaultValue = "US")
		public Locale locale1;

		@ConfigurationParameter(name = "L2")
		public Locale locale2;

		@ConfigurationParameter(name = "L3")
		public Locale locale3;

		@ConfigurationParameter(name = "L4")
		public Locale locale4;

		@Override
		public void process(JCas aJCas) throws AnalysisEngineProcessException {
			/* do nothing */
		}
	}

	@Test
	public void testLocaleParams() throws Exception {
		AnalysisEngine aed = AnalysisEngineFactory.createPrimitive(DefaultLocaleValueAE.class,
				"L2", "en-CA", "L3", "CANADA_FRENCH", "L4", "zh");
		DefaultLocaleValueAE ae = new DefaultLocaleValueAE();
		ae.initialize(aed.getUimaContext());
		assertEquals(Locale.US, ae.locale1);
		assertEquals(new Locale("en", "CA"), ae.locale2);
		assertEquals(Locale.CANADA_FRENCH, ae.locale3);
		assertEquals(new Locale("zh"), ae.locale4);

		aed = AnalysisEngineFactory.createPrimitive(DefaultLocaleValueAE.class, "L1",
				"es-ES-Traditional_WIN", "L2", "CHINA", "L3", "es", "L4", "en-CA");
		ae = new DefaultLocaleValueAE();
		ae.initialize(aed.getUimaContext());
		assertEquals(new Locale("es", "ES", "Traditional_WIN"), ae.locale1);
		assertEquals(Locale.CHINA, ae.locale2);
		assertEquals(new Locale("es"), ae.locale3);
		assertEquals(new Locale("en", "CA"), ae.locale4);

		aed = AnalysisEngineFactory.createPrimitive(DefaultLocaleValueAE.class, "L1", "", "L2", "",
				"L3", null);
		ae = new DefaultLocaleValueAE();
		ae.initialize(aed.getUimaContext());
		assertEquals(Locale.getDefault(), ae.locale1);
		assertEquals(Locale.getDefault(), ae.locale2);
		assertEquals(null, ae.locale3);
		assertEquals(null, ae.locale4);

	}

	/**
	 * This main method creates the descriptor files used in testInitialize3. If I weren't lazy I
	 * would figure out how to programmatically remove the configuration parameter corresponding to
	 * 'color'. As it is, however, the parameter must be manually removed (I used the Component
	 * Descriptor Editor to do this.) This point is moot anyways because I am checking in the
	 * generated descriptor files and there is no reason to run this main method in the future.
	 */
	public static void main(String[] args) throws ResourceInitializationException,
			FileNotFoundException, SAXException, IOException {
		AnalysisEngineDescription aed = AnalysisEngineFactory.createPrimitiveDescription(
				DefaultValueAE1.class, (Object[]) null);
		aed.toXML(new FileOutputStream("src/test/resources/data/descriptor/DefaultValueAE1.xml"));
		aed = AnalysisEngineFactory.createPrimitiveDescription(DefaultValueAE2.class,
				(Object[]) null);
		aed.toXML(new FileOutputStream("src/test/resources/data/descriptor/DefaultValueAE2.xml"));
	}

}
