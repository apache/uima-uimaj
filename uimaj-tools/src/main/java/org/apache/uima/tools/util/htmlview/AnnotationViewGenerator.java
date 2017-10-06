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

package org.apache.uima.tools.util.htmlview;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.uima.UIMARuntimeException;
import org.apache.uima.analysis_engine.AnalysisEngine;
import org.apache.uima.analysis_engine.TypeOrFeature;
import org.apache.uima.analysis_engine.metadata.AnalysisEngineMetaData;
import org.apache.uima.internal.util.XMLUtils;
import org.apache.uima.resource.metadata.Capability;
import org.apache.uima.resource.metadata.TypeDescription;
import org.apache.uima.resource.metadata.TypeSystemDescription;

/**
 * Utility that uses XSL stylesheets to produce an HTML view (w/ Javascript) of an annotated
 * document.
 * 
 * 
 */
public class AnnotationViewGenerator {

  /** Transformer factory for doing XSL transformations. */
  private TransformerFactory mTFactory;

  /**
   * XSL transform used to translate a style map XML file into the CSS stylesheet used in the
   * annotation viewer.
   */
  private Templates mStyleMapToCss;

  /**
   * XSL transform used to translate a style map XML file into the HTML legend used in the
   * annotation viewer.
   */
  private Templates mStyleMapToLegend;

  /**
   * XSL transform used to translate a style map XML file into ANOTHER XSL file, which can then be
   * applied to an annotated document to produce the main document HTML view.
   */
  private Templates mStyleMapToDocFrameXsl;

  /** Directory in which this program will write its output files. */
  private File mOutputDir;

  /**
   * Creates a new AnnotationViewGenerator.
   * 
   * @param aOutputDir
   *          directory in which this program will write its output files.
   */
  public AnnotationViewGenerator(File aOutputDir) {
    mOutputDir = aOutputDir;
    mTFactory = XMLUtils.createTransformerFactory();

    // the viewer uses several files located via the classpath
    // parse xsl files into templates
    mStyleMapToCss = getTemplates("styleMapToCss.xsl");
    mStyleMapToLegend = getTemplates("styleMapToLegend.xsl");
    mStyleMapToDocFrameXsl = getTemplates("styleMapToDocFrameXsl.xsl");
  }

