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

package org.apache.uima.caseditor.core.model.dotcorpus;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.uima.caseditor.CasEditorPlugin;
import org.apache.uima.caseditor.editor.AnnotationStyle;
import org.apache.uima.util.XMLSerializer;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/**
 * This class is responsible to read and write {@link DotCorpus} objects from or to a byte stream.
 */
public class DotCorpusSerializer {

  private static final String CONFIG_ELEMENT = "config";

  private static final String CORPUS_ELEMENT = "corpus";

  private static final String CORPUS_FOLDER_ATTRIBUTE = "folder";

  private static final String STYLE_ELEMENT = "style";

  private static final String STYLE_TYPE_ATTRIBUTE = "type";

  private static final String STYLE_STYLE_ATTRIBUTE = "style";

  private static final String STYLE_COLOR_ATTRIBUTE = "color";

  private static final String STYLE_LAYER_ATTRIBUTE = "layer";

  private static final String TYPESYSTEM_ELEMENT = "typesystem";

  private static final String TYPESYTEM_FILE_ATTRIBUTE = "file";

  private static final String CAS_PROCESSOR_ELEMENT = "processor";

  private static final String CAS_PROCESSOR_FOLDER_ATTRIBUTE = "folder";

  private static final String EDITOR_ELEMENT = "editor";

  private static final String EDITOR_LINE_LENGTH_ATTRIBUTE = "line-length-hint";

  /**
   * Creates a {@link DotCorpus} object from a given {@link InputStream}.
   * 
   * @param dotCorpusStream
   * @return the {@link DotCorpus} instance.
   * @throws CoreException
   */
  public static DotCorpus parseDotCorpus(InputStream dotCorpusStream) throws CoreException {
    DocumentBuilderFactory documentBuilderFacoty = DocumentBuilderFactory.newInstance();

    DocumentBuilder documentBuilder;

    try {
      documentBuilder = documentBuilderFacoty.newDocumentBuilder();
    } catch (ParserConfigurationException e) {
      String message = "This should never happen:" + e.getMessage() != null ? e.getMessage() : "";

      IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

      throw new CoreException(s);
    }

    org.w3c.dom.Document dotCorpusDOM;

    try {
      dotCorpusDOM = documentBuilder.parse(dotCorpusStream);
    } catch (SAXException e) {
      String message = e.getMessage() != null ? e.getMessage() : "";

      IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

      throw new CoreException(s);
    } catch (IOException e) {
      String message = e.getMessage() != null ? e.getMessage() : "";

      IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);

      throw new CoreException(s);
    }

    DotCorpus dotCorpus = new DotCorpus();

    // get corpora root element
    Element configElement = dotCorpusDOM.getDocumentElement();

    if (CONFIG_ELEMENT.equals(configElement.getNodeName())) {
      // TODO:
      // throw exception
    }

    NodeList corporaChildNodes = configElement.getChildNodes();

