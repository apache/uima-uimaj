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
package org.apache.uima.fit.examples.experiment.pos;

import java.io.File;
import java.io.IOException;

import org.apache.uima.UimaContext;
import org.apache.uima.collection.CollectionException;
import org.apache.uima.fit.component.JCasCollectionReader_ImplBase;
import org.apache.uima.fit.descriptor.ConfigurationParameter;
import org.apache.uima.jcas.JCas;
import org.apache.uima.pear.util.FileUtil;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Progress;
import org.apache.uima.util.ProgressImpl;

/**
 * This collection reader is meant for example purposes only. For a much more robust and complete
 * line reader implementation, please see org.cleartk.util.linereader.LineReader.
 * 
 * This collection reader takes a single file and produces one JCas for each line in the file
 * putting the text of the line into the default view.
 */
public class LineReader extends JCasCollectionReader_ImplBase {

  public static final String PARAM_INPUT_FILE = "inputFile";
  @ConfigurationParameter
  private File inputFile;

  private String[] lines;

  private int lineIndex = 0;

  @Override
  public void initialize(UimaContext uimaContext) throws ResourceInitializationException {
    try {
      lines = FileUtil.loadListOfStrings(inputFile);
    } catch (IOException e) {
      throw new ResourceInitializationException(e);
    }
  }

  public boolean hasNext() throws IOException, CollectionException {
    return lineIndex < lines.length;
  }

  @Override
  public void getNext(JCas jCas) throws IOException, CollectionException {
    jCas.setDocumentText(lines[lineIndex]);
    lineIndex++;
  }

  public Progress[] getProgress() {
    Progress progress = new ProgressImpl(lineIndex, lines.length, Progress.ENTITIES);
    return new Progress[] { progress };
  }
}
