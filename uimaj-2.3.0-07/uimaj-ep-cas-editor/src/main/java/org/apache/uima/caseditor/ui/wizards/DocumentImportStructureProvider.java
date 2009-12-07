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

package org.apache.uima.caseditor.ui.wizards;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.StyledDocument;
import javax.swing.text.rtf.RTFEditorKit;

import org.apache.uima.UIMAFramework;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.impl.XCASSerializer;
import org.apache.uima.cas.impl.XmiCasSerializer;
import org.apache.uima.caseditor.core.TaeError;
import org.apache.uima.caseditor.editor.DocumentFormat;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.resource.metadata.TypeSystemDescription;
import org.apache.uima.util.CasCreationUtils;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;
import org.apache.uima.util.XMLParser;
import org.eclipse.ui.wizards.datatransfer.IImportStructureProvider;
import org.xml.sax.SAXException;

/**
 */
final class DocumentImportStructureProvider implements IImportStructureProvider {

  /**
   * Constructs a new DocumentImportStructureProvider object.
   *
   * @param containerFullPath
   */
  public DocumentImportStructureProvider() {
  }

  public List<Object> getChildren(Object element) {
    return null;
  }

  private static CAS createEmtpyCAS()
  {
      XMLInputSource xmlTypeSystemSource = new XMLInputSource(DocumentImportStructureProvider.class
            .getResourceAsStream("ts.xml"), new File(""));
      XMLParser xmlParser = UIMAFramework.getXMLParser();

      TypeSystemDescription typeSystemDesciptor;

      try {
        typeSystemDesciptor = (TypeSystemDescription) xmlParser
                .parse(xmlTypeSystemSource);
      } catch (InvalidXMLException e1) {
        throw new TaeError("Integrated ts.xml typesystem descriptor is not valid!");
      }

      try {
        return CasCreationUtils.createCas(typeSystemDesciptor,
                    null, null);
      } catch (ResourceInitializationException e) {

        // should not happen
        throw new TaeError("Unexpected exception!");
      }
  }

  private InputStream getDocument(String text, DocumentFormat format) {

    CAS cas = createEmtpyCAS();
    cas.setDocumentText(text);

    ByteArrayOutputStream out = new ByteArrayOutputStream(40000);

    if (DocumentFormat.XCAS.equals(format)) {
	    try {
	      XCASSerializer.serialize(cas, out);
	    } catch (SAXException e) {
	      // should not happen
	      throw new TaeError("Unexpected exception!", e);
	    } catch (IOException e) {
	      // will not happen, writing to memory
	      throw new TaeError("Unexpected exception!", e);
	    }
    }
    else if (DocumentFormat.XMI.equals(format)) {
    	try {
			XmiCasSerializer.serialize(cas, out);
		} catch (SAXException e) {
			// should not happen
			throw new TaeError("Unexpected exception!", e);
		}
    }
    else {
    	throw new TaeError("Unkown document type!", null);
    }

    return new ByteArrayInputStream(out.toByteArray());
  }

  public InputStream getContents(Object element) {
    File fileToImport = (File) element;

    String fileName = fileToImport.getName();

    if (fileName.endsWith(".rtf")) {
      InputStream in = null;

      try {
        in = new FileInputStream((File) element);
        String text = convert(in);

        return getDocument(text, DocumentFormat.XMI);
      } catch (FileNotFoundException e) {
        return null;
      } catch (IOException e) {
        return null;
      } finally {
        try {
          if (in != null) {
            in.close();
          }
        } catch (IOException e) {
          // sorry that this can happen
        }
      }

    } else if (fileName.endsWith(".txt")) {
      InputStream in = null;
      try {
        in = new FileInputStream((File) element);

        StringBuffer textStringBuffer = new StringBuffer();

        byte[] readBuffer = new byte[2048];

        while (in.available() > 0) {
          int length = in.read(readBuffer);

          // TODO: ask the user for the correct encoding
          textStringBuffer.append(new String(readBuffer, 0, length)); //, "UTF-8"));
        }

        return getDocument(textStringBuffer.toString(), DocumentFormat.XMI);
      } catch (FileNotFoundException e) {
        return null;
      } catch (IOException e) {
        return null;
      } finally {
        if (in != null) {
          try {
            in.close();
          } catch (IOException e) {
            // sorry that this can fail
          }
        }
      }
    } else {
      try {
        return new FileInputStream((File) element);
      } catch (FileNotFoundException e) {
        return null;
      }
    }
  }

  private String convert(InputStream rtfDocumentInputStream) throws IOException {
    RTFEditorKit aRtfEditorkit = new RTFEditorKit();

    StyledDocument styledDoc = new DefaultStyledDocument();

    String textDocument;

    try {
      aRtfEditorkit.read(rtfDocumentInputStream, styledDoc, 0);

      textDocument = styledDoc.getText(0, styledDoc.getLength());
    } catch (BadLocationException e) {
      throw new IOException("Error during parsing");
    }

    return textDocument;
  }

  public String getFullPath(Object element) {
    return "";
  }

  public String getLabel(Object element) {
    File fileToImport = (File) element;

    String fileName = fileToImport.getName();

    if (fileName.endsWith(".rtf") || fileName.endsWith(".txt")) {
      int nameWithouEndingLength = fileName.lastIndexOf(".");
      String nameWithouEnding = fileName.substring(0, nameWithouEndingLength);

      return nameWithouEnding + ".xmi";
    } else {
      return fileName;
    }
  }

  public boolean isFolder(Object element) {
    return ((File) element).isDirectory();
  }
}