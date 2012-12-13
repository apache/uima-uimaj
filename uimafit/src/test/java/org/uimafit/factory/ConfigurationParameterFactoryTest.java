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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.junit.Test;

/**
 * @author Philip Ogren
 * @author Richard Eckart de Castilho
 */

public class ConfigurationParameterFactoryTest {

	public static final String PARAM_DOUBLE_1 = "org.uimafit.factory.ConfigurationParameterFactoryTest.PARAM_STRING_1";
	@ConfigurationParameter(name = PARAM_DOUBLE_1, mandatory = true, defaultValue = "3.1415")
	private Double double1;

	public static final String PARAM_DOUBLE_2 = "org.uimafit.factory.ConfigurationParameterFactoryTest.PARAM_DOUBLE_2";
	@ConfigurationParameter(name = PARAM_DOUBLE_2, mandatory = true, defaultValue = "3.3333")
	private Double[] double2;
	private Double[] double3;

	public Double[] getDouble2() {
		return double2;
	}

	public void setDouble2(Double[] double2) {
		this.double2 = double2;
	}

	public Double[] getDouble3() {
		return double3;
	}

	public void setDouble3(Double[] double3) {
		this.double3 = double3;
	}

	public Double getDouble1() {
		return double1;
	}

	public void setDouble1(Double double1) {
		this.double1 = double1;
	}

	@Test
	public void test1() throws SecurityException, NoSuchFieldException {
		Float value = (Float) ConfigurationParameterFactory
				.getDefaultValue(ConfigurationParameterFactoryTest.class
						.getDeclaredField("double1"));
		assertEquals(3.1415, value, 1e-4);

		Float[] values = (Float[]) ConfigurationParameterFactory
				.getDefaultValue(ConfigurationParameterFactoryTest.class
						.getDeclaredField("double2"));
		assertEquals(1, values.length);
		assertEquals(3.3333, values[0], 1e-4);

		IllegalArgumentException iae = null;
		try {
			ConfigurationParameterFactory.getDefaultValue(ConfigurationParameterFactoryTest.class
					.getDeclaredField("double3"));
		}
		catch (IllegalArgumentException e) {
			iae = e;
		}
		assertNotNull(iae);

	}

	@Test(expected = IllegalArgumentException.class)
	public void test2() throws Exception {
		ConfigurationParameterFactory
				.createPrimitiveParameter(ConfigurationParameterFactoryTest.class
						.getDeclaredField("double3"));
	}

	@ConfigurationParameter
	public String param1;

	@Test
	public void testParam1() throws Exception, NoSuchFieldException {
		Field field1 = ConfigurationParameterFactoryTest.class.getDeclaredField("param1");
		org.apache.uima.resource.metadata.ConfigurationParameter cp = ConfigurationParameterFactory
				.createPrimitiveParameter(field1);
		assertEquals("org.uimafit.factory.ConfigurationParameterFactoryTest.param1", cp.getName());
		assertEquals(org.apache.uima.resource.metadata.ConfigurationParameter.TYPE_STRING,
				cp.getType());
		assertEquals("", cp.getDescription());
		assertFalse(cp.isMandatory());
		assertFalse(cp.isMultiValued());
		assertNull(ConfigurationParameterFactory.getDefaultValue(field1));
	}

	@SuppressWarnings("unused")
	@ConfigurationParameter(name = "my-boolean-param", mandatory = true, description = "my description", defaultValue = {
			"false", "false", "true" })
	private boolean[] param2;

	@Test
	public void testParam2() throws Exception, NoSuchFieldException {
		Field field2 = ConfigurationParameterFactoryTest.class.getDeclaredField("param2");
		org.apache.uima.resource.metadata.ConfigurationParameter cp = ConfigurationParameterFactory
				.createPrimitiveParameter(field2);
		assertEquals("my-boolean-param", cp.getName());
		assertEquals(org.apache.uima.resource.metadata.ConfigurationParameter.TYPE_BOOLEAN,
				cp.getType());
		assertEquals("my description", cp.getDescription());
		assertTrue(cp.isMandatory());
		assertTrue(cp.isMultiValued());
		Boolean[] defaultValue = (Boolean[]) ConfigurationParameterFactory.getDefaultValue(field2);
		assertFalse(defaultValue[0]);
		assertFalse(defaultValue[1]);
		assertTrue(defaultValue[2]);
	}

