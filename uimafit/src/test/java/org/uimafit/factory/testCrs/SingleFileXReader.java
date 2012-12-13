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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XCASDeserializer;
import org.apache.uima.cas.impl.XmiCasDeserializer;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.fit.factory.ConfigurationParameterFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;
import org.uimafit.component.CasCollectionReader_ImplBase;
import org.xml.sax.SAXException;

/**
 * <br>
 * 
 * This collection reader allows one to read in a single XMI or XCAS file. It's primary purpose is
 * to help out a couple JCasFactory create methods. However, it is also used for this project unit
 * tests as an example collection reader.
 * 
 * @author Steven Bethard, Philip Ogren
 */

public class SingleFileXReader extends CasCollectionReader_ImplBase {

	public static final String PARAM_FILE_NAME = ConfigurationParameterFactory
			.createConfigurationParameterName(SingleFileXReader.class, "fileName");

	@ConfigurationParameter(mandatory = true, description = "takes the name of a single xmi or xcas file to be processed.")
	private String fileName;

	public static final String XMI = "XMI";
	public static final String XCAS = "XCAS";

	public static final String PARAM_XML_SCHEME = ConfigurationParameterFactory
			.createConfigurationParameterName(SingleFileXReader.class, "xmlScheme");
	@ConfigurationParameter(mandatory = true, description = "specifies the UIMA XML serialization scheme that should be usedValid values for this parameter are 'XMI' and 'XCAS'. See XmiCasSerializer or XCASSerializer", defaultValue = XMI)
	private String xmlScheme;

	private boolean useXMI = true;

	private boolean hasNext = true;

	private File file;

	@Override
	public void initialize(UimaContext context) throws ResourceInitializationException {
		super.initialize(context);

		file = new File(fileName);

		if (xmlScheme.equals(XMI)) {
			useXMI = true;
		}
		else if (xmlScheme.equals(XCAS)) {
			useXMI = false;
		}
		else {
			throw new ResourceInitializationException(String.format(
					"parameter '%1$s' must be either '%2$s' or '%3$s' or left empty.",
					PARAM_XML_SCHEME, XMI, XCAS), null);
		}

	}

	public void getNext(CAS cas) throws IOException, CollectionException {

		FileInputStream inputStream = new FileInputStream(file);

		try {
			if (useXMI) {
				XmiCasDeserializer.deserialize(inputStream, cas);
			}
			else {
				XCASDeserializer.deserialize(inputStream, cas);
			}
		}
		catch (SAXException e) {
			throw new CollectionException(e);
		}
		finally {
			inputStream.close();
		}

		inputStream.close();
		hasNext = false;
	}

	@Override
	public void close() throws IOException {
		// do nothing
	}

	public Progress[] getProgress() {
		if (hasNext) {
			return new Progress[] { new ProgressImpl(0, 1, Progress.ENTITIES) };
		}
		return new Progress[] { new ProgressImpl(1, 1, Progress.ENTITIES) };
	}

	public boolean hasNext() throws IOException, CollectionException {
		return hasNext;
	}

}
