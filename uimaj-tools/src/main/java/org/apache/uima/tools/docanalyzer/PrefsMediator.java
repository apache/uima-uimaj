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

package org.apache.uima.tools.docanalyzer;

/*
 * 
 * Manages the reading and storing of Preferences for the DocumentAnalyzer Contains preference code
 * so directory names can be known throughout the application.
 */
import java.io.File;
import java.util.prefs.Preferences;

import javax.swing.JButton;

public class PrefsMediator {

  private FileSelector inputFileSelector;

  private FileSelector outputFileSelector;

  private FileSelector xmlFileSelector;

  private JButton runButton, interButton, viewButton;

  private Preferences prefs;

  private String taeDir; // directory where TAE is located

  private String outputDir; // where files are written

  private String inputDir; // where original files are located

  private String defaultInputDir; // so we start with something

  private String defaultOutputDir;

  private String inputFileFormat;
  
  private String language;

  private String encoding;

  private String viewType;

  private String xmlTag;
  
  private boolean lenient;

  // constants describing preference entries
  private static final String VIEWTYPE = "viewType";

  private static final String ENCODING = "encoding";

  private static final String INPUTFILEFORMAT = "inputFileFormat";
  
  private static final String LANGUAGE = "language";

  private static final String TAEDESCRIPTOR = "taeDescriptor";

  private static final String OUTDIR = "outDir";

  private static final String INDIR = "inDir";
  
  private static final String XMLTAG = "xmlTag";
  
  private static final String LENIENT = "lenient";

  public PrefsMediator() {
    // get the installed UIMA home directory
    defaultInputDir = "examples/data";
    defaultOutputDir = "examples/data/processed";
    prefs = Preferences.userRoot().node("org/apache/uima/tools/DocumentAnalyzer1");
    restorePreferences();
  }

  public void restorePreferences() {
    inputDir = prefs.get(INDIR, defaultInputDir);
    inputFileFormat = prefs.get(INPUTFILEFORMAT, "textDocument");
    outputDir = prefs.get(OUTDIR, defaultOutputDir);
    taeDir = prefs.get(TAEDESCRIPTOR, "");
    language = prefs.get(LANGUAGE, "en");
    encoding = prefs.get(ENCODING, "UTF-8");
    viewType = prefs.get(VIEWTYPE, "Java Viewer");
    xmlTag = prefs.get(XMLTAG, "");
    lenient = prefs.getBoolean(LENIENT, true);

  }

  // saves current preferences
  public void savePreferences() {
    String t1 = inputFileSelector.getSelected();
    setInputDir(t1);
    String t2 = outputFileSelector.getSelected();
    setOutputDir(t2);
    String t3 = xmlFileSelector.getSelected();
    setTAEfile(t3);
  }

  /**
   * @return Returns the inputDir.
   */
  public String getInputDir() {
    return inputDir;
  }

  /**
   * @param inputDir
   *          The inputDir to set.
   */
  public void setInputDir(String inputDir) {
    this.inputDir = inputDir;
    prefs.put(INDIR, inputDir);

  }

  /**
   * @return Returns the outputDir.
   */
  public String getOutputDir() {
    return outputDir;
  }

  /**
   * @param outputDir
   *          The outputDir to set.
   */
  public void setOutputDir(String outputDir) {
    this.outputDir = outputDir;
    prefs.put(OUTDIR, outputDir);
  }

  /**
   * Special case of setOutpuDir needed for interactive mode. In interactive mode, we append
   * "/interactive_out" to the end, but we don't want to save this in the preferences. To support
   * that, this method takes one parameter which is the output dir to be set and used by the
   * application, and a second parameter to set the directory that is saved to the preferences.
   * 
   * @param outputDir
   *          the output dir to set
   * @param outputDirToSave
   *          the output dir to save to the preferences
   */
  public void setOutputDirForInteractiveMode(String outputDir, String outputDirToSave) {
    this.outputDir = outputDir;
    prefs.put(OUTDIR, outputDirToSave);
  }

  /**
   * @return Returns the tAEdir.
   */
  public String getTAEfile() {
    return taeDir;
  }

  /**
   * get the path to the TAE that is where the StyleMap file should be written
   * 
   */
  public String getTAEPath() {
    int index = indexOfLastFileSeparator(taeDir);
    if (index > 0) {
      String path = taeDir.substring(0, index);
      return path;
    } else {
      return "";
    }
  }

