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
package org.uimafit.factory.testAes;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.descriptor.SofaCapability;
import org.apache.uima.jcas.JCas;
import org.uimafit.component.JCasAnnotator_ImplBase;

/**
 * @author Philip Ogren
 */

@SofaCapability(inputSofas = CAS.NAME_DEFAULT_SOFA)
public class ParameterizedAE extends JCasAnnotator_ImplBase {

	public static final String PARAM_STRING_1 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_STRING_1";
	@ConfigurationParameter(name = PARAM_STRING_1, mandatory = true, defaultValue = "pineapple")
	private String string1;

	public static final String PARAM_STRING_2 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_STRING_2";
	@ConfigurationParameter(name = PARAM_STRING_2, mandatory = false, defaultValue = { "coconut",
			"mango" })
	private String[] string2;

	public static final String PARAM_STRING_3 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_STRING_3";
	@ConfigurationParameter(name = PARAM_STRING_3)
	private String string3;

	public static final String PARAM_STRING_4 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_STRING_4";
	@ConfigurationParameter(name = PARAM_STRING_4, mandatory = true, defaultValue = "apple")
	private String[] string4;

	public static final String PARAM_STRING_5 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_STRING_5";
	@ConfigurationParameter(name = PARAM_STRING_5, mandatory = false, defaultValue = "")
	private String[] string5;

	@ConfigurationParameter(name = "strings6", defaultValue = { "kiwi fruit", "grape", "pear" })
	private Set<String> strings6;

	@ConfigurationParameter(name = "strings7")
	private Set<String> strings7;

	@ConfigurationParameter(name = "strings8", defaultValue = "cherry")
	private Set<String> strings8;

