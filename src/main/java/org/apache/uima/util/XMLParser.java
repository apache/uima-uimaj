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

package org.apache.uima.util;

import java.net.URL;

import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.TaeDescription;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.CasInitializerDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.collection.metadata.CpeDescription;
import org.apache.uima.flow.FlowControllerDescription;
import org.apache.uima.resource.CustomResourceSpecifier;
import org.apache.uima.resource.PearSpecifier;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.resource.URISpecifier;
import org.apache.uima.resource.metadata.FsIndexCollection;
import org.apache.uima.resource.metadata.ResourceManagerConfiguration;
import org.apache.uima.resource.metadata.ResourceMetaData;
import org.apache.uima.resource.metadata.TypePriorities;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.search.IndexBuildSpecification;
import org.w3c.dom.Element;

/**
 * A UIMA <code>XMLParser</code> parses XML documents and generates UIMA components represented by
 * the XML.
 * <p>
 * An application obtains a reference to the <code>XMLParser</code> by calling the
 * {@link org.apache.uima.UIMAFramework#getXMLParser()} method. The application then uses the
 * <code>XMLParser</code> by passing an <code>InputStream</code> to one of its
 * <code>parse</code> methods - for example
 * {@link #parseAnalysisEngineDescription(XMLInputSource)} for parsing an
 * {@link AnalysisEngineDescription} from its XML representation.
 * <p>
 * XML schema validation is off by default; it can be turned on by calling the method
 * {@link #enableSchemaValidation(boolean)}.
 * <p>
 * UIMA developers who provide new types of XMLizable components must configure the XML parser by
 * using the {@link #addMapping(String,String)} method to specify mappings between XML Element names
 * and the class names of the objects to be built from elements with those names. All objects to be
 * built by the XML parser must implement {@link XMLizable} and provide an implementation of
 * {@link XMLizable#buildFromXMLElement(Element, XMLParser)}.
 * <p>
 * Note that we are considering replacing this ad-hoc XML data binding interface with the java
 * standard extension JAXB. See <a href="http://java.sun.com/xml/jaxb"> http://java.sun.com/xml/jaxb</a>
 * for details on JAXB.
 * 
 * 
 */
public interface XMLParser {

  /**
   * Enables or disables XML schema validation.
   * 
   * @param aEnable
   *          true to enable validation, false to disable validation
   */
  public void enableSchemaValidation(boolean aEnable);

  /**
   * Parses an XML input stream and produces an object. elements.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aNamespaceForSchema
   *          XML namespace for elements to be validated against XML schema. If null, no schema will
   *          be used (unless one is declared in the document itself). This parameter is ignored if
   *          schema validation has not been enabled via {@link #enableSchemaValidation(boolean)}.
   * @param aSchemaUrl
   *          URL to XML schema that will be used to validate the XML document. If null, no schema
   *          will be used (unless one is declared in the document itself). This parameter is
   *          ignored if schema validation has not been enabled via
   *          {@link #enableSchemaValidation(boolean)}.
   * 
   * @return an <code>XMLizable</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid object
   */
  public XMLizable parse(XMLInputSource aInput, String aNamespaceForSchema, URL aSchemaUrl)
          throws InvalidXMLException;

  /**
   * Parses an XML input stream and produces an object.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aNamespaceForSchema
   *          XML namespace for elements to be validated against XML schema. If null, no schema will
   *          be used (unless one is declared in the document itself). This parameter is ignored if
   *          schema validation has not been enabled via {@link #enableSchemaValidation(boolean)}.
   * @param aSchemaUrl
   *          URL to XML schema that will be used to validate the XML document. If null, no schema
   *          will be used (unless one is declared in the document itself). This parameter is
   *          ignored if schema validation has not been enabled via
   *          {@link #enableSchemaValidation(boolean)}.
   * @param aOptions
   *          option settings
   * 
   * @return an <code>XMLizable</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid object
   */
  public XMLizable parse(XMLInputSource aInput, String aNamespaceForSchema, URL aSchemaUrl,
          ParsingOptions aOptions) throws InvalidXMLException;

