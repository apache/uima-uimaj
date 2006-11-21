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

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.uima.analysis_component.CasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.TypeSystem;

/**
 * A multi-sofa annotator that does XML detagging. Reads XML data from the input Sofa (named
 * "xmlDocument"); this data can be stored in the CAS as a string or array, or it can be a URI to a
 * remote file. The XML is parsed using the JVM's default parser, and the plain-text content is
 * written to a new sofa called "plainTextDocument".
 */
public class XmlDetagger extends CasAnnotator_ImplBase {
  private SAXParserFactory parserFactory = SAXParserFactory.newInstance();

  private Type sourceDocInfoType;

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

    public void characters(char[] ch, int start, int length) throws SAXException {
      detaggedText.append(ch, start, length);
    }

    String getDetaggedText() {
      return detaggedText.toString();
    }
  }
}
