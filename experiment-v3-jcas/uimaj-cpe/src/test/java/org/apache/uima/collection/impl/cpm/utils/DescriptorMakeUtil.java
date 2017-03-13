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

package org.apache.uima.collection.impl.cpm.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.apache.uima.UIMAFramework;
import org.apache.uima.analysis_engine.AnalysisEngineDescription;
import org.apache.uima.collection.CasConsumerDescription;
import org.apache.uima.collection.CollectionReaderDescription;
import org.apache.uima.resource.ResourceCreationSpecifier;
import org.apache.uima.test.junit_extension.JUnitExtension;
import org.apache.uima.util.XMLInputSource;


public class DescriptorMakeUtil {
  private static final String FS = System.getProperties().getProperty("file.separator");

  public static String makeAnalysisEngine(String descFileName) throws Exception {

    return makeAnalysisEngine(descFileName, false, null, 0, null);

  }

  public static String makeAnalysisEngine(String descFileName, boolean shouldCrash,
          String functionName, int errorCount, String exceptionName) throws Exception {
    XMLInputSource in = new XMLInputSource(descFileName);
    AnalysisEngineDescription aed = UIMAFramework.getXMLParser().parseAnalysisEngineDescription(in);
    // set the function to crash, if desired
    aed.getMetaData().getConfigurationParameterSettings().setParameterValue("default",
            "TestAnnotator", Boolean.valueOf(shouldCrash));
    if (shouldCrash) {
      aed.getMetaData().getConfigurationParameterSettings().setParameterValue(functionName,
              "ErrorCount", Integer.valueOf(errorCount));
      aed.getMetaData().getConfigurationParameterSettings().setParameterValue(functionName,
              "Exception", exceptionName);
    }
    File baseDir = JUnitExtension.getFile("CpmTests" + FS + "CpeDesc");

    if (!baseDir.exists()) {
      baseDir.mkdir();
    }

    File tmpFileName = new File(baseDir, "TmpAnalysisEngine.xml");

    OutputStream out = new FileOutputStream(tmpFileName);
    serializeDescriptor(aed, out);

    return tmpFileName.getAbsolutePath();
  }

  public static String makeCasConsumer(String descFileName) throws Exception {

    return makeCasConsumer(descFileName, false, null, 0, null);

  }

  public static String makeCasConsumer(String descFileName, boolean shouldCrash,
          String functionName, int errorCount, String exceptionName) throws Exception {

    XMLInputSource in = new XMLInputSource(descFileName);
    CasConsumerDescription ccd = UIMAFramework.getXMLParser().parseCasConsumerDescription(in);
    // set the function to crash, if desired
    if (shouldCrash) {
      ccd.getCasConsumerMetaData().getConfigurationParameterSettings().setParameterValue(
              "ErrorFunction", functionName);
      ccd.getCasConsumerMetaData().getConfigurationParameterSettings().setParameterValue(
              "ErrorCount", Integer.valueOf(errorCount));
      ccd.getCasConsumerMetaData().getConfigurationParameterSettings().setParameterValue(
              "ErrorException", exceptionName);
    }
    File baseDir = JUnitExtension.getFile("CpmTests" + FS + "CpeDesc");

    if (!baseDir.exists()) {
      baseDir.mkdir();
    }

    File tmpFileName = new File(baseDir, "TmpCasConsumer.xml");
    OutputStream out = new FileOutputStream(tmpFileName);
    serializeDescriptor(ccd, out);

    return tmpFileName.getAbsolutePath();

  }

  public static String makeCollectionReader(String descFileName, int documentCount)
          throws Exception {

    return makeCollectionReader(descFileName, false, null, 0, null, documentCount);
  }

  public static String makeCollectionReader(String descFileName, boolean shouldCrash,
          String functionName, int errorCount, String exceptionName, int documentCount)
          throws Exception {

    XMLInputSource in = new XMLInputSource(descFileName);
    CollectionReaderDescription crd = UIMAFramework.getXMLParser()
            .parseCollectionReaderDescription(in);
    crd.getCollectionReaderMetaData().getConfigurationParameterSettings().setParameterValue(
            "DocumentCount", Integer.valueOf(documentCount));
    // set the function to crash, if desired
    if (shouldCrash) {
      crd.getCollectionReaderMetaData().getConfigurationParameterSettings().setParameterValue(
              "ErrorFunction", functionName);
      crd.getCollectionReaderMetaData().getConfigurationParameterSettings().setParameterValue(
              "ErrorCount", Integer.valueOf(errorCount));
      crd.getCollectionReaderMetaData().getConfigurationParameterSettings().setParameterValue(
              "ErrorException", exceptionName);
    }
    File baseDir = JUnitExtension.getFile("CpmTests" + FS + "CpeDesc");

    if (!baseDir.exists()) {
      baseDir.mkdir();
    }

    File tmpFileName = new File(baseDir, "TmpCollectionReader.xml");
    OutputStream out = new FileOutputStream(tmpFileName);
    serializeDescriptor(crd, out);
    return tmpFileName.getAbsolutePath();
  }

  private static void serializeDescriptor(ResourceCreationSpecifier specifier, OutputStream out)
          throws Exception {
    specifier.toXML(out);

  }

}