  /**
   * Parses an XML input stream and produces an object. XIncludes will be expanded but no schema
   * validation will be done.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return an <code>XMLizable</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid object
   */
  public XMLizable parse(XMLInputSource aInput) throws InvalidXMLException;

  /**
   * Parses an XML input stream and produces an object. No schema validation will be done.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return an <code>XMLizable</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid object
   */
  public XMLizable parse(XMLInputSource aInput, ParsingOptions aOptions) throws InvalidXMLException;

  /**
   * Parses a ResourceSpecifier from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>ResourceSpecifier</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid ResourceSpecifier
   */
  public ResourceSpecifier parseResourceSpecifier(XMLInputSource aInput) throws InvalidXMLException;

  /**
   * Parses a ResourceSpecifier from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>ResourceSpecifier</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid ResourceSpecifier
   */
  public ResourceSpecifier parseResourceSpecifier(XMLInputSource aInput, ParsingOptions aOptions)
          throws InvalidXMLException;

  /**
   * Parses a ResourceMetaData object from an XML input stream. XML schema validation will be done
   * against the {@link #RESOURCE_SPECIFIER_SCHEMA_NAME} if it can be found in the classpath.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>ResourceMetaData</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid ResourceSpecifier
   */
  public ResourceMetaData parseResourceMetaData(XMLInputSource aInput) throws InvalidXMLException;

  /**
   * Parses a ResourceMetaData object from an XML input stream. XML schema validation will be done
   * against the {@link #RESOURCE_SPECIFIER_SCHEMA_NAME} if it can be found in the classpath.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>ResourceMetaData</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid ResourceSpecifier
   */
  public ResourceMetaData parseResourceMetaData(XMLInputSource aInput, ParsingOptions aOptions)
          throws InvalidXMLException;

  /**
   * Parses a URISpecifier from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>URISpecifier</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid URISpecifier
   */
  public URISpecifier parseURISpecifier(XMLInputSource aInput) throws InvalidXMLException;

  /**
   * Parses a URISpecifier from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>URISpecifier</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid URISpecifier
   */
  public URISpecifier parseURISpecifier(XMLInputSource aInput, ParsingOptions aOptions)
          throws InvalidXMLException;

  /**
   * Parses an AnalysisEngineDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return an <code>AnalysisEngineDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid AnalysisEngineDescription
   */
  public AnalysisEngineDescription parseAnalysisEngineDescription(XMLInputSource aInput,
          ParsingOptions aOptions) throws InvalidXMLException;

  /**
   * Parses an AnalysisEngineDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return an <code>AnalysisEngineDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid AnalysisEngineDescription
   */
  public AnalysisEngineDescription parseAnalysisEngineDescription(XMLInputSource aInput)
          throws InvalidXMLException;

  /**
   * Parses a TaeDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>TaeDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid TaeDescription
   * 
   * @deprecated As of v2.0, {@link #parseAnalysisEngineDescription(XMLInputSource)} should be used
   *             instead.
   */
  @Deprecated
  public TaeDescription parseTaeDescription(XMLInputSource aInput) throws InvalidXMLException;

  /**
   * Parses a TaeDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>TaeDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid TaeDescription
   * 
   * @deprecated As of v2.0, {@link #parseAnalysisEngineDescription(XMLInputSource,ParsingOptions)}
   *             should be used instead.
   */
  @Deprecated
  public TaeDescription parseTaeDescription(XMLInputSource aInput, ParsingOptions aOptions)
          throws InvalidXMLException;

  /**
   * Parses a CasConsumerDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>CasConsumerDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid CasConsumerDescription
   */
  public CasConsumerDescription parseCasConsumerDescription(XMLInputSource aInput)
          throws InvalidXMLException;

  /**
   * Parses a CasConsumerDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>CasConsumerDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid CasConsumerDescription
   */
  public CasConsumerDescription parseCasConsumerDescription(XMLInputSource aInput,
          ParsingOptions aOptions) throws InvalidXMLException;

