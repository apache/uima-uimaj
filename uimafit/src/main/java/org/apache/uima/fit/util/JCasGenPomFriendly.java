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

package org.apache.uima.fit.util;

import static org.apache.uima.UIMAFramework.getXMLParser;

import java.io.IOException;

import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.fit.factory.TypeSystemDescriptionFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.tools.jcasgen.Jg;
import org.apache.uima.tools.jcasgen.LogThrowErrorImpl;
import org.apache.uima.tools.jcasgen.UimaLoggerProgressMonitor;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;

/**
 * This class provides an advanced version of the UIMA JCas wrapper generator. It's main method
 * takes two arguments. The first is a list of ANT-like URL patterns (';'-separated )which indicate
 * where to look for type descriptors. The second is the output directory. Being able to process
 * multiple type system descriptors at the same time removes the need of having a single master type
 * system file importing all others. Example section for a pom.xml:
 * 
 * <pre>
 * &lt;plugin>
 *   &lt;groupId>org.codehaus.mojo&lt;/groupId>
 *   &lt;artifactId>exec-maven-plugin&lt;/artifactId>
 *   &lt;executions>
 *     &lt;execution>
 *       &lt;phase>process-test-resources&lt;/phase>
 *       &lt;goals>
 *         &lt;goal>java&lt;/goal>
 *       &lt;/goals>
 *     &lt;/execution>
 *   &lt;/executions>
 *   &lt;configuration>
 *     &lt;classpathScope>test&lt;/classpathScope>
 *     &lt;mainClass>org.uimafit.util.JCasGenPomFriendly&lt;/mainClass>
 *     &lt;arguments>
 *       &lt;argument>file:src/test/resources/META-INF/org.uimafit/type/**&#47;*.xml&lt;/argument>
 *       &lt;argument>${basedir}/src/test/java&lt;/argument>
 *     &lt;/arguments>
 *   &lt;/configuration>
 * &lt;/plugin>
 * </pre>
 * 
 * @author Philip Ogren
 * @author Richard Eckart de Castilho
 */
public class JCasGenPomFriendly {
	/**
	 * See class-level javadoc for instructions on running this program.
	 */
	public static void main(String[] args) throws Exception {
		Jg jg = new Jg();
		for (String file : TypeSystemDescriptionFactory.resolve(args[0].split(";"))) {
			generate(jg, file, args[1], load(file));
		}
	}

	private static TypeSystemDescription load(String location) throws IOException,
			InvalidXMLException {
		XMLInputSource xmlInputType1 = new XMLInputSource(location);
		return getXMLParser().parseTypeSystemDescription(xmlInputType1);
	}

	private static void generate(Jg jg, String inputFile, String outputDirectory,
			TypeSystemDescription tsd) throws IOException, ResourceInitializationException {
		CAS cas = CasCreationUtils.createCas(tsd, null, null);
		jg.mainForCde(null, new UimaLoggerProgressMonitor(), new LogThrowErrorImpl(), inputFile,
				outputDirectory, tsd.getTypes(), (CASImpl) cas);
	}
}
