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

public class AnalysisEnginePanel extends MetaDataPanel {
  private static final long serialVersionUID = -5927337001602381195L;

  JCheckBox shouldRunCheckBox = new JCheckBox((String) null, true);

  ResourceSpecifier aeSpecifier;

  File specifierFile;

  long lastFileSyncTimestamp;

  public AnalysisEnginePanel(ResourceSpecifier aeSpecifier, File specifierFile, long fileModStamp) {
    super(4); // 4 columns
    this.aeSpecifier = aeSpecifier;
    this.specifierFile = specifierFile;
    this.lastFileSyncTimestamp = fileModStamp;
  }

  public ResourceSpecifier getAnalysisEngineSpecifier() {
    return this.aeSpecifier;
  }

  public long getLastFileSyncTimestamp() {
    return this.lastFileSyncTimestamp;
  }

  public void setLastFileSyncTimestamp(long timestamp) {
    this.lastFileSyncTimestamp = timestamp;
  }

  public boolean hasFileChanged(long lastCheck) {
    return specifierFile.lastModified() > this.lastFileSyncTimestamp
            && specifierFile.lastModified() > lastCheck;
  }

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