  /**
   * Parses a CasInitializerDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>CasInitializerDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid CasInitializerDescription
   */
  public CasInitializerDescription parseCasInitializerDescription(XMLInputSource aInput)
          throws InvalidXMLException;

  /**
   * Parses a CasInitializerDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>CasInitializerDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid CasInitializerDescription
   */
  public CasInitializerDescription parseCasInitializerDescription(XMLInputSource aInput,
          ParsingOptions aOptions) throws InvalidXMLException;

  /**
   * Parses a CollectionReaderDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>CollectionReaderDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid CollectionReaderDescription
   */
  public CollectionReaderDescription parseCollectionReaderDescription(XMLInputSource aInput)
          throws InvalidXMLException;

  /**
   * Parses a CollectionReaderDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>CollectionReaderDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid CollectionReaderDescription
   */
  public CollectionReaderDescription parseCollectionReaderDescription(XMLInputSource aInput,
          ParsingOptions aOptions) throws InvalidXMLException;

  /**
   * Parses a CpeDescription from an XML input stream.
   * <p>
   * NOTE: the option settings {@link ParsingOptions} are not currently available for parsing
   * CpeDescriptions, because they use a different parsing mechanism than the other specifier types.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>cpeDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid IndexingSpecification
   */
  public CpeDescription parseCpeDescription(XMLInputSource aInput) throws InvalidXMLException;

  /**
   * Parses a ResultSpecification from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>ResultSpecification</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid ResultSpecification
   */
  public ResultSpecification parseResultSpecification(XMLInputSource aInput)
          throws InvalidXMLException;

  /**
   * Parses a ResultSpecification from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>ResultSpecification</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid ResultSpecification
   */
  public ResultSpecification parseResultSpecification(XMLInputSource aInput, ParsingOptions aOptions)
          throws InvalidXMLException;

  /**
   * Parses a TypeSystemDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>TypeSystemDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid TypeSystemDescription
   */
  public TypeSystemDescription parseTypeSystemDescription(XMLInputSource aInput)
          throws InvalidXMLException;

  /**
   * Parses a TypeSystemDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>TypeSystemDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid TypeSystemDescription
   */
  public TypeSystemDescription parseTypeSystemDescription(XMLInputSource aInput,
          ParsingOptions aOptions) throws InvalidXMLException;

  /**
   * Parses a TypePriorities declaration from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>TypePriorities</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid TypePriorities
   */
  public TypePriorities parseTypePriorities(XMLInputSource aInput) throws InvalidXMLException;

  /**
   * Parses a TypePriorities declaration from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>TypePriorities</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid TypePriorities
   */
  public TypePriorities parseTypePriorities(XMLInputSource aInput, ParsingOptions aOptions)
          throws InvalidXMLException;

  /**
   * Parses a FsIndexCollection from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>FsIndexCollection</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid FsIndexCollection
   */
  public FsIndexCollection parseFsIndexCollection(XMLInputSource aInput) throws InvalidXMLException;

  /**
   * Parses a FsIndexCollection from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>FsIndexCollection</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid FsIndexCollection
   */
  public FsIndexCollection parseFsIndexCollection(XMLInputSource aInput, ParsingOptions aOptions)
          throws InvalidXMLException;

  /**
   * Parses a ResourceManagerConfiguration from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>ResourceManagerConfiguration</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid
   *           ResourceManagerConfiguration
   */
  public ResourceManagerConfiguration parseResourceManagerConfiguration(XMLInputSource aInput)
          throws InvalidXMLException;

  /**
   * Parses a ResourceManagerConfiguration from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>ResourceManagerConfiguration</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid
   *           ResourceManagerConfiguration
   */
  public ResourceManagerConfiguration parseResourceManagerConfiguration(XMLInputSource aInput,
          ParsingOptions aOptions) throws InvalidXMLException;

  /**
   * Parses a FlowControllerDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>FlowControllerDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid FlowControllerDescription
   */
  public FlowControllerDescription parseFlowControllerDescription(XMLInputSource aInput)
          throws InvalidXMLException;