	public static final String PARAM_STRING_9 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_STRING_)";
	@ConfigurationParameter(name = PARAM_STRING_9)
	private Set<String> strings9;

	@SuppressWarnings("unused")
	@ConfigurationParameter(name = "strings10", mandatory = true, defaultValue = {})
	private Set<String> strings10;

	public Set<String> getStrings6() {
		return strings6;
	}

	public Set<String> getStrings7() {
		return strings7;
	}

	public Set<String> getStrings8() {
		return strings8;
	}

	public Set<String> getStrings9() {
		return strings9;
	}

	public static final String PARAM_BOOLEAN_1 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_BOOLEAN_1";
	@ConfigurationParameter(name = PARAM_BOOLEAN_1, mandatory = true, defaultValue = "false")
	private boolean boolean1;

	public static final String PARAM_BOOLEAN_2 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_BOOLEAN_2";
	@ConfigurationParameter(name = PARAM_BOOLEAN_2)
	private Boolean boolean2;
	@ConfigurationParameter
	private boolean boolean2b;

	public boolean isBoolean2b() {
		return boolean2b;
	}

	public static final String PARAM_BOOLEAN_3 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_BOOLEAN_3";
	@ConfigurationParameter(name = PARAM_BOOLEAN_3, mandatory = true, defaultValue = { "true",
			"true", "false" })
	private Boolean[] boolean3;

	public static final String PARAM_BOOLEAN_4 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_BOOLEAN_4";
	@ConfigurationParameter(name = PARAM_BOOLEAN_4, mandatory = true, defaultValue = { "true",
			"false", "true" })
	public boolean[] boolean4;

	public static final String PARAM_BOOLEAN_5 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_BOOLEAN_5";
	@ConfigurationParameter(name = PARAM_BOOLEAN_5, mandatory = true, defaultValue = "false")
	private boolean[] boolean5;

	@ConfigurationParameter(name = "booleans6", defaultValue = { "true", "true", "true", "false" })
	private LinkedList<Boolean> booleans6;

	public LinkedList<Boolean> getBooleans6() {
		return booleans6;
	}

	public static final String PARAM_INT_1 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_INT_1";
	@ConfigurationParameter(name = PARAM_INT_1, mandatory = true, defaultValue = "0")
	private int int1;

	public static final String PARAM_INT_2 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_INT_2";
	@ConfigurationParameter(name = PARAM_INT_2, defaultValue = "42")
	private int int2;

	public static final String PARAM_INT_3 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_INT_3";
	@ConfigurationParameter(name = PARAM_INT_3, defaultValue = { "42", "111" })
	private int[] int3;

	public static final String PARAM_INT_4 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_INT_4";
	@ConfigurationParameter(name = PARAM_INT_4, defaultValue = "2", mandatory = true)
	private Integer[] int4;

	@ConfigurationParameter(name = "ints5", defaultValue = "2")
	private List<Integer> ints5;

	public List<Integer> getInts5() {
		return ints5;
	}

	public List<Integer> getInts6() {
		return ints6;
	}

	@ConfigurationParameter(name = "ints6", defaultValue = { "1", "2", "3", "4", "5" })
	private List<Integer> ints6;

	public static final String PARAM_FLOAT_1 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_FLOAT_1";
	@ConfigurationParameter(name = PARAM_FLOAT_1, mandatory = true, defaultValue = "0.0f")
	private float float1;

	public static final String PARAM_FLOAT_2 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_FLOAT_2";
	@ConfigurationParameter(name = PARAM_FLOAT_2, mandatory = false, defaultValue = "3.1415f")
	private float float2;

	public static final String PARAM_FLOAT_3 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_FLOAT_3";
	@ConfigurationParameter(name = PARAM_FLOAT_3, mandatory = true)
	private float float3;

	public static final String PARAM_FLOAT_4 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_FLOAT_4";
	@ConfigurationParameter(name = PARAM_FLOAT_4, mandatory = false)
	private float[] float4;

	public static final String PARAM_FLOAT_5 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_FLOAT_5";
	@ConfigurationParameter(name = PARAM_FLOAT_5, mandatory = false, defaultValue = { "0.0f",
			"3.1415f", "2.7182818f" })
	private float[] float5;

	public static final String PARAM_FLOAT_6 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_FLOAT_6";
	@ConfigurationParameter(name = PARAM_FLOAT_6, mandatory = true)
	private Float[] float6;

	public static final String PARAM_FLOAT_7 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_FLOAT_7";
	@ConfigurationParameter(name = PARAM_FLOAT_7, mandatory = true, defaultValue = { "1.1111f",
			"2.2222f", "3.333f" })
	private Float[] float7;

	public static enum EnumValue {
		ENUM_1, ENUM_2
	}
	
	public static final String PARAM_ENUM_1 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_ENUM_1";
	@ConfigurationParameter(name = PARAM_ENUM_1, mandatory = true, defaultValue = { "ENUM_1" })
	private EnumValue enum1;

	public static final String PARAM_ENUM_2 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_ENUM_2";
	@ConfigurationParameter(name = PARAM_ENUM_2, mandatory = true, defaultValue = { "ENUM_1", "ENUM_2" })
	private EnumValue[] enum2;

	public static final String PARAM_ENUM_3 = "org.uimafit.factory.testAes.ParameterizedAE.PARAM_ENUM_3";
	@ConfigurationParameter(name = PARAM_ENUM_3, mandatory = true, defaultValue = { "ENUM_1", "ENUM_2" })
	private List<EnumValue> enum3;

	@ConfigurationParameter(name = "file1", mandatory = true, defaultValue = "test/data/file")
	private File file1;
	@ConfigurationParameter(name = "file1b", mandatory = true, defaultValue = { "test/data/file",
			"test/data/file2" })
	private File file1b;
	@ConfigurationParameter(name = "file2", mandatory = true)
	private File file2;

	@ConfigurationParameter(name = "files3")
	private File[] files3;
	@ConfigurationParameter(name = "files4", defaultValue = "test/data/file")
	private File[] files4;
	@ConfigurationParameter(name = "files5", defaultValue = { "test/data/file", "test/data/file2" })
	private File[] files5;

	@ConfigurationParameter(name = "files6")
	private List<File> files6;
	@ConfigurationParameter(name = "files7", defaultValue = "test/data/file")
	private List<File> files7;
	@ConfigurationParameter(name = "files8", defaultValue = { "test/data/file", "test/data/file2" })
	private List<File> files8;
	@ConfigurationParameter(name = "files9")
	private List<File> files9;

	public EnumValue getEnum1() {
		return enum1;
	}
	
	public EnumValue[] getEnum2() {
		return enum2;
	}
	
	public List<EnumValue> getEnum3() {
		return enum3;
	}
	
	public File getFile1() {
		return file1;
	}

	public File getFile1b() {
		return file1b;
	}

	public File getFile2() {
		return file2;
	}

	public File[] getFiles3() {
		return files3;
	}

	public File[] getFiles4() {
		return files4;
	}

	public File[] getFiles5() {
		return files5;
	}

	public List<File> getFiles6() {
		return files6;
	}

	public List<File> getFiles7() {
		return files7;
	}

	public List<File> getFiles8() {
		return files8;
	}

	public List<File> getFiles9() {
		return files9;
	}

	public float[] getFloat4() {
		return float4;
	}

	public float[] getFloat5() {
		return float5;
	}

	public Float[] getFloat6() {
		return float6;
	}

	public Float[] getFloat7() {
		return float7;
	}

	public Integer[] getInt4() {
		return int4;
	}

	public float getFloat1() {
		return float1;
	}

	public float getFloat2() {
		return float2;
	}

	public float getFloat3() {
		return float3;
	}

	public int[] getInt3() {
		return int3;
	}

	public int getInt2() {
		return int2;
	}

	public int getInt1() {
		return int1;
	}

	public boolean[] getBoolean5() {
		return boolean5;
	}

	public Boolean[] getBoolean3() {
		return boolean3;
	}

	public boolean isBoolean2() {
		return boolean2;
	}

	public boolean isBoolean1() {
		return boolean1;
	}

	@Override
	public void process(JCas cas) throws AnalysisEngineProcessException {
		// do nothing
	}

	public String getString1() {
		return string1;
	}

	public String[] getString2() {
		return string2;
	}

	public String getString3() {
		return string3;
	}

	public String[] getString4() {
		return string4;
	}

	public String[] getString5() {
		return string5;
	}

	@SuppressWarnings("unused")
	@ConfigurationParameter(name = "regex1")
	private Pattern regex1;

	@SuppressWarnings("unused")
	@ConfigurationParameter(name = "regex2")
	private Pattern regex2;

}
