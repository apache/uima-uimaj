/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.uima.fit.maven;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.apache.uima.fit.factory.AnalysisEngineFactory;
import org.apache.uima.fit.factory.CollectionReaderFactory;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceSpecifier;
import org.codehaus.plexus.util.FileUtils;
import org.xml.sax.SAXException;

/**
 * Generate descriptor files for uimaFIT-based UIMA components.
 * 
 * @see http://maven.apache.org/plugin-tools/maven-plugin-tools-annotations/index.html
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
		requiresDependencyResolution = ResolutionScope.COMPILE,
		requiresDependencyCollection = ResolutionScope.COMPILE)
public class GenerateDescriptorsMojo
	extends AbstractMojo
{
	@Component
	private MavenProject project;

	private ClassLoader componentLoader;

	@Override
	public void execute()
		throws MojoExecutionException
	{
		String[] files = FileUtils.getFilesFromExtension(project.getBuild().getOutputDirectory(),
				new String[] { "class" });

		// Create a class loader which covers the classes compiled in the current project and all
		// dependencies.
		try {
			List<URL> urls = new ArrayList<URL>();
			for (Object object : project.getCompileClasspathElements()) {
				String path = (String) object;
				getLog().debug("Classpath entry: " + object);
				urls.add(new File(path).toURI().toURL());
			}
			for (Artifact dep : (Set<Artifact>) project.getDependencyArtifacts()) {
				getLog().debug("Classpath entry: " + dep.getFile());
				urls.add(dep.getFile().toURI().toURL());
			}
			componentLoader = new URLClassLoader(urls.toArray(new URL[] {}), 
					getClass().getClassLoader());
		}
		catch (Exception e) {
			throw new MojoExecutionException("Cannot initialize classloader", e);
		}

		for (String file : files) {
			String base = file.substring(0, file.length() - 6);
			String clazzName = base.substring(project.getBuild().getOutputDirectory().length() + 1)
					.replace("/", ".");
			try {
				Class clazz = getClass(clazzName);
				ResourceSpecifier desc = null;
				switch (getType(clazz)) {
				case ANALYSIS_ENGINE:
					desc = AnalysisEngineFactory.createPrimitiveDescription(clazz);
					break;
				case COLLECTION_READER:
					desc = CollectionReaderFactory.createDescription(clazz);
				default:
					// Do nothing
				}

				if (desc != null) {
					toXML(desc, base + ".xml");
				}
			}
			catch (SAXException e) {
				getLog().warn("Cannot serialize descriptor for [" + clazzName + "]", e);
			}
			catch (IOException e) {
				getLog().warn("Cannot write descriptor for [" + clazzName + "]", e);
			}
			catch (ClassNotFoundException e) {
				getLog().warn("Cannot analyze class [" + clazzName + "]", e);
			}
			catch (ResourceInitializationException e) {
				getLog().warn("Cannot generate descriptor for [" + clazzName + "]", e);
			}
		}
	}

	/**
	 * Save descriptor XML to file system.
	 * @throws IOException 
	 * @throws SAXException 
	 */
	private void toXML(ResourceSpecifier aDesc, String aFilename) throws SAXException, IOException
	{
		OutputStream os = null;
		try {
			File out = new File(aFilename);
			getLog().info("Writing descriptor to: " + out);
			os = new FileOutputStream(out);
			aDesc.toXML(os);
		}
		finally {
			IOUtils.closeQuietly(os);
		}
	}

	/**
	 * Load class using the component classloader.
	 * @throws ClassNotFoundException 
	 */
	private Class getClass(String aClassName)
		throws ClassNotFoundException
	{
		return componentLoader.loadClass(aClassName);
	}

	/**
	 * Determine what kind of class it is.
	 * @throws ClassNotFoundException 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private ComponentType getType(Class aClass)
		throws ClassNotFoundException
	{
			Class iCR = getClass("org.apache.uima.collection.CollectionReader");
			Class iAE = getClass("org.apache.uima.analysis_component.AnalysisComponent");
			if (iCR.isAssignableFrom(aClass)) {
				return ComponentType.COLLECTION_READER;
			}
			else if (iAE.isAssignableFrom(aClass)) {
				return ComponentType.ANALYSIS_ENGINE;
			}
			else {
				return ComponentType.NONE;
			}
	}

	private enum ComponentType
	{
		COLLECTION_READER, ANALYSIS_ENGINE, NONE;
	}
}