  /**
   * Parses a FlowControllerDescription from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>FlowControllerDescription</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid FlowControllerDescription
   */
  public FlowControllerDescription parseFlowControllerDescription(XMLInputSource aInput,
          ParsingOptions aOptions) throws InvalidXMLException;

  /**
   * Parses a CustomResourceSpecifier from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>CustomResourceSpecifier</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid CustomResourceSpecifier
   */
  public CustomResourceSpecifier parseCustomResourceSpecifier(XMLInputSource aInput)
          throws InvalidXMLException;

  /**
   * Parses a CustomResourceSpecifier from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>CustomResourceSpecifier</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid CustomResourceSpecifier
   */
  public CustomResourceSpecifier parseCustomResourceSpecifier(XMLInputSource aInput,
          ParsingOptions aOptions) throws InvalidXMLException;

  /**
   * Parses a PearSpecifier from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return a <code>PearSpecifier</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid PearSpecifier
   */
  public PearSpecifier parsePearSpecifier(XMLInputSource aInput)
          throws InvalidXMLException;

  /**
   * Parses a PearSpecifier from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return a <code>PearSpecifier</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid PearSpecifier
   */
  public PearSpecifier parsePearSpecifier(XMLInputSource aInput,
          ParsingOptions aOptions) throws InvalidXMLException;

  /**
   * Parses an IndexBuildSpecification from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * 
   * @return an <code>IndexBuildSpecification</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid IndexBuildSpecification
   */
  public IndexBuildSpecification parseIndexBuildSpecification(XMLInputSource aInput)
          throws InvalidXMLException;

  /**
   * Parses an IndexBuildSpecification from an XML input stream.
   * 
   * @param aInput
   *          the input source from which to read the XML document
   * @param aOptions
   *          option settings
   * 
   * @return an <code>IndexBuildSpecification</code> object constructed from the XML document
   * 
   * @throws InvalidXMLException
   *           if the input XML is not valid or does not specify a valid IndexBuildSpecification
   */
  public IndexBuildSpecification parseIndexBuildSpecification(XMLInputSource aInput,
          ParsingOptions aOptions) throws InvalidXMLException;

  /**
   * Builds an object from its XML DOM representation. This method is not typically called by
   * applications. It may be called from within a
   * {@link XMLizable#buildFromXMLElement(Element, XMLParser)} method to construct sub-objects.
   * 
   * @param aElement
   *          a DOM Element
   * 
   * @return an <code>XMLizable</code> object constructed from the DOM element
   * 
   * @throws InvalidXMLException
   *           if the XML element does not specify a valid object
   */
  public XMLizable buildObject(Element aElement) throws InvalidXMLException;

  /**
   * Builds an object from its XML DOM representation. This method is not typically called by
   * applications. It may be called from within a
   * {@link XMLizable#buildFromXMLElement(Element, XMLParser)} method to construct sub-objects.
   * 
   * @param aElement
   *          a DOM Element
   * @param aOptions
   *          option settings
   * 
   * @return an <code>XMLizable</code> object constructed from the DOM element
   * 
   * @throws InvalidXMLException
   *           if the XML element does not specify a valid object
   */
  public XMLizable buildObject(Element aElement, ParsingOptions aOptions)
          throws InvalidXMLException;

  /**
   * Builds an object from its XML DOM representation. This method is not typically called by
   * applications. It may be called from within a
   * {@link XMLizable#buildFromXMLElement(Element, XMLParser)} method to construct sub-objects.
   * <p>
   * This method is similar to {@link #buildObject(Element, XMLParser.ParsingOptions)} but can also
   * parse primitive-typed objects wrapped as XML elements, such as
   * <code>&lt;integer&gt;42&lt;/integer</code>.
   * 
   * @param aElement
   *          a DOM Element
   * @param aOptions
   *          option settings
   * 
   * @return an object constructed from the DOM element. This can be either an instance of
   *         {@link XMLizable}, {@link String}, or one of the primitive type wrapper objects (e.g.
   *         {@link Integer}, {@link Float}, {@link Boolean}).
   * 
   * @throws InvalidXMLException
   *           if the XML element does not specify a valid object
   */
  public Object buildObjectOrPrimitive(Element aElement, ParsingOptions aOptions)
          throws InvalidXMLException;