  /**
   * Parses an XML file and produces a Templates object.
   * 
   * @param filename
   *          name of .xsl file, to be looked up in the classpath, under the same package as this
   *          class.
   * @return Templates object usable for XSL transformation
   */
  private Templates getTemplates(String filename) {
    InputStream is = AnnotationViewGenerator.class.getResourceAsStream(filename);
    Templates templates;
    try {
      templates = mTFactory.newTemplates(new StreamSource(is));
    } catch (TransformerConfigurationException e) {
      throw new UIMARuntimeException(e);
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        // ignore close errors
      }
    }
    return templates;
  }

  /**
   * Writes a resource file to disk. The resource file is looked up in the classpath
   * 
   * @param filename
   *          name of the file, to be looked up in the classpath, under the same package as this
   *          class.
   * @return outputDir directory of output file. Output file will be named the same as the
   *         <code>filename</code> parameter.
   */
  private void writeToFile(String filename, File outputDir) {
    File outFile = new File(outputDir, filename);
    OutputStream os;
    try {
      os = new FileOutputStream(outFile);
    } catch (FileNotFoundException e) {
      throw new UIMARuntimeException(e);
    }
    InputStream is = AnnotationViewGenerator.class.getResourceAsStream(filename);
    try {
      byte[] buf = new byte[1024];
      int numRead;
      while ((numRead = is.read(buf)) > 0) {
        os.write(buf, 0, numRead);
      }
    } catch (IOException e) {
      throw new UIMARuntimeException(e);
    } finally {
      try {
        is.close();
      } catch (IOException e) {
        // ignore close errors
      }
      try {
        os.close();
      } catch (IOException e) {
        // ignore close errors
      }
    }
  }

  /**
   * Processes a user-specified file map and produces three outputs:
   * <UL>
   * <LI>annotations.css - A CSS stylesheet for the annotation viewer</LI>
   * <LI>legend.html - HTML document for legend (bottom pane of viewer)</LI>
   * <LI>docFrame.xsl - An XSL stylesheet to be applied to annotated documents during calls to
   * {@link #processDocument(File)}.</LI>
   * </UL>
   * 
   * @param aStyleMap
   *          path to style map to be processed
   */
  public void processStyleMap(File aStyleMap) throws TransformerException {
    // Copy static files annotations.xsl, annotationViewer.js, and index.html to
    // the output dir as well, where they will be used later
    writeToFile("annotations.xsl", mOutputDir);
    writeToFile("annotationViewer.js", mOutputDir);
    writeToFile("index.html", mOutputDir);
    
    // Generate CSS from Style Map
    Transformer cssTransformer = mStyleMapToCss.newTransformer();
    cssTransformer.transform(new StreamSource(aStyleMap), new StreamResult(new File(mOutputDir,
            "annotations.css").getAbsolutePath()));
    // NOTE: getAbsolutePath() seems to be necessary on Java 1.5

    // Generate legend from Style Map
    Transformer legendTransformer = mStyleMapToLegend.newTransformer();
    legendTransformer.transform(new StreamSource(aStyleMap), new StreamResult(new File(mOutputDir,
            "legend.html").getAbsolutePath()));
    // NOTE: getAbsolutePath() seems to be necessary on Java 1.5

    // Generate DocFrameXsl from Style Map
    Transformer docFrameXslTransformer = mStyleMapToDocFrameXsl.newTransformer();
    docFrameXslTransformer.transform(new StreamSource(aStyleMap), new StreamResult(new File(
            mOutputDir, "docFrame.xsl").getAbsolutePath()));
    // NOTE: getAbsolutePath() seems to be necessary on Java 1.5
  }

  /**
   * Processes an annotated document using the docFrame.xsl stylsheet generated by a previous call
   * to {@link #processStyleMap(File)}. Generates a file named docView.html, which represents the
   * HTML view of the annotated document.
   * 
   * @param aInlineXmlDoc
   *          path to annotated document to be processed
   */
  public void processDocument(File aInlineXmlDoc) throws TransformerException {
    // Generate document view HTML from Inline XML
    Transformer docHtmlTransformer = mTFactory.newTransformer(new StreamSource(new File(mOutputDir,
            "docFrame.xsl")));
    docHtmlTransformer.transform(new StreamSource(aInlineXmlDoc), new StreamResult(new File(
            mOutputDir, "docView.html").getAbsolutePath()));
    // NOTE: getAbsolutePath() seems to be necessary on Java 1.5

  }

  /**
   * Automatically generates a style map for the given text analysis engine. The style map will be
   * returned as an XML string.
   * 
   * @param aTaeMetaData
   *          Metadata of the Text Analysis Engine whose outputs will be viewed using the generated
   *          style map.
   * 
   * @return a String containing the XML style map
   */
  public static String autoGenerateStyleMap(AnalysisEngineMetaData aTaeMetaData) {
    // styles used in automatically generated style maps

    final String[] STYLES = { "color:black; background:lightblue;",
        "color:black; background:lightgreen;", "color:black; background:orange;",
        "color:black; background:yellow;", "color:black; background:pink;",
        "color:black; background:salmon;", "color:black; background:cyan;",
        "color:black; background:violet;", "color:black; background:tan;",
        "color:white; background:brown;", "color:white; background:blue;",
        "color:white; background:green;", "color:white; background:red;",
        "color:white; background:mediumpurple;" };

    // get list of output types from TAE
    ArrayList outputTypes = new ArrayList();
    Capability[] capabilities = aTaeMetaData.getCapabilities();

    for (int i = 0; i < capabilities.length; i++) {
      TypeOrFeature[] outputs = capabilities[i].getOutputs();

      for (int j = 0; j < outputs.length; j++) {
        if (outputs[j].isType() && !outputTypes.contains(outputs[j].getName())) {
          outputTypes.add(outputs[j].getName());
        }
      }
    }

    // generate style map by mapping each type to a background color
    StringBuffer buf = new StringBuffer();

    buf.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
    buf.append("<styleMap>\n");

    int i = 0;
    Iterator it = outputTypes.iterator();

    while (it.hasNext()) {
      String outputType = (String) it.next();
      String label = outputType;
      int lastDot = outputType.lastIndexOf('.');
      if (lastDot > -1) {
        label = outputType.substring(lastDot + 1);
      }

      buf.append("<rule>\n");
      buf.append("<pattern>");
      buf.append(outputType);
      buf.append("</pattern>\n");
      buf.append("<label>");
      buf.append(label);
      buf.append("</label>\n");
      buf.append("<style>");
      buf.append(STYLES[i % STYLES.length]);
      buf.append("</style>\n");
      buf.append("</rule>\n");
      i++;
    }

    buf.append("</styleMap>\n");

    return buf.toString();
  }

  /**
   * Automatically generates a style map for the given type system. The style map will be returned
   * as an XML string.
   * 
   * @param aTypeSystem
   *          the type system for which a style map will be generated
   * 
   * @return a String containing the XML style map
   */
  public static String autoGenerateStyleMap(TypeSystemDescription aTypeSystem) {
    // styles used in automatically generated style maps

    final String[] STYLES = { "color:black; background:lightblue;",
        "color:black; background:lightgreen;", "color:black; background:orange;",
        "color:black; background:yellow;", "color:black; background:pink;",
        "color:black; background:salmon;", "color:black; background:cyan;",
        "color:black; background:violet;", "color:black; background:tan;",
        "color:white; background:brown;", "color:white; background:blue;",
        "color:white; background:green;", "color:white; background:red;",
        "color:white; background:mediumpurple;" };

    TypeDescription[] types = aTypeSystem.getTypes();

    // generate style map by mapping each type to a background color
    StringBuffer buf = new StringBuffer();

    buf.append("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?>\n");
    buf.append("<styleMap>\n");

    for (int i = 0; i < types.length; i++) {
      String outputType = types[i].getName();
      String label = outputType;
      int lastDot = outputType.lastIndexOf('.');
      if (lastDot > -1) {
        label = outputType.substring(lastDot + 1);
      }

      buf.append("<rule>\n");
      buf.append("<pattern>");
      buf.append(outputType);
      buf.append("</pattern>\n");
      buf.append("<label>");
      buf.append(label);
      buf.append("</label>\n");
      buf.append("<style>");
      buf.append(STYLES[i % STYLES.length]);
      buf.append("</style>\n");
      buf.append("</rule>\n");
    }

    buf.append("</styleMap>\n");

    return buf.toString();
  }

  /**
   * Automatically generates a style map file for the given analysis engine. The style map will be
   * written to the file <code>aStyleMapFile</code>.
   * 
   * @param aAE
   *          the Analysis Engine whose outputs will be viewed using the generated style map.
   * @param aStyleMapFile
   *          file to which autogenerated style map will be written
   */
  public void autoGenerateStyleMapFile(AnalysisEngine aAE, File aStyleMapFile) throws IOException {
    this.autoGenerateStyleMapFile(aAE.getAnalysisEngineMetaData(), aStyleMapFile);
  }

  /**
   * Automatically generates a style map file for the given analysis engine metadata. The style map
   * will be written to the file <code>aStyleMapFile</code>.
   * 
   * 
   * @param aMetaData
   *          Metadata of the Analysis Engine whose outputs will be viewed using the generated style
   *          map.
   * @param aStyleMapFile
   *          file to which autogenerated style map will be written
   */
  public void autoGenerateStyleMapFile(AnalysisEngineMetaData aMetaData, File aStyleMapFile)
          throws IOException {
    String xmlStr = autoGenerateStyleMap(aMetaData);
    FileWriter out = null;
    try {
      out = new FileWriter(aStyleMapFile);
      out.write(xmlStr);
    } finally {
      if (out != null)
        out.close();
    }
  }

  /**
   * Automatically generates a style map file for the given type system. The style map will be
   * written to the file <code>aStyleMapFile</code>.
   * 
   * @param aTypeSystem
   *          the type system for which a style map will be generated
   * @param aStyleMapFile
   *          file to which autogenerated style map will be written
   */
  public void autoGenerateStyleMapFile(TypeSystemDescription aTypeSystem, File aStyleMapFile)
          throws IOException {
    String xmlStr = autoGenerateStyleMap(aTypeSystem);
    FileWriter out = null;
    try {
      out = new FileWriter(aStyleMapFile);
      out.write(xmlStr);
    } finally {
      if (out != null)
        out.close();
    }
  }
}
