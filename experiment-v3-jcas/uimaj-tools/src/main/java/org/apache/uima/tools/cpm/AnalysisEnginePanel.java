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

package org.apache.uima.tools.cpm;

import java.io.File;
import java.io.IOException;

import javax.swing.JCheckBox;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.resource.ResourceSpecifier;
import org.apache.uima.util.InvalidXMLException;
import org.apache.uima.util.XMLInputSource;


/**
 * The Class AnalysisEnginePanel.
 */
public class AnalysisEnginePanel extends MetaDataPanel {
  
  /** The Constant serialVersionUID. */
  private static final long serialVersionUID = -5927337001602381195L;

  /** The should run check box. */
  JCheckBox shouldRunCheckBox = new JCheckBox((String) null, true);

  /** The ae specifier. */
  ResourceSpecifier aeSpecifier;

  /** The specifier file. */
  File specifierFile;

  /** The last file sync timestamp. */
  long lastFileSyncTimestamp;

  /**
   * Instantiates a new analysis engine panel.
   *
   * @param aeSpecifier the ae specifier
   * @param specifierFile the specifier file
   * @param fileModStamp the file mod stamp
   */
  public AnalysisEnginePanel(ResourceSpecifier aeSpecifier, File specifierFile, long fileModStamp) {
    super(4); // 4 columns
    this.aeSpecifier = aeSpecifier;
    this.specifierFile = specifierFile;
    this.lastFileSyncTimestamp = fileModStamp;
  }

  /**
   * Gets the analysis engine specifier.
   *
   * @return the analysis engine specifier
   */
  public ResourceSpecifier getAnalysisEngineSpecifier() {
    return this.aeSpecifier;
  }

  /**
   * Gets the last file sync timestamp.
   *
   * @return the last file sync timestamp
   */
  public long getLastFileSyncTimestamp() {
    return this.lastFileSyncTimestamp;
  }

  /**
   * Sets the last file sync timestamp.
   *
   * @param timestamp the new last file sync timestamp
   */
  public void setLastFileSyncTimestamp(long timestamp) {
    this.lastFileSyncTimestamp = timestamp;
  }

  /**
   * Checks for file changed.
   *
   * @param lastCheck the last check
   * @return true, if successful
   */
  public boolean hasFileChanged(long lastCheck) {
    return specifierFile.lastModified() > this.lastFileSyncTimestamp
            && specifierFile.lastModified() > lastCheck;
  }

  /**
   * Refresh from file.
   *
   * @throws InvalidXMLException the invalid XML exception
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public void refreshFromFile() throws InvalidXMLException, IOException {
    clearAll();
    this.aeSpecifier = UIMAFramework.getXMLParser().parseResourceSpecifier(
            new XMLInputSource(this.specifierFile));
    if (aeSpecifier instanceof AnalysisEngineDescription) {
      AnalysisEngineDescription aeDescription = (AnalysisEngineDescription) aeSpecifier;
      populate(aeDescription.getMetaData(), null);
    } else {
      this.removeAll();
    }
    this.lastFileSyncTimestamp = this.specifierFile.lastModified();
  }
}