  public String getTAEFileNameRoot() {
    String file;
    int index = indexOfLastFileSeparator(taeDir);
    if (index > 0) {
      file = taeDir.substring(index);
    } else {
      file = taeDir;
    }
    int ix = file.indexOf(".xml");
    if (ix >= 0) {
      file = file.substring(0, ix);
    }
    return file;
  }

  /**
   * Gets index of last file separator character in a file path.
   * Supports File.separator but also / on Windows.
   * @param path
   * @return index of the last file separator char.  Returns -1 if none.
   */
  private int indexOfLastFileSeparator(String path) {
    int index = path.lastIndexOf(File.separator);
    if (!File.separator.equals("/")) {
      index = Math.max(index, path.lastIndexOf('/'));
    }
    return index;
  }

  /**
   * @param edir
   *          set the TAE directory
   */
  public void setTAEfile(String edir) {
    taeDir = edir;
    prefs.put(TAEDESCRIPTOR, taeDir);
  }

  /**
   * @return Returns the encoding.
   */
  public String getEncoding() {
    return encoding;
  }

  /**
   * @param encoding
   *          The encoding to set.
   */
  public void setEncoding(String encoding) {
    this.encoding = encoding;
    prefs.put(ENCODING, encoding);
  }
  
  /**
   * @return Returns the input file format.
   */
  public String getInputFileFormat() {
    return inputFileFormat;
  }

  /**
   * @param inputFileFormat
   *          The input file format to set.
   */
  public void setInputFileFormat(String inputFileFormat) {
    this.inputFileFormat = inputFileFormat;
    prefs.put(INPUTFILEFORMAT, inputFileFormat);
  }

  /**
   * @return Returns the language.
   */
  public String getLanguage() {
    return language;
  }

  /**
   * @param language
   *          The language to set.
   */
  public void setLanguage(String language) {
    this.language = language;
    prefs.put(LANGUAGE, language);
  }

  /**
   * @return Returns the view type.
   */
  public String getViewType() {
    return viewType;
  }

  /**
   * @param viewType
   *          The view type to set.
   */
  public void setViewType(String viewType) {
    this.viewType = viewType;
    prefs.put(VIEWTYPE, viewType);
  }

  /**
   * @return Returns the xmlTag.
   */
  public String getXmlTag() {
    return xmlTag;
  }

  /**
   * @param xmlTag
   *          The xmlTag to set.
   */
  public void setXmlTag(String xmlTag) {
    this.xmlTag = xmlTag;
    prefs.put(XMLTAG, xmlTag);
  }

  /**
   * @return Returns lenient.
   */
  public Boolean getLenient() {
    return lenient;
  }

  /**
   * @param lenient
   *          The lenient to set.
   */
  public void setLenient(Boolean lenient) {
    this.lenient = lenient;
    prefs.putBoolean(LENIENT, lenient);
  }
  
  /** returns the new edited stylemap file */
  public File getStylemapFile() {
    String s = getTAEPath() + getTAEFileNameRoot() + "StyleMap.xml";
    return new File(s);
  }

  // gets copies of buttonreference se it can mediate their on-ness
  public void setDocButtons(JButton run, JButton inter, JButton view) {
    runButton = run;
    interButton = inter;
    viewButton = view;

    fieldFocusLost(); // sets any enabled that should be
  }

  // sets the File Seelctors
  public void setFileSelectors(FileSelector input, FileSelector output, FileSelector xml) {
    inputFileSelector = input;
    outputFileSelector = output;
    xmlFileSelector = xml;
  }

  // check all 3 text fields and adjust the enabling of the 3 buttons
  public void fieldFocusLost() {
    boolean enableRun = false;
    boolean enableInter = false;
    boolean enableView = false;
    if (inputFileSelector != null) {
      String t1 = inputFileSelector.getSelected();
      setInputDir(t1);
      String t2 = outputFileSelector.getSelected();
      setOutputDir(t2);
      String t3 = xmlFileSelector.getSelected();
      setTAEfile(t3);
      if (t2.length() > 0) {
        enableView = true;
      }
      if ((t2.length() > 0) && (t3.length() > 0)) {
        enableInter = true;
        if (t1.length() > 0) {
          enableRun = true;
        }
      }
    }
    runButton.setEnabled(enableRun);
    interButton.setEnabled(enableInter);
    viewButton.setEnabled(enableView);
  }

}
