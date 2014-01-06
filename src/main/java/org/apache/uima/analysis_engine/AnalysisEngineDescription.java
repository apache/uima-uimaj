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

package org.apache.uima.analysis_engine;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

import org.apache.uima.Constants;
import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.analysis_engine.metadata.FlowControllerDeclaration;
import org.apache.uima.analysis_engine.metadata.SofaMapping;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.ResourceManager;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.metadata.MetaDataObject;
import org.apache.uima.util.InvalidXMLException;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

/**
 * An <code>AnalysisEngineDescription</code> contains all of the information needed to instantiate
 * and use an {@link org.apache.uima.analysis_engine.AnalysisEngine}.
 * <p>
 * The {@link org.apache.uima.UIMAFramework#produceAnalysisEngine(ResourceSpecifier)} factory method
 * is used to create an AnalysisEngine instance from a <code>AnalysisEngineDescription</code>.
 * This insulates applications from knowledge of the particular AnalysisEngine implementation being
 * used.
 * <p>
 * The AnalysisEngine Description contains the following information:
 * <p>
 * <b><u>For a Primitive AnalysisEngine:</u></b>
 * <ul>
 * <li>Name of main annotator class</li>
 * </ul>
 * <p>
 * <b><u>For an Aggregate AnalysisEngine:</u></b>
 * <ul>
 * <li>A set of Resource Specifiers that specify the component AnalysisEngines that comprise the
 * aggregate.</li>
 * <li>Optionally, a Resource Specifier for the FlowController component that determines routing of
 * the CAS to the component AnalysisEngines.</li>
 * </ul>
 * <p>
 * <b><u>For All AnalysisEngines:</u></b>
 * <ul>
 * <li>A set of Resource Specifiers that specify the external resources needed by this
 * AnalysisEngine.</li>
 * <li>{@link AnalysisEngineMetaData Metadata} for this AnalysisEngine - this includes the
 * capabilities of this AnalysisEngine, the TypeSystem used by this AnalysisEngine, and other
 * parameters.</li>
 * </ul>
 * 
 * 
 */
public interface AnalysisEngineDescription extends ResourceCreationSpecifier {

  /**
   * Gets the name of the framework implementation within which the AnalysisEngine executes. The
   * framework name for this implementation is given by {@link Constants#JAVA_FRAMEWORK_NAME}.
   * 
   * @return the framework implementation name
   */
  public String getFrameworkImplementation();

  /**
   * Sets the name of the framework implementation within which the AnalysisEngine executes. The
   * framework name for this implementation is given by {@link Constants#JAVA_FRAMEWORK_NAME}.
   * 
   * @param aFrameworkImplementation
   *          the framework implementation name
   */
  public void setFrameworkImplementation(String aFrameworkImplementation);

  /**
   * Retrieves whether the AnalysisEngine is primitive (consisting of one annotator), as opposed to
   * aggregate (containing multiple delegate AnalysisEngines).
   * <p>
   * Some of the methods on this class apply only to one type of AnalysisEngine:<br>
   * {@link #getAnnotatorImplementationName()} - primitive AnalysisEngine only<br>
   * {@link #getDelegateAnalysisEngineSpecifiers()} - aggregate AnalysisEngine only<br>
   * {@link #getFlowControllerDeclaration()} - aggregate AnalysisEngine only<br>
   * <p>
   * 
   * @return true if and only if the AnalysisEngine is primitive
   */
  public boolean isPrimitive();

  /**
   * Sets whether the AnalysisEngine is primitive (consisting of one annotator), as opposed to
   * aggregate (containing multiple delegate AnalysisEngines).
   * 
   * @param aPrimitive
   *          true if and only if the AnalysisEngine is primitive
   */
  public void setPrimitive(boolean aPrimitive);

  /**
   * For a primitive AnalysisEngine only, retrieves the name of the annotator implementation. For
   * Java annotators, this will be a fully qualified Java class name.
   * 
   * @return the implementation name of the annotator. If the AnalysisEngine is aggregate, always
   *         returns <code>null</code>.
   */
  public String getAnnotatorImplementationName();

  /**
   * For a primitive AnalysisEngine only, sets the name of the annotator implementation. For Java
   * annotators, this must be a fully qualified Java class name.
   * 
   * @param aImplementationName
   *          the implementation name of the annotator.
   */
  public void setAnnotatorImplementationName(String aImplementationName);

