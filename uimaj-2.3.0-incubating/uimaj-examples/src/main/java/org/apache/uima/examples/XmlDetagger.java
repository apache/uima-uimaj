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

package org.apache.uima.examples;

import java.io.InputStream;
import java.util.Iterator;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;
import org.apache.uima.resource.ResourceInitializationException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A multi-sofa annotator that does XML detagging. Reads XML data from the input Sofa (named
 * "xmlDocument"); this data can be stored in the CAS as a string or array, or it can be a URI to a
 * remote file. The XML is parsed using the JVM's default parser, and the plain-text content is
 * written to a new sofa called "plainTextDocument".
 */
public class XmlDetagger extends CasAnnotator_ImplBase {
  /**
   * Name of optional configuration parameter that contains the name of an XML tag that appears in
   * the input file. Only text that falls within this XML tag will be considered part of the
   * "document" that it is added to the CAS by this CAS Initializer. If not specified, the entire
   * file will be considered the document.
   */
  public static final String PARAM_XMLTAG = "XmlTagContainingText";
  
  private SAXParserFactory parserFactory = SAXParserFactory.newInstance();

  private Type sourceDocInfoType;

  private String mXmlTagContainingText = null;

    
  public void initialize(UimaContext aContext) throws ResourceInitializationException {
    super.initialize(aContext);
    // Get config param setting
    mXmlTagContainingText  = (String) getContext().getConfigParameterValue(PARAM_XMLTAG);
  }

  public void typeSystemInit(TypeSystem aTypeSystem) throws AnalysisEngineProcessException {
    sourceDocInfoType = aTypeSystem.getType("org.apache.uima.examples.SourceDocumentInformation");
  }

  public void process(CAS aCAS) throws AnalysisEngineProcessException {
    // get handle to CAS view containing XML document
    CAS xmlCas = aCAS.getView("xmlDocument");
    InputStream xmlStream = xmlCas.getSofa().getSofaDataStream();

    // parse with detag handler
    DetagHandler handler = new DetagHandler();
    try {
      SAXParser parser = parserFactory.newSAXParser();
      parser.parse(xmlStream, handler);
    } catch (Exception e) {
      throw new AnalysisEngineProcessException(e);
    }

    // create the plain text view and set its document text
    CAS plainTextView = aCAS.createView("plainTextDocument");
    plainTextView.setDocumentText(handler.getDetaggedText());
    plainTextView.setDocumentLanguage(aCAS.getView("_InitialView").getDocumentLanguage());

    // Index the SourceDocumentInformation object, if there is one, in the new sofa.
    // This is needed by the SemanticSearchCasIndexer
    Iterator iter = xmlCas.getAnnotationIndex(sourceDocInfoType).iterator();
    if (iter.hasNext()) {
      FeatureStructure sourceDocInfoFs = (FeatureStructure) iter.next();
      plainTextView.getIndexRepository().addFS(sourceDocInfoFs);

    }

  }

  class DetagHandler extends DefaultHandler {
    private StringBuffer detaggedText = new StringBuffer();
    private boolean insideTextTag;

    public DetagHandler() {
      insideTextTag = (mXmlTagContainingText == null);
    }
        
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
      if (qName.equalsIgnoreCase(mXmlTagContainingText)) {
        insideTextTag = true;
      }
    }

    public void endElement(String uri, String localName, String qName) throws SAXException {
      if (qName.equalsIgnoreCase(mXmlTagContainingText)) {
        insideTextTag = false;
      }
    }

    public void characters(char[] ch, int start, int length) throws SAXException {
      if (insideTextTag) {
        detaggedText.append(ch, start, length);        
      }
    }
    
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
      if (insideTextTag) {
        detaggedText.append(ch, start, length);        
      }
    }

    String getDetaggedText() {
      return detaggedText.toString();
    }
  }
}
