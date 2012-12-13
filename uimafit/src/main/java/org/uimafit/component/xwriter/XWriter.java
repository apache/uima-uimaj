/*
  Copyright 2010 Regents of the University of Colorado.
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

package org.uimafit.component.xwriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.fit.factory.initializable.InitializableFactory;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.XMLSerializer;
import org.uimafit.component.JCasConsumer_ImplBase;
import org.xml.sax.SAXException;

/**
 * 
 * A simple CAS consumer that generates XCAS (XML representation of the CAS) files in the
 * filesystem.
 * 
 * @author Philip Ogren
 */

public class XWriter extends JCasConsumer_ImplBase {

	/**
	 * The parameter name for the configuration parameter that specifies the output directory
	 */
	public static final String PARAM_OUTPUT_DIRECTORY_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName(XWriter.class, "outputDirectoryName");
	@ConfigurationParameter(mandatory = true, description = "takes a path to directory into which output files will be written.")
	private String outputDirectoryName;

	/**
	 * The parameter name for the configuration parameter that provides the name of the XML scheme
	 * to use.
	 */
	public static final String PARAM_XML_SCHEME_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName(XWriter.class, "xmlSchemeName");
	@ConfigurationParameter(mandatory = true, defaultValue = "XMI", description = "specifies the UIMA XML serialization scheme that should be used. "
			+ "Valid values for this parameter are 'XMI' (default) and 'XCAS'.")
	private String xmlSchemeName;

	/**
	 * The parameter name for the configuration parameter that specifies the name of the class that
	 * implements the file namer
	 */
	public static final String PARAM_FILE_NAMER_CLASS_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName(XWriter.class, "fileNamerClassName");
	@ConfigurationParameter(mandatory = true, description = "the class name of the XWriterFileNamer implementation to use", defaultValue = "org.uimafit.component.xwriter.IntegerFileNamer")
	protected String fileNamerClassName;

	/**
	 * The name of the XMI XML scheme. This is a valid value for the parameter
	 * {@value #PARAM_XML_SCHEME_NAME}
	 */
	public static final String XMI = "XMI";

	/**
	 * The name of the XCAS XML scheme. This is a valid value for the parameter
	 * {@value #PARAM_XML_SCHEME_NAME}
	 */
	public static final String XCAS = "XCAS";

	private File outputDirectory;

	private boolean useXMI = true;

	private XWriterFileNamer fileNamer;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		outputDirectory = new File(outputDirectoryName);
		if (!outputDirectory.exists()) {
			outputDirectory.mkdirs();
		}

		if (xmlSchemeName.equals(XMI)) {
			useXMI = true;
		}
		else if (xmlSchemeName.equals(XCAS)) {
			useXMI = false;
		}
		else {
			throw new ResourceInitializationException(String.format(
					"parameter '%1$s' must be either '%2$s' or '%3$s'.", PARAM_XML_SCHEME_NAME,
					XMI, XCAS), null);
		}

		fileNamer = InitializableFactory
				.create(context, fileNamerClassName, XWriterFileNamer.class);
	}

	@Override
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		String fileName = fileNamer.nameFile(jcas);
		try {
			if (useXMI) {
				writeXmi(jcas.getCas(), fileName);
			}
			else {
				writeXCas(jcas.getCas(), fileName);
			}
		}
		catch (IOException e) {
			throw new AnalysisEngineProcessException(e);
		}
		catch (SAXException e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	private void writeXCas(CAS aCas, String fileName) throws IOException, SAXException {
		File outFile = new File(outputDirectory, fileName + ".xcas");
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(outFile);
			XCASSerializer ser = new XCASSerializer(aCas.getTypeSystem());
			XMLSerializer xmlSer = new XMLSerializer(out, false);
			ser.serialize(aCas, xmlSer.getContentHandler());
		}
		finally {
			if (out != null) {
				out.close();
			}
		}
	}

	private void writeXmi(CAS aCas, String id) throws IOException, SAXException {
		File outFile = new File(outputDirectory, id + ".xmi");
		FileOutputStream out = null;

		try {
			out = new FileOutputStream(outFile);
			XmiCasSerializer ser = new XmiCasSerializer(aCas.getTypeSystem());
			XMLSerializer xmlSer = new XMLSerializer(out, false);
			ser.serialize(aCas, xmlSer.getContentHandler());
		}
		finally {
			if (out != null) {
				out.close();
			}
		}
	}

}