  /**
   * For an aggregate AnalysisEngine only, retrieves a collection of {@link ResourceSpecifier}s
   * that indicate which delegate AnalysisEngines comprise the aggregate. Each
   * <code>ResourceSpecifier</code> can either:
   * <ol type="a">
   * <li>completely describe how to build a AnalysisEngine instance</li>
   * <li>describe how to locate a distributed AnalysisEngine service, for example a specific
   * endpoint or a JNDI name</li>
   * </ol>
   * <p>
   * This method returns an unmodifiable Map whose keys are string identifiers and whose values are
   * the <code>ResourceSpecifier</code> objects. The string identifiers in this Map are to refer
   * to the delegate AnalysisEngines from elsewhere in this <code>AnalysisEngineDescription</code>.
   * (For example in the {@link org.apache.uima.analysis_engine.metadata.FlowConstraints}
   * description.)
   * <p>
   * Note that the Map returned by this method will never contain
   * {@link org.apache.uima.resource.metadata.Import} objects -- they will always be resolved first.
   * If you want to get access to the original Import objects, use
   * {@link #getDelegateAnalysisEngineSpecifiersWithImports()}. Also use that method if you want to
   * be able to make changes to the Map.
   * 
   * @return an unmodifiable Map with <code>String</code> keys and {@link ResourceSpecifier}
   *         values.
   * 
   * @throws InvalidXMLException
   *           if import resolution failed
   */
  public Map<String, ResourceSpecifier> getDelegateAnalysisEngineSpecifiers() throws InvalidXMLException;

  /**
   * For an aggregate AnalysisEngine only, retrieves a collection of {@link ResourceSpecifier}s
   * that indicate which delegate AnalysisEngines comprise the aggregate. Each
   * <code>ResourceSpecifier</code> can either:
   * <ol type="a">
   * <li>completely describe how to build a AnalysisEngine instance</li>
   * <li>describe how to locate a distributed AnalysisEngine service, for example a specific
   * endpoint or a JNDI name</li>
   * </ol>
   * <p>
   * This method returns an unmodifiable Map whose keys are string identifiers and whose values are
   * the <code>ResourceSpecifier</code> objects. The string identifiers in this Map are to refer
   * to the delegate AnalysisEngines from elsewhere in this <code>AnalysisEngineDescription</code>.
   * (For example in the {@link org.apache.uima.analysis_engine.metadata.FlowConstraints}
   * description.)
   * <p>
   * Note that the Map returned by this method will never contain
   * {@link org.apache.uima.resource.metadata.Import} objects -- they will always be resolved first.
   * If you want to get access to the original Import objects, use
   * {@link #getDelegateAnalysisEngineSpecifiersWithImports()}. Also use that method if you want to
   * be able to make changes to the Map.
   * 
   * @param aResourceManager
   *          the ResourceManager to use to get the datapath needed to resolve imports
   * 
   * @return an unmodifiable Map with <code>String</code> keys and {@link ResourceSpecifier}
   *         values.
   * 
   * @throws InvalidXMLException
   *           if import resolution failed
   */
  public Map<String, ResourceSpecifier> getDelegateAnalysisEngineSpecifiers(ResourceManager aResourceManager)
          throws InvalidXMLException;

  /**
   * Retrieves a Map whose keys are string identifiers and whose values are the either
   * {@link org.apache.uima.resource.metadata.Import} or {@link ResourceSpecifier} objects. These
   * indicate the delegate AnalysisEngines that comprise the aggregate.
   * <p>
   * This is a direct representation of what is in the XML syntax for the descriptor. That is, if
   * the XML had an &lt;import&gt; element, the Map will contain an <code>Import</code> object. If
   * you do not want to deal with imports, use the {@link #getDelegateAnalysisEngineSpecifiers()}
   * method instead.
   * 
   * @return a Map with <code>String</code> keys and {@link ResourceSpecifier} or
   *         {@link org.apache.uima.resource.metadata.Import} objects as values. This Map may be
   *         modified to add or remove imports or specifiers.
   */
  public Map<String, MetaDataObject> getDelegateAnalysisEngineSpecifiersWithImports();

  /**
   * For an aggregate AnalysisEngine only, gets the declaration of which FlowController should be
   * used by the AnalysisEngine.
   * 
   * @return an object containing either an import of a ResourceSpecifier or a ResourceSpecifier
   *         itself. This specifier will be used to create the FlowController.
   */
  public FlowControllerDeclaration getFlowControllerDeclaration();