	@SuppressWarnings("unused")
	@ConfigurationParameter
	private Integer param3;

	@Test
	public void testParam3() throws Exception, NoSuchFieldException {
		Field field3 = ConfigurationParameterFactoryTest.class.getDeclaredField("param3");
		org.apache.uima.resource.metadata.ConfigurationParameter cp = ConfigurationParameterFactory
				.createPrimitiveParameter(field3);
		assertEquals("org.uimafit.factory.ConfigurationParameterFactoryTest.param3", cp.getName());
		assertEquals(org.apache.uima.resource.metadata.ConfigurationParameter.TYPE_INTEGER,
				cp.getType());
		assertEquals("", cp.getDescription());
		assertFalse(cp.isMandatory());
		assertFalse(cp.isMultiValued());
		assertNull(ConfigurationParameterFactory.getDefaultValue(field3));
	}

	private static class CPFT {
		@SuppressWarnings("unused")
		@ConfigurationParameter(defaultValue = { "a", "b", "c" })
		private String[] param4;
	}

	@Test
	public void testParam4() throws Exception, NoSuchFieldException {
		Field field4 = CPFT.class.getDeclaredField("param4");
		org.apache.uima.resource.metadata.ConfigurationParameter cp = ConfigurationParameterFactory
				.createPrimitiveParameter(field4);
		assertEquals("org.uimafit.factory.ConfigurationParameterFactoryTest$CPFT.param4",
				cp.getName());
		assertEquals(org.apache.uima.resource.metadata.ConfigurationParameter.TYPE_STRING,
				cp.getType());
		assertEquals("", cp.getDescription());
		assertFalse(cp.isMandatory());
		assertTrue(cp.isMultiValued());
		assertArrayEquals(new String[] { "a", "b", "c" },
				(String[]) ConfigurationParameterFactory.getDefaultValue(field4));
	}

	@SuppressWarnings("unused")
	@ConfigurationParameter(defaultValue = { "data/foo", "bar" })
	private List<File> fileList;

	@Test
	public void testFileList() throws Exception {
		Field field = this.getClass().getDeclaredField("fileList");
		org.apache.uima.resource.metadata.ConfigurationParameter param;
		param = ConfigurationParameterFactory.createPrimitiveParameter(field);
		assertEquals(this.getClass().getName() + ".fileList", param.getName());
		assertEquals(org.apache.uima.resource.metadata.ConfigurationParameter.TYPE_STRING,
				param.getType());
		assertEquals("", param.getDescription());
		assertFalse(param.isMandatory());
		String[] expected = new String[] { "data/foo", "bar" };
		String[] actual = (String[]) ConfigurationParameterFactory.getDefaultValue(field);
		assertArrayEquals(expected, actual);
	}

	@SuppressWarnings("unused")
	@ConfigurationParameter(defaultValue = { "5", "5", "4", "3" })
	private Set<String> stringSet;

	@Test
	public void testStringSet() throws Exception {
		Field field = this.getClass().getDeclaredField("stringSet");
		org.apache.uima.resource.metadata.ConfigurationParameter param;
		param = ConfigurationParameterFactory.createPrimitiveParameter(field);
		assertEquals(this.getClass().getName() + ".stringSet", param.getName());
		assertEquals(org.apache.uima.resource.metadata.ConfigurationParameter.TYPE_STRING,
				param.getType());
		assertFalse(param.isMandatory());
		String[] expected = new String[] { "5", "5", "4", "3" };
		String[] actual = (String[]) ConfigurationParameterFactory.getDefaultValue(field);
		assertArrayEquals(expected, actual);
	}

}