  /**
   * Creates a new <code>SaxDeserializer</code> object, which implements the SAX
   * {@link org.xml.sax.ContentHandler} interface and can be used to deserialize an
   * {@link XMLizable} object from the events sent from a SAX parser. This can be used if the
   * application already has a SAX parser that generates these events. In most cases, it is easier
   * to use one of the <code>parse</code> methods on this interface.
   * <p>
   * The SAX deserializer returned my this method will expand XIncludes but will not do schema
   * validation.
   * 
   * @return an object that implements {@link org.xml.sax.ContentHandler} and can be used to
   *         deserialize an {@link XMLizable} object from SAX events.
   * 
   */
  public SaxDeserializer newSaxDeserializer();

  /**
   * Creates a new <code>SaxDeserializer</code>.
   * 
   * @param aOptions
   *          option settings
   * 
   * @return an object that implements {@link org.xml.sax.ContentHandler} and can be used to
   *         deserialize an {@link XMLizable} object from SAX events.
   * 
   * @see #newSaxDeserializer()
   * 
   */
  public SaxDeserializer newSaxDeserializer(ParsingOptions aOptions);

  /**
   * Configures this XMLParser by registering a mapping between the name of an XML element and the
   * Class of object to be built from elements with that name.
   * 
   * @param aElementName
   *          the name of an XML element
   * @param aClassName
   *          the name of a Class of object to be built. This class must implement {@link XMLizable}
   *          and have a zero-argument constructor.
   * 
   * @throws ClassNotFoundException
   *           if the class named by <code>aClassName</code> could not be found
   */
  public void addMapping(String aElementName, String aClassName) throws ClassNotFoundException;

  /**
   * XML namespace for ResourceSpecifiers. XML ResourceSpecifier documents must use this namespace
   * or they will be considered invalid by the schema validator.
   */
  public static final String RESOURCE_SPECIFIER_NAMESPACE = "http://uima.apache.org/resourceSpecifier";

  /**
   * Name of schema for ResourceSpecifiers. This file will be looked up in the classpath.
   */
  public static final String RESOURCE_SPECIFIER_SCHEMA_NAME = "/resourceSpecifierSchema.xsd";

  /**
   * Option settings for the parser.
   * 
   * 
   */
  public static class ParsingOptions {
    /**
     * Whether to expand &lt;xi:include&gt; elements according to the XInclude spec.
     * 
     * @deprecated XInclude is no longer supported
     */
    @Deprecated
	  public boolean expandXIncludes;

    /**
     * Whether to expand &lt;envVarRef&gt;VARNAME&lt;/envVarRef&gt; elements by substituting the
     * value of the System property VARNAME.
     */
    public boolean expandEnvVarRefs;
    
    /**
     * Whether to preserve comments and ignorable whitespace
     */
    public boolean preserveComments = false;

    /**
     * Creates a new ParsingOptions object.
     * 
     * @param aExpandXIncludes
     *          Whether to expand &lt;xi:include&gt; elements according to the XInclude spec.
     * @param aExpandEnvVarRefs
     *          Whether to expand &lt;envVarRef&gt;VARNAME&lt;/envVarRef&gt; elements by
     *          substituting the value of the System property VARNAME.
     * @deprecated XInclude is no longer supported
     */
    @Deprecated
	  public ParsingOptions(boolean aExpandXIncludes, boolean aExpandEnvVarRefs) {
      expandXIncludes = aExpandXIncludes;
      expandEnvVarRefs = aExpandEnvVarRefs;
    }

    /**
     * Creates a new ParsingOptions object.
     * 
     * @param aExpandEnvVarRefs
     *          Whether to expand &lt;envVarRef&gt;VARNAME&lt;/envVarRef&gt; elements by
     *          substituting the value of the System property VARNAME.
     */
    public ParsingOptions(boolean aExpandEnvVarRefs) {
      expandEnvVarRefs = aExpandEnvVarRefs;
    }
  }
}