  /**
   * For an aggregate AnalysisEngine only, sets the declaration of which FlowController should be
   * used by the AnalysisEngine.
   * 
   * @param aFlowControllerDeclaration
   *          an object containing either an import of a ResourceSpecifier or a ResourceSpecifier
   *          itself. This specifier will be used to create the FlowController.
   */
  public void setFlowControllerDeclaration(FlowControllerDeclaration aFlowControllerDeclaration);

  /**
   * For an aggregate AnalysisEngine only, gets the ResourceSpecifiers of all components in this
   * aggregate. This includes the FlowController as well as all of the component AnalysisEngines.
   * <p>
   * This method returns an unmodifiable Map whose keys are string identifiers and whose values are
   * the <code>ResourceSpecifier</code> objects. The string identifiers in this Map are to refer
   * to the components from elsewhere in this aggregate descriptor, for example in configuration
   * parameter overrides and resource bindings.
   * <p>
   * Note that the Map returned by this method will never contain
   * {@link org.apache.uima.resource.metadata.Import} objects -- they will always be resolved first.
   * If you want to get access to the original Import objects, use
   * {@link #getDelegateAnalysisEngineSpecifiersWithImports()} and {@link #getFlowControllerDeclaration()}.
   * Also use those methods if you want to make changes to be able to make changes to the Map.
   * 
   * @param aResourceManager
   *          the ResourceManager from which to get the datapath needed to resolve imports. Pass
   *          null to use the default ResourceManager.
   * 
   * @return an unmodifiable Map with <code>String</code> keys and {@link ResourceSpecifier}
   *         values.
   * 
   * @throws InvalidXMLException
   *           if import resolution failed
   */
  public Map<String, ResourceSpecifier> getAllComponentSpecifiers(ResourceManager aResourceManager) throws InvalidXMLException;

  /**
   * Retrieves the metadata that describes the AnalysisEngine. This includes the AnalysisEngine's
   * capabilties, the TypeSystem that is uses, the specified Flow information for an aggregate
   * AnalysisEngine, and various informational attributes such as name, description, version,
   * vendor, and copyright.
   * 
   * @return the <code>AnalysisEngineMetaData</code> object containing the AnalysisEngine's
   *         metadata. This object can be modified.
   */
  public AnalysisEngineMetaData getAnalysisEngineMetaData();

  // /**
  // *
  // * @return SofaMapping[]
  // */
  /*
   * Reserved for future use.
   */
  public SofaMapping[] getSofaMappings();

  // /**
  // *
  // * @param aSofaMappings
  // */
  /*
   * Reserved for future use.
   */
  public void setSofaMappings(SofaMapping[] aSofaMappings);

  /**
   * Gets the ResourceSpecifier of one a component of this aggregate, based on its key. This may be
   * the specifier of a component (i.e. delegate) AnalysisEngine, or it may be the specifier of the
   * FlowController.
   * 
   * @param key
   *          the key of the component specifier to get
   * @return the specifier for the component, null if there is no component with the given key
   * @throws ResourceInitializationException
   *           if there's a problem resolving imports
   */
  public ResourceSpecifier getComponentSpecifier(String key) throws ResourceInitializationException;

  /**
   * Does full validation of this Analysis Engine Description. This essentially performs all
   * operations necessary to instantiate an Analysis Engine from this description, except that it
   * does not actually instantiate the Annotator classes (although it does try to <i>load</i> these
   * classes). This method will also attempt to create a CAS based on the descriptor, in order to do
   * full type system verification. If any operations fail, an exception will be thrown.
   * 
   * @throws ResourceInitializationException
   *           if validation failed
   */
  public void doFullValidation() throws ResourceInitializationException;

  /**
   * Does full validation of this Analysis Engine Description. This essentially performs all
   * operations necessary to instantiate an Analysis Engine from this description, except that it
   * does not actually instantiate the Annotator classes (although it does try to <i>load</i> these
   * classes). This method will also attempt to create a CAS based on the descriptor, in order to do
   * full type system verification. If any operations fail, an exception will be thrown.
   * 
   * @param aResourceManager
   *          a ResourceManager instance to use to load annotator classes, external resource
   *          classes, and resolve imports by name.
   * @throws ResourceInitializationException
   *           if validation failed
   */
  public void doFullValidation(ResourceManager aResourceManager)
          throws ResourceInitializationException;