    for (int i = 0; i < corporaChildNodes.getLength(); i++) {
      Node corporaChildNode = corporaChildNodes.item(i);

      if (!(corporaChildNode instanceof Element)) {
        continue;
      }

      Element corporaChildElement = (Element) corporaChildNode;

      if (TYPESYSTEM_ELEMENT.equals(corporaChildElement.getNodeName())) {
        dotCorpus.setTypeSystemFilename(corporaChildElement.getAttribute(TYPESYTEM_FILE_ATTRIBUTE));
      } else if (CORPUS_ELEMENT.equals(corporaChildElement.getNodeName())) {
        String corpusFolderName = corporaChildElement.getAttribute(CORPUS_FOLDER_ATTRIBUTE);

        dotCorpus.addCorpusFolder(corpusFolderName);
      } else if (STYLE_ELEMENT.equals(corporaChildElement.getNodeName())) {
        String type = corporaChildElement.getAttribute(STYLE_TYPE_ATTRIBUTE);

        String styleString = corporaChildElement.getAttribute(STYLE_STYLE_ATTRIBUTE);

        int colorInteger = Integer
                .parseInt(corporaChildElement.getAttribute(STYLE_COLOR_ATTRIBUTE));

        Color color = new Color(colorInteger);

        String drawingLayerString = corporaChildElement.getAttribute(STYLE_LAYER_ATTRIBUTE);

        int drawingLayer;

        try {
          drawingLayer = Integer.parseInt(drawingLayerString);
        } catch (NumberFormatException e) {
          drawingLayer = 0;
        }

        AnnotationStyle style = new AnnotationStyle(type, AnnotationStyle.Style
                .valueOf(styleString), color, drawingLayer);

        dotCorpus.setStyle(style);
      } else if (CAS_PROCESSOR_ELEMENT.equals(corporaChildElement.getNodeName())) {
        dotCorpus.addCasProcessorFolder(corporaChildElement
                .getAttribute(CAS_PROCESSOR_FOLDER_ATTRIBUTE));
      } else if (EDITOR_ELEMENT.equals(corporaChildElement.getNodeName())) {
        String lineLengthHintString = corporaChildElement
                .getAttribute(EDITOR_LINE_LENGTH_ATTRIBUTE);

        int lineLengthHint = Integer.parseInt(lineLengthHintString);

        dotCorpus.setEditorLineLength(lineLengthHint);
      } else {
        String message = "Unexpected element: " + corporaChildElement.getNodeName();

        IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, null);

        throw new CoreException(s);
      }
    }

    return dotCorpus;
  }

  /**
   * Writes the <code>DotCorpus</code> instance to the given <code>OutputStream</code>.
   * 
   * @param dotCorpus
   *          the {@link DotCorpus} object to serialize.
   * @param out
   *          - the stream to write the current <code>DotCorpus</code> instance.
   * @throws CoreException
   */
  public static void serialize(DotCorpus dotCorpus, OutputStream out) throws CoreException {

    XMLSerializer xmlSerializer = new XMLSerializer(out, true);
    ContentHandler xmlSerHandler = xmlSerializer.getContentHandler();

    try {
      xmlSerHandler.startDocument();
      xmlSerHandler.startElement("", CONFIG_ELEMENT, CONFIG_ELEMENT, new AttributesImpl());

      for (String corpusFolder : dotCorpus.getCorpusFolderNameList()) {
        AttributesImpl corpusFolderAttributes = new AttributesImpl();
        corpusFolderAttributes.addAttribute("", "", CORPUS_FOLDER_ATTRIBUTE, "", corpusFolder);

        xmlSerHandler.startElement("", CORPUS_ELEMENT, CORPUS_ELEMENT, corpusFolderAttributes);
        xmlSerHandler.endElement("", CORPUS_ELEMENT, CORPUS_ELEMENT);
      }

      for (AnnotationStyle style : dotCorpus.getAnnotationStyles()) {
        AttributesImpl styleAttributes = new AttributesImpl();
        styleAttributes.addAttribute("", "", STYLE_TYPE_ATTRIBUTE, "", style.getAnnotation());
        styleAttributes.addAttribute("", "", STYLE_STYLE_ATTRIBUTE, "", style.getStyle().name());

        Color color = style.getColor();
        Integer colorInt = new Color(color.getRed(), color.getGreen(), color.getBlue()).getRGB();
        styleAttributes.addAttribute("", "", STYLE_COLOR_ATTRIBUTE, "", colorInt.toString());
        styleAttributes.addAttribute("", "", STYLE_LAYER_ATTRIBUTE, "", Integer.toString(style
                .getLayer()));

        xmlSerHandler.startElement("", STYLE_ELEMENT, STYLE_ELEMENT, styleAttributes);
        xmlSerHandler.endElement("", STYLE_ELEMENT, STYLE_ELEMENT);
      }

      if (dotCorpus.getTypeSystemFileName() != null) {
        AttributesImpl typeSystemFileAttributes = new AttributesImpl();
        typeSystemFileAttributes.addAttribute("", "", TYPESYTEM_FILE_ATTRIBUTE, "", dotCorpus
                .getTypeSystemFileName());

        xmlSerHandler.startElement("", TYPESYSTEM_ELEMENT, TYPESYSTEM_ELEMENT,
                typeSystemFileAttributes);
        xmlSerHandler.endElement("", TYPESYSTEM_ELEMENT, TYPESYSTEM_ELEMENT);
      }

      for (String folder : dotCorpus.getCasProcessorFolderNames()) {
        AttributesImpl taggerConfigAttributes = new AttributesImpl();
        taggerConfigAttributes.addAttribute("", "", CAS_PROCESSOR_FOLDER_ATTRIBUTE, "", folder);

        xmlSerHandler.startElement("", CAS_PROCESSOR_ELEMENT, CAS_PROCESSOR_ELEMENT,
                taggerConfigAttributes);
        xmlSerHandler.endElement("", CAS_PROCESSOR_ELEMENT, CAS_PROCESSOR_ELEMENT);
      }

      if (dotCorpus.getEditorLineLengthHint() != DotCorpus.EDITOR_LINE_LENGTH_HINT_DEFAULT) {
        AttributesImpl editorLineLengthHintAttributes = new AttributesImpl();
        editorLineLengthHintAttributes.addAttribute("", "", EDITOR_LINE_LENGTH_ATTRIBUTE, "",
                Integer.toString(dotCorpus.getEditorLineLengthHint()));

        xmlSerHandler.startElement("", EDITOR_ELEMENT, EDITOR_ELEMENT,
                editorLineLengthHintAttributes);
        xmlSerHandler.endElement("", EDITOR_ELEMENT, EDITOR_ELEMENT);
      }

      xmlSerHandler.endElement("", CONFIG_ELEMENT, CONFIG_ELEMENT);
      xmlSerHandler.endDocument();
    } catch (SAXException e) {
      String message = e.getMessage() != null ? e.getMessage() : "";

      IStatus s = new Status(IStatus.ERROR, CasEditorPlugin.ID, IStatus.OK, message, e);
      throw new CoreException(s);
    }
  }
}