  /**
   * Resolves all import declarations in this AnalysisEngineDescription. For an aggregate, this is
   * recursive, also resolving all imports in each delegate AnalysisEngine. Users do not typically
   * need to call this method; it is called automatically when
   * {@link UIMAFramework#produceAnalysisEngine(ResourceSpecifier)} is called.
   * 
   * @param aResourceManager
   *          the Resource Manager used to locate imports by name. For example, the path in which to
   *          locate these imported descriptors can be set via the
   *          {@link ResourceManager#setDataPath(String)} method.
   * 
   * @throws InvalidXMLException
   *           if an import target does not exist or is invalid
   */
  public void resolveImports(ResourceManager aResourceManager) throws InvalidXMLException;

  /**
   * Resolves all import declarations in this AnalysisEngineDescription. For an aggregate, this is
   * recursive, also resolving all imports in each delegate AnalysisEngine. Users do not typically
   * need to call this method; it is called automatically when
   * {@link UIMAFramework#produceAnalysisEngine(ResourceSpecifier)} is called.
   * <p>
   * This version is used internally to resolve nested imports.
   * 
   * @param aAlreadyImportedDelegateAeUrls
   *          URLs of already imported AE descriptors, so we don't import them again.
   * @param aResourceManager
   *          the Resource Manager used to locate imports by name. For example, the path in which to
   *          locate these imported descriptors can be set via the
   *          {@link ResourceManager#setDataPath(String)} method.
   * 
   * @throws InvalidXMLException
   *           if an import target does not exist or is invalid
   */
  public void resolveImports(Collection<String> aAlreadyImportedDelegateAeUrls,
          ResourceManager aResourceManager) throws InvalidXMLException;

  /**
   * Writes this object's XML representation as a string. Note that if you want to write the XML to
   * a file, it is highly recommended that you use {@link #toXML(OutputStream)} instead, as it
   * ensures that output is written in UTF-8 encoding, which is the default encoding that should be
   * used for XML files.
   * 
   * @param aWriter
   *          a Writer to which the XML string will be written
   * @param aPreserveDelegateAnalysisEngineImports
   *          if true, XML serialization will always preserve &lt;import&gt; elements used to import
   *          delegate analysis engine specifiers into an aggregate. If false, the default import
   *          serialization behavior applies, which is to write &lt;import&gt; elements only in the
   *          case where they have not previously been resolved.
   * 
   * @throws IOException
   *           if an I/O failure occurs
   * @throws SAXException if a SAX exception occurs
   */
  public void toXML(Writer aWriter, boolean aPreserveDelegateAnalysisEngineImports)
          throws SAXException, IOException;

  /**
   * Writes this object's XML representation as a string in UTF-8 encoding.
   * 
   * @param aOutputStream
   *          an OutputStream to which the XML string will be written, in UTF-8 encoding.
   * @param aPreserveDelegateAnalysisEngineImports
   *          if true, XML serialization will always preserve &lt;import&gt; elements used to import
   *          delegate analysis engine specifiers into an aggregate. If false, the default import
   *          serialization behavior applies, which is to write &lt;import&gt; elements only in the
   *          case where they have not previously been resolved.
   * 
   * @throws IOException
   *           if an I/O failure occurs
   * @throws SAXException if a SAX exception occurs
   */
  public void toXML(OutputStream aOutputStream, boolean aPreserveDelegateAnalysisEngineImports)
          throws SAXException, IOException;

  /**
   * Writes this object's XML representation by making calls on a SAX {@link ContentHandler}.
   * 
   * @param aContentHandler
   *          the content handler to which this object will write events that describe its XML
   *          representation.
   * @param aWriteDefaultNamespaceAttribute
   *          whether the namespace of this element should be written as the default namespace. This
   *          should be done only for the root element, and it defaults to false.
   * @param aPreserveDelegateAnalysisEngineImports
   *          if true, XML serialization will always preserve &lt;import&gt; elements used to import
   *          delegate analysis engine specifiers into an aggregate. If false, the default import
   *          serialization behavior applies, which is to write &lt;import&gt; elements only in the
   *          case where they have not previously been resolved.
   * 
   * @throws SAXException if a SAX exception occurs
   */
  public void toXML(ContentHandler aContentHandler, boolean aWriteDefaultNamespaceAttribute,
          boolean aPreserveDelegateAnalysisEngineImports) throws SAXException;

}
